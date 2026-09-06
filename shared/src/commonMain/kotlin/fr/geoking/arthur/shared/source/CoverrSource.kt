package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Coverr stock-video Remote Source.
 * [httpGet] must send `Authorization: Bearer <apiKey>`; blank [apiKey] uses [offlineFallback].
 *
 * Search: `GET https://api.coverr.co/videos?query=…&urls=true`
 */
class CoverrSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val category: () -> StockPhotoCategory = { StockPhotoCategory.Nature },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Coverr"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return offlineFallback()
        val q = RemoteCategoryMapping.stockQuery(category(), RemoteProvider.Coverr)
            ?: return emptyList()
        val art = runCatching {
            val page = json.decodeFromString<CoverrVideoSearchPage>(httpGet(searchUrl(q, limit)))
            page.hits.mapNotNull { hit ->
                if (hit.isVertical == true) return@mapNotNull null
                val videoUrl = hit.urls?.mp4?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = hit.title?.takeIf { it.isNotBlank() } ?: "Coverr ${hit.id}"
                Artwork(
                    id = "coverr-${hit.id}",
                    title = title,
                    attribution = "Coverr",
                    sourceId = ID,
                    kind = ArtworkKind.Video,
                    remoteUrl = videoUrl,
                )
            }
        }.getOrDefault(emptyList())
        if (art.isNotEmpty()) {
            onLoaded(art)
            return art
        }
        return offlineFallback()
    }

    companion object {
        const val ID = "coverr"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            query: String = RemoteCategoryMapping.stockQuery(
                StockPhotoCategory.Nature,
                RemoteProvider.Coverr,
            )!!,
            pageSize: Int = DEFAULT_LIMIT,
        ): String =
            "https://api.coverr.co/videos" +
                "?query=${query.replace(" ", "%20")}" +
                "&urls=true&page_size=$pageSize&sort=popular"
    }
}

@Serializable
internal data class CoverrVideoSearchPage(
    val hits: List<CoverrVideoHit> = emptyList(),
)

@Serializable
internal data class CoverrVideoHit(
    val id: String,
    val title: String? = null,
    @SerialName("is_vertical")
    val isVertical: Boolean? = null,
    val urls: CoverrVideoUrls? = null,
)

@Serializable
internal data class CoverrVideoUrls(
    val mp4: String? = null,
)
