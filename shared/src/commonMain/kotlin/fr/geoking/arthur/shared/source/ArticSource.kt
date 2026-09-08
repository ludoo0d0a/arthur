package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Art Institute of Chicago Remote Source (no API key).
 * Public-domain works with IIIF images only; [httpGet] is injected for fixtures.
 *
 * Search: `GET /api/v1/artworks/search?q=…` — `q` from [RemoteCategoryMapping].
 * No native random: each [load] picks a random page and samples the hits.
 */
class ArticSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Art Institute of Chicago"

    // Rotates which MuseumSearchKind target this Source hydrates each load() call
    // — defers the other target(s) to the next call instead of fetching them all now.
    private var targetCursor = 0

    // Advances each load() call so "load more" pages forward instead of re-sampling.
    private var pageCursor = 0

    override suspend fun load(): List<Artwork> = runCatching {
        MuseumLoad.acrossTargets(
            kind(),
            limit,
            random,
            nextTargetIndex = { targetCursor++ },
        ) { target, perKind ->
            val pageIndex = RemoteSample.nextPage(
                cursor = pageCursor++,
                maxPage = RemoteSample.maxPageForHitWindow(),
            )
            val payload = RemoteSample.fetchWindow(
                randomOffset = pageIndex,
                firstOffset = 1,
                fetch = { page -> httpGet(searchUrl(RemoteSample.SEARCH_POOL, target, page = page)) },
                isEmpty = ::looksEmptyArtic,
            )
            val page = json.decodeFromString<ArticSearchPage>(payload)
            val iiifBase = page.config?.iiifUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_IIIF_BASE
            val mapped = page.data
                .filter { it.isPublicDomain }
                .mapNotNull { item -> toArtwork(item, iiifBase, target) }
            RemoteSample.sample(mapped, perKind, random)
        }
    }.getOrDefault(emptyList())

    private fun looksEmptyArtic(payload: String): Boolean =
        runCatching {
            json.decodeFromString<ArticSearchPage>(payload).data.isEmpty()
        }.getOrDefault(true)

    private fun toArtwork(
        item: ArticArtwork,
        iiifBase: String,
        searchKind: MuseumSearchKind,
    ): Artwork? {
        val imageId = item.imageId?.takeIf { it.isNotBlank() } ?: return null
        val objectId = item.id ?: return null
        val title = item.title?.takeIf { it.isNotBlank() } ?: "Object $objectId"
        val attribution = item.artistDisplay?.takeIf { it.isNotBlank() }
            ?: "Art Institute of Chicago"
        val description = item.description
            ?.let(::plainTextDescription)
            ?.takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
            .orEmpty()
        return Artwork(
            id = "artic-$objectId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = iiifImageUrl(iiifBase, imageId),
            description = description,
            date = item.dateDisplay?.takeIf { it.isNotBlank() }.orEmpty(),
            medium = item.mediumDisplay?.takeIf { it.isNotBlank() }.orEmpty(),
            license = if (item.isPublicDomain) "Public Domain" else "",
            externalUrl = collectionPageUrl(objectId),
        )
    }

    companion object {
        const val ID = "artic"
        const val DEFAULT_LIMIT = 20
        const val DEFAULT_IIIF_BASE = "https://www.artic.edu/iiif/2"
        private const val IIIF_SIZE = "843,"

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            limit: Int = DEFAULT_LIMIT,
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
            page: Int = 1,
        ): String {
            val q = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Artic).query.orEmpty()
            // Percent-encode brackets — raw `query[term]…` breaks some HTTP stacks.
            return "https://api.artic.edu/api/v1/artworks/search" +
                "?q=$q" +
                "&query%5Bterm%5D%5Bis_public_domain%5D=true" +
                "&limit=$limit" +
                "&page=$page" +
                "&fields=id,title,artist_display,image_id,is_public_domain,description," +
                "date_display,medium_display"
        }

        fun collectionPageUrl(objectId: Int): String =
            "https://www.artic.edu/artworks/$objectId"

        fun iiifImageUrl(iiifBase: String, imageId: String): String =
            "${iiifBase.trimEnd('/')}/$imageId/full/$IIIF_SIZE/0/default.jpg"

        /** Strip light HTML from Artic description blobs. */
        internal fun plainTextDescription(raw: String): String =
            raw
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}

@Serializable
internal data class ArticSearchPage(
    val data: List<ArticArtwork> = emptyList(),
    val config: ArticConfig? = null,
)

@Serializable
internal data class ArticConfig(
    @SerialName("iiif_url") val iiifUrl: String? = null,
)

@Serializable
internal data class ArticArtwork(
    val id: Int? = null,
    val title: String? = null,
    @SerialName("artist_display") val artistDisplay: String? = null,
    @SerialName("image_id") val imageId: String? = null,
    @SerialName("is_public_domain") val isPublicDomain: Boolean = false,
    val description: String? = null,
    @SerialName("date_display") val dateDisplay: String? = null,
    @SerialName("medium_display") val mediumDisplay: String? = null,
)
