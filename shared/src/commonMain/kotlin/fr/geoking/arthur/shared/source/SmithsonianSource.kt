package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Smithsonian Open Access Remote Source (API key required).
 * Prefers CC0 media with images; blank [apiKey] yields an empty catalog.
 *
 * Search: `GET /openaccess/api/v1.0/search?q=…&api_key=…`
 * No native random: each [load] picks a random `start` and samples the window.
 */
class SmithsonianSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Smithsonian"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return emptyList()
        return runCatching {
            val targets = RemoteCategoryMapping.museumTargets(kind())
            val perKind = (limit / targets.size).coerceAtLeast(1)
            val results = mutableListOf<Artwork>()
            for (target in targets) {
                val start = RemoteSample.randomStart(RemoteSample.SEARCH_POOL, random = random)
                val payload = httpGet(
                    searchUrl(apiKey, RemoteSample.SEARCH_POOL, target, start = start),
                ).let { body ->
                    if (start > 0 && looksEmptySmithsonian(body)) {
                        httpGet(searchUrl(apiKey, RemoteSample.SEARCH_POOL, target, start = 0))
                    } else {
                        body
                    }
                }
                val page = json.decodeFromString<SmithsonianSearchPage>(payload)
                val mapped = page.response?.rows.orEmpty().mapNotNull { toArtwork(it, target) }
                results.addAll(RemoteSample.sample(mapped, perKind, random))
            }
            RemoteSample.sample(results, limit, random)
        }.getOrDefault(emptyList())
    }

    private fun looksEmptySmithsonian(payload: String): Boolean =
        runCatching {
            json.decodeFromString<SmithsonianSearchPage>(payload).response?.rows.isNullOrEmpty()
        }.getOrDefault(true)

    private fun toArtwork(row: SmithsonianRow, searchKind: MuseumSearchKind): Artwork? {
        val recordId = row.url?.takeIf { it.isNotBlank() }
            ?: row.id?.takeIf { it.isNotBlank() }
            ?: return null
        val media = row.content?.descriptiveNonRepeating?.onlineMedia?.media
            ?.firstOrNull { media ->
                media.type.equals("Images", ignoreCase = true) &&
                    (media.usage?.access.equals("CC0", ignoreCase = true) ||
                        row.content.descriptiveNonRepeating.metadataUsage?.access
                            .equals("CC0", ignoreCase = true))
            }
            ?: row.content?.descriptiveNonRepeating?.onlineMedia?.media
                ?.firstOrNull { it.type.equals("Images", ignoreCase = true) }
            ?: return null
        val imageUrl = media.resources
            ?.firstOrNull { it.label.equals("Screen Image", ignoreCase = true) }
            ?.url
            ?.takeIf { it.isNotBlank() }
            ?: media.content?.takeIf { it.isNotBlank() }
            ?: return null
        val title = row.title?.takeIf { it.isNotBlank() }
            ?: row.content?.descriptiveNonRepeating?.title?.content?.takeIf { it.isNotBlank() }
            ?: "Smithsonian $recordId"
        val artist = row.content?.freetext?.name
            ?.firstOrNull { it.label.equals("Artist", ignoreCase = true) }
            ?.content
            ?.takeIf { it.isNotBlank() }
        val dataSource = row.content?.descriptiveNonRepeating?.dataSource
            ?.takeIf { it.isNotBlank() }
            ?: "Smithsonian"
        val attribution = listOfNotNull(artist, dataSource).joinToString(" / ")
        val slug = recordId.replace(':', '-').replace('/', '-')
        return Artwork(
            id = "smithsonian-$slug",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
        )
    }

    companion object {
        const val ID = "smithsonian"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            apiKey: String,
            limit: Int = DEFAULT_LIMIT,
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
            /** Smithsonian result offset; first item is 0. */
            start: Int = 0,
        ): String {
            val q = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Smithsonian).query
                .orEmpty()
            // Spaces must be encoded for fixture URL matching.
            val encoded = q.replace(" ", "%20")
            return "https://api.si.edu/openaccess/api/v1.0/search" +
                "?q=$encoded" +
                "&rows=$limit" +
                "&start=$start" +
                "&api_key=$apiKey"
        }
    }
}

@Serializable
internal data class SmithsonianSearchPage(
    val response: SmithsonianResponse? = null,
)

@Serializable
internal data class SmithsonianResponse(
    val rows: List<SmithsonianRow> = emptyList(),
)

@Serializable
internal data class SmithsonianRow(
    val id: String? = null,
    val title: String? = null,
    val url: String? = null,
    val content: SmithsonianContent? = null,
)

@Serializable
internal data class SmithsonianContent(
    val freetext: SmithsonianFreetext? = null,
    val descriptiveNonRepeating: SmithsonianDescriptive? = null,
)

@Serializable
internal data class SmithsonianFreetext(
    val name: List<SmithsonianLabeledText>? = null,
)

@Serializable
internal data class SmithsonianLabeledText(
    val label: String? = null,
    val content: String? = null,
)

@Serializable
internal data class SmithsonianDescriptive(
    val title: SmithsonianLabeledText? = null,
    @SerialName("data_source") val dataSource: String? = null,
    @SerialName("online_media") val onlineMedia: SmithsonianOnlineMedia? = null,
    @SerialName("metadata_usage") val metadataUsage: SmithsonianUsage? = null,
)

@Serializable
internal data class SmithsonianOnlineMedia(
    val media: List<SmithsonianMedia> = emptyList(),
)

@Serializable
internal data class SmithsonianMedia(
    val type: String? = null,
    val content: String? = null,
    val usage: SmithsonianUsage? = null,
    val resources: List<SmithsonianResource>? = null,
)

@Serializable
internal data class SmithsonianUsage(
    val access: String? = null,
)

@Serializable
internal data class SmithsonianResource(
    val label: String? = null,
    val url: String? = null,
)
