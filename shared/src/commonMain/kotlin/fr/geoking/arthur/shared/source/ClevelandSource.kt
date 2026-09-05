package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cleveland Museum of Art Open Access Remote Source (no API key).
 * CC0 works with JPEG images only; [httpGet] is injected for fixtures.
 */
class ClevelandSource(
    private val httpGet: suspend (url: String) -> String,
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Cleveland Museum of Art"

    override suspend fun load(): List<Artwork> = runCatching {
        val payload = httpGet(searchUrl(limit))
        val page = json.decodeFromString<ClevelandSearchPage>(payload)
        page.data
            .asSequence()
            .mapNotNull { toArtwork(it) }
            .take(limit)
            .toList()
    }.getOrDefault(emptyList())

    private fun toArtwork(item: ClevelandArtwork): Artwork? {
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
            kind = parseKind(item.type),
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
        const val DEFAULT_LIMIT = 8

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(limit: Int = DEFAULT_LIMIT): String =
            "https://openaccess-api.clevelandart.org/api/artworks/" +
                "?cc0=1&has_image=1&limit=$limit&type=Painting"
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
