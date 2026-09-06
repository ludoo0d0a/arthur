package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cleveland Museum of Art Open Access Remote Source (no API key).
 * CC0 works with JPEG images only; [httpGet] is injected for fixtures.
 *
 * Search: `GET /api/artworks/?type=…` — `type` codes from [RemoteCategoryMapping].
 * No native random: each [load] uses a random `skip` and samples the window.
 */
class ClevelandSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Cleveland Museum of Art"

    override suspend fun load(): List<Artwork> = runCatching {
        val targets = RemoteCategoryMapping.museumTargets(kind())
        val perKind = (limit / targets.size).coerceAtLeast(1)
        val results = mutableListOf<Artwork>()
        for (target in targets) {
            val skip = RemoteSample.randomStart(RemoteSample.SEARCH_POOL, random = random)
            val payload = httpGet(searchUrl(RemoteSample.SEARCH_POOL, target, skip = skip))
                .let { body ->
                    if (skip > 0 && looksEmptyCleveland(body)) {
                        httpGet(searchUrl(RemoteSample.SEARCH_POOL, target, skip = 0))
                    } else {
                        body
                    }
                }
            val page = json.decodeFromString<ClevelandSearchPage>(payload)
            val mapped = page.data.mapNotNull { toArtwork(it, target) }
            results.addAll(RemoteSample.sample(mapped, perKind, random))
        }
        RemoteSample.sample(results, limit, random)
    }.getOrDefault(emptyList())

    private fun looksEmptyCleveland(payload: String): Boolean =
        runCatching {
            json.decodeFromString<ClevelandSearchPage>(payload).data.isEmpty()
        }.getOrDefault(true)

    private fun toArtwork(item: ClevelandArtwork, searchKind: MuseumSearchKind): Artwork? {
        val objectId = item.id ?: return null
        val imageUrl = item.images?.preferredJpegUrl()?.takeIf { it.isNotBlank() } ?: return null
        val title = item.title?.takeIf { it.isNotBlank() } ?: "Object $objectId"
        val attribution = item.creators
            ?.firstOrNull()
            ?.let { it.description?.takeIf { d -> d.isNotBlank() } ?: it.name }
            ?.takeIf { it.isNotBlank() }
            ?: "Cleveland Museum of Art"
        return Artwork(
            id = "cleveland-$objectId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = item.type?.let { parseKind(it) }
                ?: searchKind.artworkKind
                ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
        )
    }

    private fun parseKind(type: String?): ArtworkKind = when {
        type.equals("Sculpture", ignoreCase = true) -> ArtworkKind.Sculpture
        type.equals("Photograph", ignoreCase = true) -> ArtworkKind.Photo
        else -> ArtworkKind.Painting
    }

    companion object {
        const val ID = "cleveland"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            limit: Int = DEFAULT_LIMIT,
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
            skip: Int = 0,
        ): String {
            val type = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Cleveland).type
                .orEmpty()
            return "https://openaccess-api.clevelandart.org/api/artworks/" +
                "?cc0=1&has_image=1&limit=$limit&skip=$skip&type=$type"
        }
    }
}

@Serializable
internal data class ClevelandSearchPage(
    val data: List<ClevelandArtwork> = emptyList(),
)

@Serializable
internal data class ClevelandArtwork(
    val id: Int? = null,
    val title: String? = null,
    val type: String? = null,
    @SerialName("share_license_status") val shareLicenseStatus: String? = null,
    val creators: List<ClevelandCreator>? = null,
    val images: ClevelandImages? = null,
)

@Serializable
internal data class ClevelandCreator(
    val description: String? = null,
    val name: String? = null,
)

@Serializable
internal data class ClevelandImages(
    val web: ClevelandImageRef? = null,
    val print: ClevelandImageRef? = null,
) {
    fun preferredJpegUrl(): String? =
        print?.url?.takeIf { it.isNotBlank() && !it.endsWith(".tif", ignoreCase = true) }
            ?: web?.url?.takeIf { it.isNotBlank() }
}

@Serializable
internal data class ClevelandImageRef(
    val url: String? = null,
)
