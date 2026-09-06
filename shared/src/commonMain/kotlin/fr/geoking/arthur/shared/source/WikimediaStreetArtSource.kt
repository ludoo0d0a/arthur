package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wikimedia Commons street-art Remote Source (no API key).
 * Loads files from [CATEGORY] via the MediaWiki API; keeps open licenses only
 * (PD / CC0 / CC BY / CC BY-SA — no NC/ND). [httpGet] is injected for fixtures.
 *
 * Distinct from museum connectors: Wikimedia is not the famous-art Source (ADR 0008).
 * Each [load] samples a random subset of the fetched category window.
 */
class WikimediaStreetArtSource(
    private val httpGet: suspend (url: String) -> String,
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Wikimedia Street Art"

    override suspend fun load(): List<Artwork> = runCatching {
        val payload = httpGet(searchUrl(limit = limit.coerceAtLeast(1) * FETCH_MULTIPLIER))
        val response = json.decodeFromString<WikimediaQueryResponse>(payload)
        val mapped = response.query?.pages.orEmpty().mapNotNull { toArtwork(it) }
        RemoteSample.sample(mapped, limit, random)
    }.getOrDefault(emptyList())

    private fun toArtwork(page: WikimediaPage): Artwork? {
        val pageId = page.pageid ?: return null
        val info = page.imageinfo?.firstOrNull() ?: return null
        if (!isAllowedMime(info.mime)) return null
        val license = info.extmetadata?.LicenseShortName?.value
        if (!isOpenLicense(license)) return null
        val imageUrl = info.thumburl?.takeIf { it.isNotBlank() }
            ?: info.url?.takeIf { it.isNotBlank() }
            ?: return null
        val title = cleanTitle(page.title) ?: "Street art $pageId"
        val artist = stripHtml(info.extmetadata?.Artist?.value)
            ?.takeIf { it.isNotBlank() }
        val attribution = listOfNotNull(artist, "Wikimedia Commons").joinToString(" / ")
        return Artwork(
            id = "wikimedia-streetart-$pageId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = ArtworkKind.Painting,
            remoteUrl = imageUrl,
        )
    }

    companion object {
        const val ID = "wikimedia-streetart"
        const val DEFAULT_LIMIT = 20
        const val CATEGORY = "Category:Street_art"
        private const val FETCH_MULTIPLIER = 3
        private const val THUMB_WIDTH = 1600

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            limit: Int = DEFAULT_LIMIT * FETCH_MULTIPLIER,
            category: String = CATEGORY,
        ): String {
            val encodedCategory = category.replace(" ", "_")
            return "https://commons.wikimedia.org/w/api.php" +
                "?action=query" +
                "&generator=categorymembers" +
                "&gcmtitle=$encodedCategory" +
                "&gcmtype=file" +
                "&gcmlimit=$limit" +
                "&prop=imageinfo" +
                "&iiprop=url|extmetadata|mime" +
                "&iiurlwidth=$THUMB_WIDTH" +
                "&format=json" +
                "&formatversion=2"
        }

        fun isOpenLicense(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val n = name.trim()
            if (n.contains("NC", ignoreCase = true) || n.contains("ND", ignoreCase = true)) {
                return false
            }
            return n.equals("Public domain", ignoreCase = true) ||
                n.equals("CC0", ignoreCase = true) ||
                n.startsWith("CC BY", ignoreCase = true) ||
                n.contains("PDM", ignoreCase = true)
        }

        fun isAllowedMime(mime: String?): Boolean = when (mime?.lowercase()) {
            "image/jpeg", "image/png", "image/webp" -> true
            else -> false
        }

        fun cleanTitle(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            var t = raw.removePrefix("File:").trim()
            val dot = t.lastIndexOf('.')
            if (dot > 0) t = t.substring(0, dot)
            return t.takeIf { it.isNotBlank() }
        }

        fun stripHtml(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            return raw
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace(Regex("\\s+"), " ")
                .trim()
                .takeIf { it.isNotBlank() }
        }
    }
}

@Serializable
internal data class WikimediaQueryResponse(
    val query: WikimediaQuery? = null,
)

@Serializable
internal data class WikimediaQuery(
    val pages: List<WikimediaPage> = emptyList(),
)

@Serializable
internal data class WikimediaPage(
    val pageid: Long? = null,
    val title: String? = null,
    val imageinfo: List<WikimediaImageInfo>? = null,
)

@Serializable
internal data class WikimediaImageInfo(
    val url: String? = null,
    val thumburl: String? = null,
    val mime: String? = null,
    val extmetadata: WikimediaExtMetadata? = null,
)

@Serializable
internal data class WikimediaExtMetadata(
    val LicenseShortName: WikimediaMetaValue? = null,
    val Artist: WikimediaMetaValue? = null,
)

@Serializable
internal data class WikimediaMetaValue(
    val value: String? = null,
)
