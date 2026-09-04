package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pexels stock-photo Remote Source (API key required).
 * [httpGet] must send `Authorization: <apiKey>`; blank [apiKey] skips the network.
 */
class PexelsSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Pexels"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return emptyList()
        return runCatching {
            val page = json.decodeFromString<PexelsSearchPage>(httpGet(searchUrl(limit)))
            page.photos.mapNotNull { photo ->
                val imageUrl = photo.src?.large2x?.takeIf { it.isNotBlank() }
                    ?: photo.src?.large?.takeIf { it.isNotBlank() }
                    ?: photo.src?.original?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val title = photo.alt?.takeIf { it.isNotBlank() } ?: "Pexels ${photo.id}"
                val photographer = photo.photographer?.takeIf { it.isNotBlank() } ?: "Pexels"
                Artwork(
                    id = "pexels-${photo.id}",
                    title = title,
                    attribution = "$photographer / Pexels",
                    sourceId = ID,
                    kind = ArtworkKind.Photo,
                    remoteUrl = imageUrl,
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val ID = "pexels"
        const val DEFAULT_LIMIT = 8

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(perPage: Int = DEFAULT_LIMIT): String =
            "https://api.pexels.com/v1/search" +
                "?query=nature&orientation=landscape&per_page=$perPage"
    }
}

@Serializable
internal data class PexelsSearchPage(
    val photos: List<PexelsPhoto> = emptyList(),
)

@Serializable
internal data class PexelsPhoto(
    val id: Long,
    val alt: String? = null,
    val photographer: String? = null,
    val src: PexelsSrc? = null,
)

@Serializable
internal data class PexelsSrc(
    val original: String? = null,
    val large: String? = null,
    val large2x: String? = null,
)
