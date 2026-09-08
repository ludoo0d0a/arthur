package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Harvard Art Museums Remote Source (API key required).
 * Objects with images only; blank [apiKey] yields an empty catalog.
 *
 * Search: `GET /object?classification=…&hasimage=1&apikey=…`
 * No native random: each [load] picks a random page and samples the window.
 */
class HarvardSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
    private val errorLogger: ErrorLogger? = null,
) : Source {
    override val id: String = ID
    override val displayName: String = "Harvard Art Museums"

    // Rotates which MuseumSearchKind target this Source hydrates each load() call
    // — defers the other target(s) to the next call instead of fetching them all now.
    private var targetCursor = 0

    // Advances each load() call so "load more" pages forward instead of re-sampling.
    private var pageCursor = 0

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) {
            errorLogger?.log(
                sourceId = id,
                category = ErrorCategory.Authentication,
                message = "API key for $displayName is missing or blank",
            )
            return emptyList()
        }
        return runCatching {
            MuseumLoad.acrossTargets(
                kind(),
                limit,
                random,
                nextTargetIndex = { targetCursor++ },
            ) { target, perKind ->
                val pageIndex = RemoteSample.nextPage(pageCursor++)
                val payload = RemoteSample.fetchWindow(
                    randomOffset = pageIndex,
                    firstOffset = 1,
                    fetch = { p ->
                        httpGet(searchUrl(apiKey, RemoteSample.SEARCH_POOL, target, page = p))
                    },
                    isEmpty = ::looksEmptyHarvard,
                )
                val page = json.decodeFromString<HarvardObjectPage>(payload)
                RemoteSample.sample(
                    page.records.orEmpty().mapNotNull { toArtwork(it, target) },
                    perKind,
                    random,
                )
            }
        }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            errorLogger?.log(
                sourceId = id,
                category = ErrorClassifier.classify(null, e),
                message = "Failed to load $displayName catalog",
                details = e.stackTraceToString().take(300),
                throwable = e,
            )
        }.getOrDefault(emptyList())
    }

    private fun looksEmptyHarvard(payload: String): Boolean =
        runCatching {
            json.decodeFromString<HarvardObjectPage>(payload).records.isNullOrEmpty()
        }.getOrDefault(true)

    private fun toArtwork(item: HarvardObject, searchKind: MuseumSearchKind): Artwork? {
        val objectId = item.id ?: return null
        val imageUrl = item.primaryImageUrl?.takeIf { it.isNotBlank() }
            ?: item.images?.firstOrNull()?.baseImageUrl?.takeIf { it.isNotBlank() }
            ?: return null
        val title = item.title?.takeIf { it.isNotBlank() } ?: "Object $objectId"
        val artist = item.people
            ?.firstOrNull { it.role.equals("Artist", ignoreCase = true) || it.displayname != null }
            ?.displayname
            ?.takeIf { it.isNotBlank() }
        val attribution = listOfNotNull(artist, "Harvard Art Museums").joinToString(" / ")
        val externalUrl = item.url?.takeIf { it.isNotBlank() }
            ?: "https://www.harvardartmuseums.org/collections/object/$objectId"
        return Artwork(
            id = "harvard-$objectId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
            externalUrl = externalUrl,
        )
    }

    companion object {
        const val ID = "harvard"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            apiKey: String,
            limit: Int = DEFAULT_LIMIT,
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
            /** Harvard page index; first page is 1. */
            page: Int = 1,
        ): String {
            val classification = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Harvard)
                .type
                .orEmpty()
            // hasimage=1 alone still returns permission-gated records with null
            // primaryimageurl (esp. Photographs). Level 0 = publicly viewable.
            return "https://api.harvardartmuseums.org/object" +
                "?apikey=$apiKey" +
                "&classification=$classification" +
                "&hasimage=1" +
                "&q=imagepermissionlevel%3A0" +
                "&size=$limit" +
                "&page=$page" +
                "&sort=random" +
                "&fields=id,title,primaryimageurl,people,classification,url,images"
        }
    }
}

@Serializable
internal data class HarvardObjectPage(
    val records: List<HarvardObject>? = emptyList(),
)

@Serializable
internal data class HarvardObject(
    val id: Int? = null,
    val title: String? = null,
    @SerialName("primaryimageurl") val primaryImageUrl: String? = null,
    val people: List<HarvardPerson>? = null,
    val classification: String? = null,
    val url: String? = null,
    val images: List<HarvardImage>? = null,
)

@Serializable
internal data class HarvardImage(
    @SerialName("baseimageurl") val baseImageUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alttext: String? = null,
)

@Serializable
internal data class HarvardPerson(
    val role: String? = null,
    val displayname: String? = null,
)
