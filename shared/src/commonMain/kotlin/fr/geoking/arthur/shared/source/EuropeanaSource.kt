package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Europeana Search API Remote Source (API key required).
 * Open-reusability image records only; blank [apiKey] yields an empty catalog.
 * [httpGet] should send `X-Api-Key: <apiKey>` (preferred over deprecated `wskey`).
 *
 * Search: `GET /record/v2/search.json?query=…&reusability=open&media=true&qf=TYPE:IMAGE`
 */
class EuropeanaSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Europeana"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return emptyList()
        return runCatching {
            val targets = RemoteCategoryMapping.museumTargets(kind())
            val perKind = (limit / targets.size).coerceAtLeast(1)
            val results = mutableListOf<Artwork>()
            for (target in targets) {
                val payload = httpGet(searchUrl(perKind, target))
                val page = json.decodeFromString<EuropeanaSearchPage>(payload)
                page.items
                    .asSequence()
                    .mapNotNull { toArtwork(it, target) }
                    .take(perKind)
                    .forEach { results.add(it) }
            }
            results.take(limit)
        }.getOrDefault(emptyList())
    }

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
        ): String {
            val query = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Europeana).query
                .orEmpty()
            return "https://api.europeana.eu/record/v2/search.json" +
                "?query=$query&reusability=open&media=true&qf=TYPE:IMAGE" +
                "&rows=$limit&start=$start&profile=standard"
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
    val type: String? = null,
)
