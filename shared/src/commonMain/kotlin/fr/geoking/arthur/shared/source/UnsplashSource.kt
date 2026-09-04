package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
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
 */
class UnsplashSource(
    private val httpGet: suspend (url: String) -> String,
    private val accessKey: String,
    private val query: () -> String = { StockPhotoCategory.Nature.query },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Unsplash"

    override suspend fun load(): List<Artwork> {
        if (accessKey.isBlank()) return offlineFallback()
        val art = runCatching {
            val page = json.decodeFromString<UnsplashSearchPage>(httpGet(searchUrl(query(), limit)))
            page.results.mapNotNull { photo ->
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
        }.getOrDefault(emptyList())
        if (art.isNotEmpty()) {
            onLoaded(art)
            return art
        }
        return offlineFallback()
    }

    companion object {
        const val ID = "unsplash"
        const val DEFAULT_LIMIT = 8

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            query: String = StockPhotoCategory.Nature.query,
            perPage: Int = DEFAULT_LIMIT,
        ): String =
            "https://api.unsplash.com/search/photos" +
                "?query=$query&orientation=landscape&per_page=$perPage"
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
