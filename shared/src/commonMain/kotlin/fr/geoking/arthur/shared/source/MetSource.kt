package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The Met Collection API Remote Source (no API key).
 * Open-access works with images only; [httpGet] is injected so unit tests use fixtures.
 *
 * Search contract: `GET /public/collection/v1/search`
 * (`q`, `medium`, `hasImages`, `isPublicDomain`) — tokens from [RemoteCategoryMapping].
 */
class MetSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "The Met"

    override suspend fun load(): List<Artwork> = runCatching {
        val targets = RemoteCategoryMapping.museumTargets(kind())
        val perKind = (limit / targets.size).coerceAtLeast(1)
        val results = mutableListOf<Artwork>()
        for (target in targets) {
            val searchJson = httpGet(searchUrl(target))
            val ids = parseSearchIds(searchJson).take(perKind)
            for (objectId in ids) {
                runCatching { loadArtwork(objectId, target) }.getOrNull()?.let { results.add(it) }
            }
        }
        results.take(limit)
    }.getOrDefault(emptyList())

    private suspend fun loadArtwork(objectId: Int, searchKind: MuseumSearchKind): Artwork? {
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
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
        )
    }

    companion object {
        const val ID = "met"
        const val DEFAULT_LIMIT = 20

        /** @deprecated Prefer [searchUrl] with [MuseumSearchKind]. */
        val SEARCH_URL: String = searchUrl(MuseumSearchKind.Painting)

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(kind: MuseumSearchKind = MuseumSearchKind.Painting): String {
            val params = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Met)
            val q = params.query.orEmpty()
            val medium = params.medium.orEmpty()
            return "https://collectionapi.metmuseum.org/public/collection/v1/search" +
                "?q=$q&medium=$medium&hasImages=true&isPublicDomain=true"
        }

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
