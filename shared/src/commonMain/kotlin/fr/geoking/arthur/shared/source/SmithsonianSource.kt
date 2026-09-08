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
    private val errorLogger: ErrorLogger? = null,
) : Source {
    override val id: String = ID
    override val displayName: String = "Smithsonian"

    // Rotates which MuseumSearchKind target this Source hydrates each load() call
    // — defers the other target(s) to the next call instead of fetching them all now.
    private var targetCursor = 0

    // Advances each load() call so "load more" pages forward instead of re-sampling.
    private var startCursor = 0

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
                val start = RemoteSample.nextStart(startCursor++, RemoteSample.SEARCH_POOL)
                val payload = RemoteSample.fetchWindow(
                    randomOffset = start,
                    firstOffset = 0,
                    fetch = { s ->
                        httpGet(searchUrl(apiKey, RemoteSample.SEARCH_POOL, target, start = s))
                    },
                    isEmpty = ::looksEmptySmithsonian,
                )
                val page = json.decodeFromString<SmithsonianSearchPage>(payload)
                RemoteSample.sample(
                    page.response?.rows.orEmpty().mapNotNull { toArtwork(it, target) },
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

    private fun looksEmptySmithsonian(payload: String): Boolean =
        runCatching {
            json.decodeFromString<SmithsonianSearchPage>(payload).response?.rows.isNullOrEmpty()
        }.getOrDefault(true)

    private fun toArtwork(row: SmithsonianRow, searchKind: MuseumSearchKind): Artwork? {
        val recordId = row.url?.takeIf { it.isNotBlank() }
            ?: row.id?.takeIf { it.isNotBlank() }
            ?: return null
        val mediaList = row.content?.descriptiveNonRepeating?.onlineMedia?.media.orEmpty()
        val media = mediaList
            .firstOrNull { media ->
                (media.type.isNullOrBlank() || media.type.equals("Images", ignoreCase = true)) &&
                    (media.usage?.access.equals("CC0", ignoreCase = true) ||
                        row.content?.descriptiveNonRepeating?.metadataUsage?.access
                            .equals("CC0", ignoreCase = true))
            }
            ?: mediaList.firstOrNull { media ->
                media.type.isNullOrBlank() || media.type.equals("Images", ignoreCase = true)
            }
            ?: return null
        val imageUrl = media.resources
            ?.firstOrNull {
                it.label.equals("Screen Image", ignoreCase = true) ||
                    it.label.equals("High-resolution JPEG", ignoreCase = true)
            }
            ?.url
            ?.takeIf { it.isNotBlank() }
            ?: media.resources?.firstOrNull { !it.url.isNullOrBlank() }?.url
            ?: media.content?.takeIf { it.isNotBlank() }
            ?: return null
        val title = row.title?.takeIf { it.isNotBlank() }
            ?: row.content?.descriptiveNonRepeating?.title?.content?.takeIf { it.isNotBlank() }
            ?: "Smithsonian $recordId"
        val artist = row.content?.freetext?.name
            ?.firstOrNull { it.label.equals("Artist", ignoreCase = true) || it.label == null }
            ?.content
            ?.takeIf { it.isNotBlank() }
        val dataSource = row.content?.descriptiveNonRepeating?.dataSource
            ?.takeIf { it.isNotBlank() }
            ?: "Smithsonian"
        val attribution = listOfNotNull(artist, dataSource).distinct().joinToString(" / ")
        val externalUrl = row.content?.descriptiveNonRepeating?.recordLink?.takeIf { it.isNotBlank() }
            ?: row.content?.descriptiveNonRepeating?.guid?.takeIf { it.isNotBlank() }
            ?: row.url?.takeIf { it.isNotBlank() && it.startsWith("http") }
        val slug = recordId.replace(':', '-').replace('/', '-')
        return Artwork(
            id = "smithsonian-$slug",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
            externalUrl = externalUrl,
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
            // Encode reserved query chars (`:`, spaces) for fixture URL matching + HTTP stacks.
            val encoded = buildString(q.length + 8) {
                for (ch in q) {
                    when (ch) {
                        ' ' -> append("%20")
                        ':' -> append("%3A")
                        else -> append(ch)
                    }
                }
            }
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
    val rows: List<SmithsonianRow>? = emptyList(),
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
    val guid: String? = null,
    @SerialName("data_source") val dataSource: String? = null,
    @SerialName("record_link") val recordLink: String? = null,
    @SerialName("online_media") val onlineMedia: SmithsonianOnlineMedia? = null,
    @SerialName("metadata_usage") val metadataUsage: SmithsonianUsage? = null,
)

@Serializable
internal data class SmithsonianOnlineMedia(
    val media: List<SmithsonianMedia>? = emptyList(),
    @SerialName("mediaCount") val mediaCount: Int? = null,
)

@Serializable
internal data class SmithsonianMedia(
    val id: String? = null,
    val type: String? = null,
    val content: String? = null,
    val thumbnail: String? = null,
    @SerialName("idsId") val idsId: String? = null,
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
    val width: Int? = null,
    val height: Int? = null,
)
