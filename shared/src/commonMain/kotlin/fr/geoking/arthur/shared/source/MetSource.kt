package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The Met Collection API Remote Source (no API key).
 * Open-access works with images only; [httpGet] is injected so unit tests use fixtures.
 *
 * Search is paginated ([offset]/[limit]); each [load] advances the window and samples
 * a subset, then hydrates those objects — renew Ambient by calling [load] again.
 *
 * Search contract: `GET /public/collection/v1.1/search`
 * (`q`, `medium`, `hasImages`, `isPublicDomain`, `offset`, `limit`) —
 * tokens from [RemoteCategoryMapping].
 */
class MetSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "The Met"

    // Rotates which MuseumSearchKind target this Source hydrates each load() call
    // — defers the other target(s) to the next call instead of fetching them all now.
    private var targetCursor = 0

    // Advances each load() call so "load more" pages forward instead of re-sampling.
    private var startCursor = 0

    // Caching resolved objects avoids re-running the per-object hydration call for ids
    // seen again when search windows overlap across renewals.
    private val hydratedCache = mutableMapOf<Int, Artwork>()

    override suspend fun load(): List<Artwork> = load(limit = limit)

    override suspend fun load(limit: Int): List<Artwork> = runCatching {
        MuseumLoad.acrossTargets(
            kind(),
            limit,
            random,
            nextTargetIndex = { targetCursor++ },
        ) { target, perKind ->
            val offset = RemoteSample.nextStart(
                startCursor++,
                pageSize = RemoteSample.SEARCH_POOL,
                maxStart = RemoteSample.MET_MAX_START,
            )
            val payload = RemoteSample.fetchWindow(
                randomOffset = offset,
                firstOffset = 0,
                fetch = { o ->
                    httpGet(searchUrl(target, limit = RemoteSample.SEARCH_POOL, offset = o))
                },
                isEmpty = ::looksEmptyMet,
            )
            val ids = RemoteSample.sample(parseSearchIds(payload), perKind, random)
            ids.mapNotNull { objectId ->
                hydratedCache[objectId]
                    ?: runCatching { loadArtwork(objectId, target) }.getOrNull()
                        ?.also { hydratedCache[objectId] = it }
            }
        }
    }.getOrDefault(emptyList())

    private fun looksEmptyMet(payload: String): Boolean =
        runCatching { parseSearchIds(payload).isEmpty() }.getOrDefault(true)

    private suspend fun loadArtwork(objectId: Int, searchKind: MuseumSearchKind): Artwork? {
        val payload = runCatching { httpGet(objectUrl(objectId)) }.getOrNull() ?: return null
        val obj = runCatching { json.decodeFromString<MetObject>(payload) }.getOrNull() ?: return null
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
            description = obj.creditLine?.takeIf { it.isNotBlank() }.orEmpty(),
            date = obj.objectDate?.takeIf { it.isNotBlank() }.orEmpty(),
            medium = obj.medium?.takeIf { it.isNotBlank() }.orEmpty(),
            license = if (obj.isPublicDomain) "Public Domain" else "",
            externalUrl = collectionPageUrl(objectId),
        )
    }

    companion object {
        const val ID = "met"
        const val DEFAULT_LIMIT = 20

        /** @deprecated Prefer [searchUrl] with [MuseumSearchKind]. */
        val SEARCH_URL: String = searchUrl(MuseumSearchKind.Painting)

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
            limit: Int = RemoteSample.SEARCH_POOL,
            offset: Int = 0,
        ): String {
            val params = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Met)
            val q = params.query.orEmpty()
            val medium = params.medium.orEmpty()
            return "https://collectionapi.metmuseum.org/public/collection/v1.1/search" +
                "?q=$q&medium=$medium&hasImages=true&isPublicDomain=true" +
                "&offset=$offset&limit=$limit"
        }

        fun objectUrl(objectId: Int): String =
            "https://collectionapi.metmuseum.org/public/collection/v1/objects/$objectId"

        fun collectionPageUrl(objectId: Int): String =
            "https://www.metmuseum.org/art/collection/search/$objectId"

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
    val objectDate: String? = null,
    val medium: String? = null,
    val creditLine: String? = null,
)
