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
 * Fetches a batch of ZenQuotes, caches them (TTL), and cycles one quote per Ambient slide.
 * Failures are soft: [nextQuote] returns null so Ambient can omit the overlay line.
 */
class QuoteRepository(
    context: Context,
    private val httpGet: suspend (url: String) -> String,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var quotes: List<Quote> = emptyList()
    private var loadedAtMs: Long = 0L
    private var nextIndex: Int = 0

    init {
        restoreFromPrefs()
    }

    /** Next quote from the cache, refreshing the batch when expired or empty. */
    suspend fun nextQuote(): Quote? = mutex.withLock {
        ensureLoadedLocked()
        if (quotes.isEmpty()) return null
        val quote = quotes[nextIndex % quotes.size]
        nextIndex = (nextIndex + 1) % quotes.size
        prefs.edit().putInt(KEY_INDEX, nextIndex).apply()
        quote
    }

    private suspend fun ensureLoadedLocked() {
        val now = clock()
        if (quotes.isNotEmpty() && now - loadedAtMs < TTL_MS) return
        val fetched = runCatching { fetchBatch() }.getOrElse { emptyList() }
        if (fetched.isEmpty()) {
            // Keep stale cache if network failed and we still have quotes.
            if (quotes.isNotEmpty()) return
            return
        }
        quotes = fetched
        loadedAtMs = now
        nextIndex = 0
        prefs.edit()
            .putString(KEY_CACHE_JSON, encodeQuotes(quotes))
            .putLong(KEY_CACHE_AT, loadedAtMs)
            .putInt(KEY_INDEX, nextIndex)
            .apply()
    }

    private suspend fun fetchBatch(): List<Quote> {
        val body = httpGet(API_URL)
        return parseZenQuotes(body)
    }

    private fun restoreFromPrefs() {
        val raw = prefs.getString(KEY_CACHE_JSON, null) ?: return
        val at = prefs.getLong(KEY_CACHE_AT, 0L)
        val parsed = runCatching { parseCachedQuotes(raw) }.getOrElse { emptyList() }
        if (parsed.isEmpty()) return
        quotes = parsed
        loadedAtMs = at
        nextIndex = prefs.getInt(KEY_INDEX, 0).coerceIn(0, parsed.lastIndex.coerceAtLeast(0))
    }

    companion object {
        const val SOURCE_ID = "zenquotes"
        const val API_URL = "https://zenquotes.io/api/quotes"
        /** Refresh the batch every 4 hours (ZenQuotes recommends looping locally for hours). */
        const val TTL_MS = 4L * 60L * 60L * 1000L
        private const val PREFS = "arthur_quotes_cache"
        private const val KEY_CACHE_JSON = "cache_json"
        private const val KEY_CACHE_AT = "cache_at"
        private const val KEY_INDEX = "next_index"

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
