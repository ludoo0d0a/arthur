package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Unsplash stock-photo Remote Source.
 *
 * Unsplash apps have two credentials:
 * - **Access Key** (required here): public `Authorization: Client-ID <accessKey>`
 * - **Secret Key**: OAuth only — never sent on catalog search; keep out of the APK
 *
 * Blank [accessKey] uses [offlineFallback]. [httpGet] must send the Client-ID header.
 * Search query mapped via [RemoteCategoryMapping].
 * No native random: each [load] picks a random page and samples the hits.
 */
class UnsplashSource(
    private val httpGet: suspend (url: String) -> String,
    private val accessKey: String,
    private val category: () -> StockPhotoCategory = { StockPhotoCategory.Nature },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Unsplash"

    override suspend fun load(): List<Artwork> {
        if (accessKey.isBlank()) return offlineFallback()
        val q = RemoteCategoryMapping.stockQuery(category(), RemoteProvider.Unsplash)
            ?: return emptyList()
        val art = runCatching {
            val pageIndex = RemoteSample.randomPage(random = random)
            val payload = RemoteSample.fetchWindow(
                randomOffset = pageIndex,
                firstOffset = 1,
                fetch = { page -> httpGet(searchUrl(q, RemoteSample.SEARCH_POOL, page = page)) },
                isEmpty = { body ->
                    json.decodeFromString<UnsplashSearchPage>(body).results.isEmpty()
                },
            )
            val mapped = json.decodeFromString<UnsplashSearchPage>(payload).results.mapNotNull { photo ->
                val imageUrl = photo.urls?.regular?.takeIf { it.isNotBlank() }
                    ?: photo.urls?.full?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val title = photo.description?.takeIf { it.isNotBlank() }
                    ?: photo.altDescription?.takeIf { it.isNotBlank() }
                    ?: "Unsplash ${photo.id}"
                val photographer = photo.user?.name?.takeIf { it.isNotBlank() } ?: "Unsplash"
                Artwork(
                    id = "unsplash-${photo.id}",
                    title = title,
                    attribution = "$photographer / Unsplash",
                    sourceId = ID,
                    kind = ArtworkKind.Photo,
                    remoteUrl = imageUrl,
                )
            }
            RemoteSample.sample(mapped, limit, random)
        }.getOrDefault(emptyList())
        if (art.isNotEmpty()) {
            onLoaded(art)
            return art
        }
        return offlineFallback()
    }

    companion object {
        const val ID = "unsplash"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            query: String = RemoteCategoryMapping.stockQuery(
                StockPhotoCategory.Nature,
                RemoteProvider.Unsplash,
            )!!,
            perPage: Int = DEFAULT_LIMIT,
            page: Int = 1,
        ): String =
            "https://api.unsplash.com/search/photos" +
                "?query=${query.replace(" ", "%20")}" +
                "&orientation=landscape&per_page=$perPage&page=$page"
    }
}

@Serializable
internal data class UnsplashSearchPage(
    val results: List<UnsplashPhoto> = emptyList(),
)

@Serializable
internal data class UnsplashPhoto(
    val id: String,
    val description: String? = null,
    @SerialName("alt_description")
    val altDescription: String? = null,
    val urls: UnsplashUrls? = null,
    val user: UnsplashUser? = null,
)

@Serializable
internal data class UnsplashUrls(
    val regular: String? = null,
    val full: String? = null,
)

@Serializable
internal data class UnsplashUser(
    val name: String? = null,
)
