package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The Met Collection API Remote Source (no API key).
 * Open-access works with images only; [httpGet] is injected so unit tests use fixtures.
 */
class MetSource(
    private val httpGet: suspend (url: String) -> String,
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "The Met"

    override suspend fun load(): List<Artwork> = runCatching {
        val searchJson = httpGet(SEARCH_URL)
        val ids = parseSearchIds(searchJson).take(limit)
        ids.mapNotNull { objectId ->
            runCatching { loadArtwork(objectId) }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private suspend fun loadArtwork(objectId: Int): Artwork? {
        val obj = json.decodeFromString<MetObject>(httpGet(objectUrl(objectId)))
        val imageUrl = obj.primaryImage?.takeIf { it.isNotBlank() }
            ?: obj.primaryImageSmall?.takeIf { it.isNotBlank() }
            ?: return null
        if (!obj.isPublicDomain) return null
        val title = obj.title?.takeIf { it.isNotBlank() } ?: "Object $objectId"
        val attribution = obj.artistDisplayName?.takeIf { it.isNotBlank() } ?: "The Met"
        return Artwork(
            id = "met-$objectId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = ArtworkKind.Painting,
            remoteUrl = imageUrl,
        )
    }

    companion object {
        const val ID = "met"
        const val DEFAULT_LIMIT = 8
        const val SEARCH_URL =
            "https://collectionapi.metmuseum.org/public/collection/v1/search" +
                "?q=painting&hasImages=true&isPublicDomain=true"

        private val json = Json { ignoreUnknownKeys = true }

        fun objectUrl(objectId: Int): String =
            "https://collectionapi.metmuseum.org/public/collection/v1/objects/$objectId"

        internal fun parseSearchIds(payload: String): List<Int> {
            val page = json.decodeFromString<MetSearchPage>(payload)
            return page.objectIDs.orEmpty()
        }
    }
}

@Serializable
internal data class MetSearchPage(
    val total: Int = 0,
    val objectIDs: List<Int>? = null,
)

@Serializable
internal data class MetObject(
    val objectID: Int? = null,
    val isPublicDomain: Boolean = false,
    val title: String? = null,
    val artistDisplayName: String? = null,
    val primaryImage: String? = null,
    val primaryImageSmall: String? = null,
)
