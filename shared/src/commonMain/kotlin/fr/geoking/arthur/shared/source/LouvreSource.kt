package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Musée du Louvre Remote Source (no API key).
 * Louvre publishes per-object JSON (`…/ark:/53355/{id}.json`) but no search API,
 * so Arthur loads a curated open-access ARK list and hydrates titles/images from JSON.
 * Each [load] samples a random subset of the curated list.
 *
 * Follow Louvre Collections ToS for image reuse; attribution is required.
 */
class LouvreSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val arkIds: (MuseumSearchKind) -> List<String> = { defaultArks(it) },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Musée du Louvre"

    // Rotates which MuseumSearchKind target this Source hydrates each load() call
    // — defers the other target(s) to the next call instead of fetching them all now.
    private var targetCursor = 0

    // The curated ARK lists are small and static, so once every id has been hydrated
    // once, later load() calls for the same kind need no network calls at all.
    private val hydratedCache = mutableMapOf<String, Artwork>()

    override suspend fun load(): List<Artwork> = load(limit = limit)

    override suspend fun load(limit: Int): List<Artwork> = runCatching {
        MuseumLoad.acrossTargets(
            kind(),
            limit,
            random,
            nextTargetIndex = { targetCursor++ },
        ) { target, perKind ->
            val selected = RemoteSample.sample(arkIds(target), perKind, random)
            selected.mapNotNull { arkId ->
                hydratedCache[arkId] ?: run {
                    val payload = httpGet(objectUrl(arkId))
                    val record = json.decodeFromString<LouvreRecord>(payload)
                    toArtwork(record, target)?.also { hydratedCache[arkId] = it }
                }
            }
        }
    }.onFailure { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
    }.getOrDefault(emptyList())

    private fun toArtwork(record: LouvreRecord, searchKind: MuseumSearchKind): Artwork? {
        val arkId = record.arkId?.takeIf { it.isNotBlank() } ?: return null
        val imageUrl = record.image
            ?.sortedBy { it.position ?: Int.MAX_VALUE }
            ?.firstOrNull { !it.urlImage.isNullOrBlank() }
            ?.urlImage
            ?: return null
        val title = record.title?.takeIf { it.isNotBlank() } ?: "Louvre $arkId"
        val artist = record.creator
            ?.firstOrNull { !it.label.isNullOrBlank() }
            ?.label
        val attribution = listOfNotNull(artist, "Musée du Louvre").joinToString(" / ")
        return Artwork(
            id = "louvre-$arkId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
            externalUrl = collectionPageUrl(arkId),
        )
    }

    companion object {
        const val ID = "louvre"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        /** Curated Peintures notices (Collections site ARK ids). */
        val PAINTING_ARKS = listOf(
            "cl010062370", // La Joconde
            "cl010061995", // Pèlerinage à l'île de Cythère
            "cl010066284", // Potocki
            "cl010065782", // Cathédrale de Barcelone
            "cl010065351", // Portrait d'homme
            "cl010066571", // Couronnement de la Vierge
            "cl010065400", // La Charité
            "cl010065600", // Salomon et la reine de Saba
            "cl010064800", // Portrait de deux hommes
            "cl010064900", // Lisière de forêt
            "cl010065000", // Vierge et l'Enfant
            "cl010066100", // Le Galant Militaire
        )

        /** Curated Sculptures / antiquités notices. */
        val SCULPTURE_ARKS = listOf(
            "cl010277627", // Vénus de Milo
            "cl010278000", // Apollon Sauroctone
            "cl010279000", // Korè de Samos
            "cl010091872", // Esclave mourant
            "cl010093501", // Modillon
            "cl010094156", // Chapiteau
            "cl010090641", // Félix Couturier
            "cl010091234", // Jean Goujon
            "cl010278100", // statuette
        )

        fun objectUrl(arkId: String): String =
            "https://collections.louvre.fr/ark:/53355/$arkId.json"

        fun collectionPageUrl(arkId: String): String =
            "https://collections.louvre.fr/ark:/53355/$arkId"

        fun defaultArks(kind: MuseumSearchKind): List<String> = when (kind) {
            MuseumSearchKind.Sculpture -> SCULPTURE_ARKS
            MuseumSearchKind.All, MuseumSearchKind.Painting -> PAINTING_ARKS
            MuseumSearchKind.Photo -> emptyList()
        }
    }
}

@Serializable
internal data class LouvreRecord(
    val arkId: String? = null,
    val title: String? = null,
    val creator: List<LouvreCreator>? = null,
    val collection: String? = null,
    val image: List<LouvreImage>? = null,
)

@Serializable
internal data class LouvreCreator(
    val label: String? = null,
)

@Serializable
internal data class LouvreImage(
    @SerialName("urlImage") val urlImage: String? = null,
    val copyright: String? = null,
    val position: Int? = null,
)
