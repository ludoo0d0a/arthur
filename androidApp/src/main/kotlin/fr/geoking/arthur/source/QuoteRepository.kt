package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.db.QuoteDao
import fr.geoking.arthur.db.QuoteEntity
import kotlinx.coroutines.runBlocking
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
 * Fetches quotes from the selected [QuoteProvider], caches them (TTL), and assigns
 * one quote per artwork when an Ambient pool loads. Backed by Room 3 [QuoteDao] with coroutines.
 */
class QuoteRepository(
    context: Context,
    private val httpGet: suspend (url: String) -> String,
    private val provider: () -> QuoteProvider,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val quoteDao: QuoteDao? = null,
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

    /**
     * Stable quote map for a rotation pool: one entry per artwork id.
     * Prefers a distinct quote per id when the cache is large enough; grows the
     * Lecog cache when the pool needs more unique quotes than currently stored.
     * Call when the list loads (not on each slide advance).
     */
    suspend fun quotesForArtworks(artworkIds: List<String>): Map<String, Quote> = mutex.withLock {
        if (artworkIds.isEmpty()) return emptyMap()
        val distinctIds = artworkIds.distinct()
        val minSize = distinctIds.size.coerceAtLeast(1)
        ensureLoadedLocked(minSize = minSize)
        if (quotes.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, Quote>(distinctIds.size)
        val usedTexts = HashSet<String>(distinctIds.size)
        for (id in distinctIds) {
            result[id] = takeNextPreferringUnusedLocked(usedTexts)
        }
        prefs.edit().putInt(KEY_INDEX, nextIndex).apply()
        result
    }

    /** Next quote from the cache, refreshing the batch when expired, empty, or provider changed. */
    suspend fun nextQuote(): Quote? = mutex.withLock {
        ensureLoadedLocked(minSize = 1)
        if (quotes.isEmpty()) return null
        val quote = takeNextLocked()
        prefs.edit().putInt(KEY_INDEX, nextIndex).apply()
        quote
    }

    private fun takeNextLocked(): Quote {
        val quote = quotes[nextIndex % quotes.size]
        nextIndex = (nextIndex + 1) % quotes.size
        return quote
    }

    /**
     * Advances the cursor, skipping quotes already used in this assignment when
     * alternatives remain. Falls back to the next quote when the batch is exhausted.
     */
    private fun takeNextPreferringUnusedLocked(usedTexts: MutableSet<String>): Quote {
        if (quotes.isEmpty()) error("quotes empty")
        val size = quotes.size
        if (usedTexts.size >= size) {
            val quote = takeNextLocked()
            usedTexts.add(quote.text)
            return quote
        }
        repeat(size) {
            val quote = takeNextLocked()
            if (usedTexts.add(quote.text)) return quote
        }
        val quote = takeNextLocked()
        usedTexts.add(quote.text)
        return quote
    }

    private suspend fun ensureLoadedLocked(minSize: Int) {
        val wanted = provider()
        val now = clock()
        val providerOk = cachedProviderId == wanted.id
        val ttlOk = now - loadedAtMs < TTL_MS
        val targetSize = minSize.coerceAtMost(MAX_CACHED_QUOTES)

        if (quotes.size >= targetSize && providerOk && ttlOk) {
            return
        }

        val needsFullRefresh = !providerOk || !ttlOk || quotes.isEmpty()
        if (needsFullRefresh) {
            val fetched = runCatching { fetchBatch(wanted, round = 0) }.getOrElse { emptyList() }
            if (fetched.isEmpty()) {
                if (quotes.isEmpty() || !providerOk) return
                // keep stale; may still grow below
            } else {
                quotes = fetched.distinctBy { it.text }
                loadedAtMs = now
                nextIndex = 0
                cachedProviderId = wanted.id
                persistCacheLocked(wanted.id, now)
            }
        }

        // Grow (Lecog) when the pool needs more unique quotes than one batch provides.
        var rounds = 0
        while (quotes.size < targetSize && rounds < GROW_MAX_ROUNDS) {
            rounds++
            val fetched = runCatching { fetchBatch(wanted, round = rounds) }.getOrElse { emptyList() }
            if (fetched.isEmpty()) break
            val existing = quotes.mapTo(HashSet()) { it.text }
            val added = fetched.filter { existing.add(it.text) }
            if (added.isEmpty()) break
            quotes = quotes + added
            loadedAtMs = now
            cachedProviderId = wanted.id
            persistCacheLocked(wanted.id, now)
        }
    }

    private suspend fun persistCacheLocked(providerId: String, now: Long) {
        prefs.edit()
            .putString(KEY_CACHE_JSON, encodeQuotes(quotes))
            .putLong(KEY_CACHE_AT, loadedAtMs)
            .putInt(KEY_INDEX, nextIndex)
            .putString(KEY_CACHE_PROVIDER, providerId)
            .apply()

        if (quoteDao != null) {
            quoteDao.deleteForProvider(providerId)
            quoteDao.insertAll(
                quotes.mapIndexed { idx, q ->
                    QuoteEntity(
                        id = "${providerId}_$idx",
                        text = q.text,
                        author = q.author,
                        providerId = providerId,
                        cachedAtMs = now,
                    )
                },
            )
        }
    }

    private suspend fun fetchBatch(provider: QuoteProvider, round: Int): List<Quote> = when (provider) {
        QuoteProvider.ZenQuotes -> {
            val body = httpGet(ZENQUOTES_API_URL)
            parseZenQuotes(body)
        }
        QuoteProvider.CitationLecog -> fetchCitationLecogBatch(round)
    }

    private suspend fun fetchCitationLecogBatch(round: Int): List<Quote> {
        val out = ArrayList<Quote>(LECOG_BATCH_SIZE)
        val seen = HashSet<String>()
        val stamp = clock() + round
        repeat(LECOG_BATCH_SIZE) { index ->
            val url = lecogRandomQuoteUrl(round * LECOG_BATCH_SIZE + index, stamp)
            val body = runCatching { httpGet(url) }.getOrNull() ?: return@repeat
            val quote = parseCitationLecog(body) ?: return@repeat
            if (seen.add(quote.text)) out.add(quote)
        }
        return out
    }

    private fun restoreFromPrefs() {
        val wantedProviderId = provider().id
        if (quoteDao != null) {
            runBlocking {
                val dbEntities = quoteDao.getQuotesForProvider(wantedProviderId)
                if (dbEntities.isNotEmpty()) {
                    quotes = dbEntities.map { Quote(text = it.text, author = it.author) }
                    loadedAtMs = dbEntities.firstOrNull()?.cachedAtMs ?: 0L
                    nextIndex = prefs.getInt(KEY_INDEX, 0).coerceIn(0, quotes.lastIndex.coerceAtLeast(0))
                    cachedProviderId = wantedProviderId
                    return@runBlocking
                }
            }
        }

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
        const val LECOG_BATCH_SIZE = 15
        /** Cap unique quotes kept in memory / prefs (covers Ambient auto-rotation pools). */
        const val MAX_CACHED_QUOTES = 30
        /** Extra Lecog fetch rounds when growing past one batch. */
        const val GROW_MAX_ROUNDS = 2
        const val TTL_MS = 4L * 60L * 60L * 1000L

        internal fun lecogRandomQuoteUrl(index: Int, timestampMs: Long): String =
            "$LECOG_API_URL?n=$index&t=$timestampMs"

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
                val text = sanitizeQuoteText(obj["q"]?.jsonPrimitive?.contentOrNull.orEmpty())
                if (text.isEmpty()) null
                else Quote(
                    text = text,
                    author = sanitizeQuoteText(obj["a"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                )
            }

        internal fun parseCitationLecog(body: String): Quote? {
            val root = json.parseToJsonElement(body).jsonObject
            if (root["success"]?.jsonPrimitive?.contentOrNull == "false") return null
            val data = root["data"]?.jsonObject ?: return null
            val text = sanitizeQuoteText(data["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
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
                val text = sanitizeQuoteText(obj["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
                if (text.isEmpty()) null
                else Quote(
                    text = text,
                    author = sanitizeQuoteText(obj["author"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                )
            }

        internal fun sanitizeQuoteText(raw: String): String =
            raw
                .replace(BR_TAG_REGEX, " ")
                .replace(WHITESPACE_REGEX, " ")
                .trim()

        private val BR_TAG_REGEX = Regex("""(?i)<br\s*/?>""")
        private val WHITESPACE_REGEX = Regex("""\s+""")

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
