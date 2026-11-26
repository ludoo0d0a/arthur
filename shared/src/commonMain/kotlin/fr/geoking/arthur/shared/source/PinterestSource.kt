package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pinterest Remote Source for ambient photography (API v5 partner search).
 *
 * Docs: [API v5 introduction](https://developers.pinterest.com/docs/api/v5/introduction).
 *
 * Uses `GET /v5/search/partner/pins` — a **beta** endpoint that searches public Pins
 * by term (unlike `/v5/search/pins`, which only searches the token owner's Pins).
 * Access requires a Pinterest app approved for partner/trial search, plus an OAuth
 * access token with `pins:read` and `boards:read`.
 *
 * [httpGet] must send `Authorization: Bearer <accessToken>`; blank [accessToken]
 * uses [offlineFallback]. Pins are attribution-only (All-Rights-Reserved by default) —
 * link back to the Pin page; do not treat as open-license stock.
 *
 * Partner search returns a small page (docs: top 10) with optional [bookmark] paging;
 * each [load] advances the bookmark cursor when present.
 */
class PinterestSource(
    private val httpGet: suspend (url: String) -> String,
    private val accessToken: String,
    private val category: () -> StockPhotoCategory = { StockPhotoCategory.Nature },
    private val countryCode: () -> String = { DEFAULT_COUNTRY },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
) : Source {
    override val id: String = ID
    override val displayName: String = "Pinterest"

    private var bookmark: String? = null

    override suspend fun load(): List<Artwork> {
        if (accessToken.isBlank()) return offlineFallback()
        val term = RemoteCategoryMapping.stockQuery(category(), RemoteProvider.Pinterest)
        val art = runCatching {
            val payload = httpGet(
                searchUrl(
                    term = term,
                    countryCode = countryCode(),
                    bookmark = bookmark,
                    limit = PARTNER_PAGE_LIMIT,
                ),
            )
            val page = json.decodeFromString<PinterestSearchPage>(payload)
            bookmark = page.bookmark?.takeIf { it.isNotBlank() }
            val mapped = page.items.mapNotNull { pin ->
                val pinId = pin.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val imageUrl = imageUrlFromMedia(pin.media) ?: return@mapNotNull null
                val title = pin.title?.takeIf { it.isNotBlank() }
                    ?: pin.altText?.takeIf { it.isNotBlank() }
                    ?: "Pinterest $pinId"
                Artwork(
                    id = "pinterest-$pinId",
                    title = title,
                    attribution = "Pinterest",
                    sourceId = ID,
                    kind = ArtworkKind.Photo,
                    remoteUrl = imageUrl,
                    description = pin.description.orEmpty(),
                    license = "Pinterest — verify reuse rights before redistribution",
                    externalUrl = pin.link?.takeIf { it.isNotBlank() }
                        ?: "https://www.pinterest.com/pin/$pinId/",
                )
            }
            RemoteSample.sample(mapped, limit, random)
        }.getOrDefault(emptyList())
        if (art.isNotEmpty()) {
            onLoaded(art)
            return art
        }
        return offlineFallback()
    }

    companion object {
        const val ID = "pinterest"
        const val DEFAULT_LIMIT = 20
        const val DEFAULT_COUNTRY = "US"

        /** Partner search docs advertise a small page (top 10); keep within typical max. */
        const val PARTNER_PAGE_LIMIT = 10

        private val json = Json { ignoreUnknownKeys = true }

        private val preferredImageKeys = listOf("1200x", "600x", "400x300", "150x150")

        fun searchUrl(
            term: String = RemoteCategoryMapping.stockQuery(
                StockPhotoCategory.Nature,
                RemoteProvider.Pinterest,
            ),
            countryCode: String = DEFAULT_COUNTRY,
            bookmark: String? = null,
            limit: Int = PARTNER_PAGE_LIMIT,
        ): String {
            val encodedTerm = term.replace(" ", "%20")
            val base = "https://api.pinterest.com/v5/search/partner/pins" +
                "?term=$encodedTerm&country_code=$countryCode&limit=$limit"
            val bm = bookmark?.takeIf { it.isNotBlank() } ?: return base
            return "$base&bookmark=${bm.replace(" ", "%20")}"
        }

        internal fun imageUrlFromMedia(media: JsonObject?): String? {
            if (media == null) return null
            val images = media["images"]?.jsonObject ?: return null
            for (key in preferredImageKeys) {
                val url = images[key]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                if (!url.isNullOrBlank()) return url
            }
            return images.values.firstNotNullOfOrNull { value ->
                value.jsonObject["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            }
        }
    }
}

@Serializable
internal data class PinterestSearchPage(
    val items: List<PinterestSummaryPin> = emptyList(),
    val bookmark: String? = null,
)

@Serializable
internal data class PinterestSummaryPin(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("alt_text")
    val altText: String? = null,
    val link: String? = null,
    val media: JsonObject? = null,
)
