package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Europeana Search API Remote Source (API key required).
 * Open-reusability image records only; blank [apiKey] yields an empty catalog.
 * [httpGet] should send `X-Api-Key: <apiKey>` (preferred over deprecated `wskey`).
 *
 * Search: `GET /record/v2/search.json?query=…&reusability=open&media=true&qf=TYPE:IMAGE`
 * No native random: each [load] picks a random `start` and samples the window.
 */
class EuropeanaSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
    private val errorLogger: ErrorLogger? = null,
) : Source {
    override val id: String = ID
    override val displayName: String = "Europeana"

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
            MuseumLoad.acrossTargets(kind(), limit, random) { target, perKind ->
                val start = 1 + RemoteSample.randomStart(
                    pageSize = RemoteSample.SEARCH_POOL,
                    maxStart = RemoteSample.EUROPEANA_MAX_START,
                    random = random,
                )
                val payload = RemoteSample.fetchWindow(
                    randomOffset = start,
                    firstOffset = 1,
                    fetch = { s -> httpGet(searchUrl(RemoteSample.SEARCH_POOL, target, start = s, apiKey = apiKey)) },
                    isEmpty = ::looksEmptyEuropeana,
                )
                val page = json.decodeFromString<EuropeanaSearchPage>(payload)
                RemoteSample.sample(page.items.mapNotNull { toArtwork(it, target) }, perKind, random)
            }
        }.onFailure { e ->
            errorLogger?.log(
                sourceId = id,
                category = ErrorClassifier.classify(null, e),
                message = "Failed to load $displayName catalog",
                details = e.stackTraceToString().take(300),
                throwable = e,
            )
        }.getOrDefault(emptyList())
    }

    private fun looksEmptyEuropeana(payload: String): Boolean =
        runCatching {
            json.decodeFromString<EuropeanaSearchPage>(payload).items.isEmpty()
        }.getOrDefault(true)

    private fun toArtwork(item: EuropeanaItem, searchKind: MuseumSearchKind): Artwork? {
        val recordId = item.id?.takeIf { it.isNotBlank() } ?: return null
        val imageUrl = item.edmIsShownBy?.firstOrNull { it.isNotBlank() }
            ?: item.edmPreview?.firstOrNull { it.isNotBlank() }
            ?: return null
        val title = item.title?.firstOrNull { it.isNotBlank() } ?: "Europeana $recordId"
        val creator = item.dcCreator?.firstOrNull { it.isNotBlank() }
        val provider = item.dataProvider?.firstOrNull { it.isNotBlank() }
        val attribution = listOfNotNull(creator, provider, "Europeana")
            .distinct()
            .joinToString(" / ")
        val slug = recordId.trimStart('/').replace('/', '-')
        return Artwork(
            id = "europeana-$slug",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
            externalUrl = item.guid?.takeIf { it.isNotBlank() }
                ?: "https://www.europeana.eu/item$recordId",
        )
    }

    companion object {
        const val ID = "europeana"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            limit: Int = DEFAULT_LIMIT,
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
            /** Europeana result offset; first item is 1. */
            start: Int = 1,
            apiKey: String = "",
        ): String {
            val params = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Europeana)
            val rawQuery = params.query.orEmpty().ifBlank { "*" }
            val query = rawQuery.replace(" ", "%20")
            val themeParam = params.medium?.takeIf { it.isNotBlank() }?.let { "&theme=$it" }.orEmpty()
            val wskeyParam = if (apiKey.isNotBlank()) "&wskey=$apiKey" else ""
            // Encode qf value so `TYPE:IMAGE` survives strict URL parsers.
            return "https://api.europeana.eu/record/v2/search.json" +
                "?query=$query$themeParam&reusability=open&media=true&qf=TYPE%3AIMAGE" +
                "&rows=$limit&start=$start&profile=standard$wskeyParam"
        }
    }
}

@Serializable
internal data class EuropeanaSearchPage(
    val items: List<EuropeanaItem> = emptyList(),
)

@Serializable
internal data class EuropeanaItem(
    val id: String? = null,
    val title: List<String>? = null,
    val dcCreator: List<String>? = null,
    val dataProvider: List<String>? = null,
    val edmPreview: List<String>? = null,
    val edmIsShownBy: List<String>? = null,
    val guid: String? = null,
    val type: String? = null,
)
