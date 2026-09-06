package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pexels stock-video Remote Source (same API key as [PexelsSource]).
 * [httpGet] must send `Authorization: <apiKey>`; blank [apiKey] uses [offlineFallback].
 *
 * Search: `GET /videos/search?query=…&orientation=landscape`
 */
class PexelsVideoSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val category: () -> StockPhotoCategory = { StockPhotoCategory.Nature },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Pexels Videos"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return offlineFallback()
        val q = RemoteCategoryMapping.stockQuery(category(), RemoteProvider.PexelsVideo)
            ?: return emptyList()
        val art = runCatching {
            val page = json.decodeFromString<PexelsVideoSearchPage>(httpGet(searchUrl(q, limit)))
            page.videos.mapNotNull { video ->
                val videoUrl = pickVideoUrl(video.videoFiles) ?: return@mapNotNull null
                val title = video.user?.name?.takeIf { it.isNotBlank() }?.let { "Video by $it" }
                    ?: "Pexels video ${video.id}"
                val author = video.user?.name?.takeIf { it.isNotBlank() } ?: "Pexels"
                Artwork(
                    id = "pexels-video-${video.id}",
                    title = title,
                    attribution = "$author / Pexels",
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
        const val ID = "pexels-video"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            query: String = RemoteCategoryMapping.stockQuery(
                StockPhotoCategory.Nature,
                RemoteProvider.PexelsVideo,
            )!!,
            perPage: Int = DEFAULT_LIMIT,
        ): String =
            "https://api.pexels.com/v1/videos/search" +
                "?query=${query.replace(" ", "%20")}&orientation=landscape&per_page=$perPage"

        internal fun pickVideoUrl(files: List<PexelsVideoFile>): String? {
            val withLink = files.filter { !it.link.isNullOrBlank() }
            if (withLink.isEmpty()) return null
            val landscape = withLink.filter { (it.width ?: 0) >= (it.height ?: 0) }
            val pool = landscape.ifEmpty { withLink }
            return pool.firstOrNull { it.quality.equals("hd", ignoreCase = true) }?.link
                ?: pool.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.link
        }
    }
}

@Serializable
internal data class PexelsVideoSearchPage(
    val videos: List<PexelsVideo> = emptyList(),
)

@Serializable
internal data class PexelsVideo(
    val id: Long,
    val user: PexelsVideoUser? = null,
    @SerialName("video_files")
    val videoFiles: List<PexelsVideoFile> = emptyList(),
)

@Serializable
internal data class PexelsVideoUser(
    val name: String? = null,
)

@Serializable
internal data class PexelsVideoFile(
    val quality: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val link: String? = null,
)
