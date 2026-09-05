package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Art Institute of Chicago Remote Source (no API key).
 * Public-domain works with IIIF images only; [httpGet] is injected for fixtures.
 *
 * Search: `GET /api/v1/artworks/search?q=…` — `q` from [RemoteCategoryMapping].
 */
class ArticSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Art Institute of Chicago"

    override suspend fun load(): List<Artwork> = runCatching {
        val targets = RemoteCategoryMapping.museumTargets(kind())
        val perKind = (limit / targets.size).coerceAtLeast(1)
        val results = mutableListOf<Artwork>()
        for (target in targets) {
            val payload = httpGet(searchUrl(perKind, target))
            val page = json.decodeFromString<ArticSearchPage>(payload)
            val iiifBase = page.config?.iiifUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_IIIF_BASE
            page.data
                .asSequence()
                .filter { it.isPublicDomain }
                .mapNotNull { item -> toArtwork(item, iiifBase, target) }
                .take(perKind)
                .forEach { results.add(it) }
        }
        results.take(limit)
    }.getOrDefault(emptyList())

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
        return Artwork(
            id = "artic-$objectId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = iiifImageUrl(iiifBase, imageId),
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
        ): String {
            val q = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Artic).query.orEmpty()
            return "https://api.artic.edu/api/v1/artworks/search" +
                "?q=$q" +
                "&query[term][is_public_domain]=true" +
                "&limit=$limit" +
                "&fields=id,title,artist_display,image_id,is_public_domain"
        }

        fun iiifImageUrl(iiifBase: String, imageId: String): String =
            "${iiifBase.trimEnd('/')}/$imageId/full/$IIIF_SIZE/0/default.jpg"
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
)
