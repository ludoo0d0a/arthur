package fr.geoking.arthur.source

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Fetches quotes from the selected [QuoteProvider], caches them (TTL), and cycles
 * one quote per Ambient slide. Failures are soft: [nextQuote] returns null so
 * Ambient can omit the overlay line.
 */
class QuoteRepository(
    context: Context,
    private val httpGet: suspend (url: String) -> String,
    private val provider: () -> QuoteProvider,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var quotes: List<Quote> = emptyList()
    private var loadedAtMs: Long = 0L
    private var nextIndex: Int = 0
    private var cachedProviderId: String? = null

    init {
        restoreFromPrefs()
    }

    /** Next quote from the cache, refreshing the batch when expired, empty, or provider changed. */
    suspend fun nextQuote(): Quote? = mutex.withLock {
        ensureLoadedLocked()
        if (quotes.isEmpty()) return null
        val quote = quotes[nextIndex % quotes.size]
        nextIndex = (nextIndex + 1) % quotes.size
        prefs.edit().putInt(KEY_INDEX, nextIndex).apply()
        quote
    }

    private suspend fun ensureLoadedLocked() {
        val wanted = provider()
        val now = clock()
        if (
            quotes.isNotEmpty() &&
            cachedProviderId == wanted.id &&
            now - loadedAtMs < TTL_MS
        ) {
            return
        }
        val fetched = runCatching { fetchBatch(wanted) }.getOrElse { emptyList() }
        if (fetched.isEmpty()) {
            // Keep stale cache if network failed and we still have quotes for this provider.
            if (quotes.isNotEmpty() && cachedProviderId == wanted.id) return
            return
        }
        quotes = fetched
        loadedAtMs = now
        nextIndex = 0
        cachedProviderId = wanted.id
        prefs.edit()
            .putString(KEY_CACHE_JSON, encodeQuotes(quotes))
            .putLong(KEY_CACHE_AT, loadedAtMs)
            .putInt(KEY_INDEX, nextIndex)
            .putString(KEY_CACHE_PROVIDER, wanted.id)
            .apply()
    }

    private suspend fun fetchBatch(provider: QuoteProvider): List<Quote> = when (provider) {
        QuoteProvider.ZenQuotes -> {
            val body = httpGet(ZENQUOTES_API_URL)
            parseZenQuotes(body)
        }
        QuoteProvider.CitationLecog -> fetchCitationLecogBatch()
    }

    private suspend fun fetchCitationLecogBatch(): List<Quote> {
        val out = ArrayList<Quote>(LECOG_BATCH_SIZE)
        val seen = HashSet<String>()
        repeat(LECOG_BATCH_SIZE) {
            val body = runCatching { httpGet(LECOG_API_URL) }.getOrNull() ?: return@repeat
            val quote = parseCitationLecog(body) ?: return@repeat
            if (seen.add(quote.text)) out.add(quote)
        }
        return out
    }

    private fun restoreFromPrefs() {
        val raw = prefs.getString(KEY_CACHE_JSON, null) ?: return
        val at = prefs.getLong(KEY_CACHE_AT, 0L)
        val parsed = runCatching { parseCachedQuotes(raw) }.getOrElse { emptyList() }
        if (parsed.isEmpty()) return
        quotes = parsed
        loadedAtMs = at
        nextIndex = prefs.getInt(KEY_INDEX, 0).coerceIn(0, parsed.lastIndex.coerceAtLeast(0))
        cachedProviderId = prefs.getString(KEY_CACHE_PROVIDER, null)
    }

    companion object {
        const val ZENQUOTES_API_URL = "https://zenquotes.io/api/quotes"
        const val LECOG_API_URL = "https://citation.lecog.fr/public/api/random-quote.php"
        /** Prefetch size for Citation.lecog (no batch endpoint; stay well under 100 req/h). */
        const val LECOG_BATCH_SIZE = 15
        /** Refresh the batch every 4 hours. */
        const val TTL_MS = 4L * 60L * 60L * 1000L

        private const val PREFS = "arthur_quotes_cache"
        private const val KEY_CACHE_JSON = "cache_json"
        private const val KEY_CACHE_AT = "cache_at"
        private const val KEY_INDEX = "next_index"
        private const val KEY_CACHE_PROVIDER = "cache_provider"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        internal fun parseZenQuotes(body: String): List<Quote> =
            json.parseToJsonElement(body).jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val text = obj["q"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (text.isEmpty()) null
                else Quote(
                    text = text,
                    author = obj["a"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                )
            }

        internal fun parseCitationLecog(body: String): Quote? {
            val root = json.parseToJsonElement(body).jsonObject
            if (root["success"]?.jsonPrimitive?.contentOrNull == "false") return null
            val data = root["data"]?.jsonObject ?: return null
            val text = data["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (text.isEmpty()) return null
            val authorObj = data["author"]?.jsonObject
            val forename = authorObj?.get("forename")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val name = authorObj?.get("name")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val author = listOf(forename, name).filter { it.isNotEmpty() }.joinToString(" ")
            return Quote(text = text, author = author)
        }

        internal fun parseCachedQuotes(body: String): List<Quote> =
            json.parseToJsonElement(body).jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (text.isEmpty()) null
                else Quote(
                    text = text,
                    author = obj["author"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty(),
                )
            }

        internal fun encodeQuotes(quotes: List<Quote>): String =
            buildJsonArray {
                quotes.forEach { quote ->
                    add(
                        buildJsonObject {
                            put("text", quote.text)
                            put("author", quote.author)
                        },
                    )
                }
            }.toString()
    }
}
