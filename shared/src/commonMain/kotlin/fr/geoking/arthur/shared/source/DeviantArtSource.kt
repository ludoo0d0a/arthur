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
 * DeviantArt Browse API Remote Source (community digital art).
 *
 * DeviantArt only exposes read-only Browse access via OAuth2 client-credentials:
 * a token request (`grant_type=client_credentials`) followed by `browse/tags`
 * calls carrying `access_token`. Both requests are plain GETs (DeviantArt's own
 * docs demonstrate the token endpoint as a GET with query params), so this Source
 * fits the same injected [httpGet] shape as every other Remote Source. Blank
 * [clientId] / [clientSecret] yields [offlineFallback]. A fresh token is fetched
 * on every [load] — Browse volume is low (one call per rotation refresh), so this
 * stays simple rather than adding token-expiry bookkeeping.
 *
 * Deviations default to All-Rights-Reserved unless the artist opts into a
 * Creative Commons license, and Browse does not expose per-item license data —
 * this Source keeps full attribution plus a link back to the deviation page, but
 * reuse beyond in-app display needs a ToS / license review (see
 * `docs/roadmap-art-community-sources.md`).
 *
 * No native random: each [load] picks a random `offset` and samples the window.
 */
class DeviantArtSource(
    private val httpGet: suspend (url: String) -> String,
    private val clientId: String,
    private val clientSecret: String,
    private val category: () -> StockPhotoCategory = { StockPhotoCategory.Nature },
    private val offlineFallback: () -> List<Artwork> = { emptyList() },
    private val onLoaded: (List<Artwork>) -> Unit = {},
    private val limit: Int = DEFAULT_LIMIT,
    private val random: Random = Random.Default,
    private val errorLogger: ErrorLogger? = null,
) : Source {
    override val id: String = ID
    override val displayName: String = "DeviantArt"

    override suspend fun load(): List<Artwork> {
        if (clientId.isBlank() || clientSecret.isBlank()) return offlineFallback()
        val tag = RemoteCategoryMapping.stockQuery(category(), RemoteProvider.DeviantArt)
            ?: return emptyList()
        val art = runCatching {
            val token = json.decodeFromString<DeviantArtToken>(
                httpGet(tokenUrl(clientId, clientSecret)),
            ).accessToken?.takeIf { it.isNotBlank() } ?: return@runCatching emptyList()
            val offset = RemoteSample.randomStart(pageSize = RemoteSample.SEARCH_POOL, random = random)
            val payload = RemoteSample.fetchWindow(
                randomOffset = offset,
                firstOffset = 0,
                fetch = { o -> httpGet(browseUrl(tag, token, RemoteSample.SEARCH_POOL, o)) },
                isEmpty = { body -> json.decodeFromString<DeviantArtBrowsePage>(body).results.isEmpty() },
            )
            val mapped = json.decodeFromString<DeviantArtBrowsePage>(payload).results.mapNotNull { d ->
                if (d.isMature == true) return@mapNotNull null
                val imageUrl = d.content?.src?.takeIf { it.isNotBlank() }
                    ?: d.preview?.src?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val deviationId = d.deviationid?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val author = d.author?.username?.takeIf { it.isNotBlank() } ?: "DeviantArt"
                Artwork(
                    id = "deviantart-$deviationId",
                    title = d.title?.takeIf { it.isNotBlank() } ?: "DeviantArt $deviationId",
                    attribution = "$author / DeviantArt",
                    sourceId = ID,
                    kind = ArtworkKind.Painting,
                    remoteUrl = imageUrl,
                    license = "DeviantArt — verify reuse rights before redistribution",
                    externalUrl = d.url?.takeIf { it.isNotBlank() },
                )
            }
            RemoteSample.sample(mapped, limit, random)
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
        if (art.isNotEmpty()) {
            onLoaded(art)
            return art
        }
        return offlineFallback()
    }

    companion object {
        const val ID = "deviantart"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun tokenUrl(clientId: String, clientSecret: String): String =
            "https://www.deviantart.com/oauth2/token" +
                "?grant_type=client_credentials&client_id=$clientId&client_secret=$clientSecret"

        fun browseUrl(
            tag: String,
            accessToken: String,
            limit: Int = DEFAULT_LIMIT,
            offset: Int = 0,
        ): String =
            "https://www.deviantart.com/api/v1/oauth2/browse/tags" +
                "?tag=${tag.replace(" ", "")}&access_token=$accessToken" +
                "&limit=$limit&offset=$offset&mature_content=false"
    }
}

@Serializable
internal data class DeviantArtToken(
    @SerialName("access_token")
    val accessToken: String? = null,
)

@Serializable
internal data class DeviantArtBrowsePage(
    val results: List<DeviantArtDeviation> = emptyList(),
)

@Serializable
internal data class DeviantArtDeviation(
    val deviationid: String? = null,
    val title: String? = null,
    val url: String? = null,
    val author: DeviantArtAuthor? = null,
    val content: DeviantArtMedia? = null,
    val preview: DeviantArtMedia? = null,
    @SerialName("is_mature")
    val isMature: Boolean? = null,
)

@Serializable
internal data class DeviantArtAuthor(
    val username: String? = null,
)

@Serializable
internal data class DeviantArtMedia(
    val src: String? = null,
)
