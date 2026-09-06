package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pixabay stock-video Remote Source.
 * API key is embedded in the search URL; blank [apiKey] uses [offlineFallback].
 *
 * Search: `GET https://pixabay.com/api/videos/?key=…&q=…`
 */
class PixabayVideoSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val category: () -> StockPhotoCategory = { StockPhotoCategory.Nature },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Pixabay Videos"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return offlineFallback()
        val q = RemoteCategoryMapping.stockQuery(category(), RemoteProvider.Pixabay)
            ?: return emptyList()
        val art = runCatching {
            val page = json.decodeFromString<PixabayVideoSearchPage>(
                httpGet(searchUrl(apiKey, q, limit)),
            )
            page.hits.mapNotNull { hit ->
                val videoUrl = pickVideoUrl(hit.videos) ?: return@mapNotNull null
                val tags = hit.tags?.takeIf { it.isNotBlank() }
                val title = tags?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "Pixabay video ${hit.id}"
                val user = hit.user?.takeIf { it.isNotBlank() } ?: "Pixabay"
                Artwork(
                    id = "pixabay-video-${hit.id}",
                    title = title,
                    attribution = "$user / Pixabay",
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
        const val ID = "pixabay-video"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            apiKey: String,
            query: String = RemoteCategoryMapping.stockQuery(
                StockPhotoCategory.Nature,
                RemoteProvider.Pixabay,
            )!!,
            perPage: Int = DEFAULT_LIMIT,
        ): String =
            "https://pixabay.com/api/videos/" +
                "?key=$apiKey" +
                "&q=${query.replace(" ", "+")}" +
                "&video_type=film&safesearch=true&per_page=$perPage"

        internal fun pickVideoUrl(videos: PixabayVideoSizes?): String? {
            if (videos == null) return null
            return sequenceOf(videos.medium, videos.large, videos.small, videos.tiny)
                .mapNotNull { it?.url?.takeIf { url -> url.isNotBlank() } }
                .firstOrNull()
        }
    }
}

@Serializable
internal data class PixabayVideoSearchPage(
    val hits: List<PixabayVideoHit> = emptyList(),
)

@Serializable
internal data class PixabayVideoHit(
    val id: Long,
    val tags: String? = null,
    val user: String? = null,
    val videos: PixabayVideoSizes? = null,
)

@Serializable
internal data class PixabayVideoSizes(
    val large: PixabayVideoFile? = null,
    val medium: PixabayVideoFile? = null,
    val small: PixabayVideoFile? = null,
    val tiny: PixabayVideoFile? = null,
)

@Serializable
internal data class PixabayVideoFile(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)
