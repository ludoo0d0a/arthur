package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QuoteSettingsTest {
    @Test
    fun showQuotes_defaultsTrue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = QuoteSettings(context)
        assertTrue(settings.showQuotes.value)
    }

    @Test
    fun showQuotes_persists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = QuoteSettings(context)
        settings.setShowQuotes(false)
        assertEquals(false, settings.showQuotes.value)
        assertEquals(false, QuoteSettings(context).showQuotes.value)
    }

    @Test
    fun provider_defaultsEnglishToZenQuotes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = QuoteSettings(context)
        assertEquals(QuoteProvider.ZenQuotes, settings.provider.value)
    }

    @Test
    fun provider_persists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = QuoteSettings(context)
        settings.setProvider(QuoteProvider.CitationLecog)
        assertEquals(QuoteProvider.CitationLecog, settings.provider.value)
        assertEquals(QuoteProvider.CitationLecog, QuoteSettings(context).provider.value)
    }

    @Test
    fun defaultForLanguage_frenchUsesLecog() {
        assertEquals(QuoteProvider.CitationLecog, QuoteProvider.defaultForLanguage("fr"))
        assertEquals(QuoteProvider.CitationLecog, QuoteProvider.defaultForLanguage("fr-FR"))
        assertEquals(QuoteProvider.ZenQuotes, QuoteProvider.defaultForLanguage("en"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QuoteRepositoryTest {
    @Test
    fun nextQuote_parsesBatchAndCycles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        var fetches = 0
        val repo = QuoteRepository(
            context = context,
            httpGet = {
                fetches++
                """[{"q":"First","a":"A"},{"q":"Second","a":"B"}]"""
            },
            provider = { QuoteProvider.ZenQuotes },
            clock = { 1_000L },
        )
        assertEquals(Quote("First", "A"), repo.nextQuote())
        assertEquals(Quote("Second", "B"), repo.nextQuote())
        assertEquals(Quote("First", "A"), repo.nextQuote())
        assertEquals(1, fetches)
    }

    @Test
    fun quotesForArtworks_assignsOnePerId() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        val repo = QuoteRepository(
            context = context,
            httpGet = { """[{"q":"First","a":"A"},{"q":"Second","a":"B"}]""" },
            provider = { QuoteProvider.ZenQuotes },
            clock = { 1_000L },
        )
        val assigned = repo.quotesForArtworks(listOf("img-1", "img-2", "img-3"))
        assertEquals(Quote("First", "A"), assigned["img-1"])
        assertEquals(Quote("Second", "B"), assigned["img-2"])
        assertEquals(Quote("First", "A"), assigned["img-3"])
        // Same pool lookup stays stable — nextQuote continues after the assignment cursor.
        assertEquals(Quote("Second", "B"), repo.nextQuote())
    }

    @Test
    fun nextQuote_usesCacheWithinTtl() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        var now = 1_000L
        var fetches = 0
        val repo = QuoteRepository(
            context = context,
            httpGet = {
                fetches++
                """[{"q":"Cached","a":"C"}]"""
            },
            provider = { QuoteProvider.ZenQuotes },
            clock = { now },
        )
        assertEquals(Quote("Cached", "C"), repo.nextQuote())
        now += QuoteRepository.TTL_MS - 1
        assertEquals(Quote("Cached", "C"), repo.nextQuote())
        assertEquals(1, fetches)
    }

    @Test
    fun nextQuote_returnsNullWhenFetchFailsAndCacheEmpty() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        val repo = QuoteRepository(
            context = context,
            httpGet = { error("network") },
            provider = { QuoteProvider.ZenQuotes },
        )
        assertNull(repo.nextQuote())
    }

    @Test
    fun nextQuote_citationLecog_parsesAndBatches() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        var fetches = 0
        val repo = QuoteRepository(
            context = context,
            httpGet = {
                fetches++
                """{"success":true,"data":{"text":"Je pense, donc je suis.","author":{"forename":"René","name":"Descartes"}}}"""
            },
            provider = { QuoteProvider.CitationLecog },
            clock = { 1_000L },
        )
        assertEquals(Quote("Je pense, donc je suis.", "René Descartes"), repo.nextQuote())
        // Deduped identical responses → single quote; still fetched LECOG_BATCH_SIZE times.
        assertEquals(QuoteRepository.LECOG_BATCH_SIZE, fetches)
        assertEquals(Quote("Je pense, donc je suis.", "René Descartes"), repo.nextQuote())
        assertEquals(QuoteRepository.LECOG_BATCH_SIZE, fetches)
    }

    @Test
    fun citationLecog_usesCacheBustedUrlsPerAttempt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        val urls = mutableListOf<String>()
        val repo = QuoteRepository(
            context = context,
            httpGet = { url ->
                urls += url
                val n = url.substringAfter("n=").substringBefore("&").toInt()
                """{"success":true,"data":{"text":"Quote $n","author":{"forename":"","name":"A$n"}}}"""
            },
            provider = { QuoteProvider.CitationLecog },
            clock = { 42_000L },
        )
        val assigned = repo.quotesForArtworks(listOf("a", "b", "c"))
        assertEquals(QuoteRepository.LECOG_BATCH_SIZE, urls.size)
        assertEquals(urls.toSet().size, urls.size)
        assertTrue(urls.all { it.startsWith(QuoteRepository.LECOG_API_URL) && "t=42000" in it })
        assertEquals(Quote("Quote 0", "A0"), assigned["a"])
        assertEquals(Quote("Quote 1", "A1"), assigned["b"])
        assertEquals(Quote("Quote 2", "A2"), assigned["c"])
    }

    @Test
    fun quotesForArtworks_refetchesWhenCachedBatchTooSmall() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        var now = 1_000L
        var fetches = 0
        var diverse = false
        val repo = QuoteRepository(
            context = context,
            httpGet = { url ->
                fetches++
                if (!diverse) {
                    """{"success":true,"data":{"text":"Same","author":{"forename":"","name":"A"}}}"""
                } else {
                    val n = url.substringAfter("n=").substringBefore("&").toInt()
                    """{"success":true,"data":{"text":"Lecog $n","author":{"forename":"","name":"L$n"}}}"""
                }
            },
            provider = { QuoteProvider.CitationLecog },
            clock = { now },
        )
        // Poison cache with a single-quote batch (HTTP cache collapse scenario).
        assertEquals(Quote("Same", "A"), repo.nextQuote())
        val afterPoison = fetches
        diverse = true
        now += 1
        val assigned = repo.quotesForArtworks(listOf("img-1", "img-2"))
        assertTrue(fetches > afterPoison)
        assertEquals(Quote("Lecog 0", "L0"), assigned["img-1"])
        assertEquals(Quote("Lecog 1", "L1"), assigned["img-2"])
    }

    @Test
    fun nextQuote_refetchesWhenProviderChanges() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_quotes_cache", Context.MODE_PRIVATE).edit().clear().commit()
        var provider = QuoteProvider.ZenQuotes
        var fetches = 0
        val repo = QuoteRepository(
            context = context,
            httpGet = { url ->
                fetches++
                if (url.contains("zenquotes")) {
                    """[{"q":"EN","a":"A"}]"""
                } else {
                    """{"success":true,"data":{"text":"FR","author":{"forename":"","name":"B"}}}"""
                }
            },
            provider = { provider },
            clock = { 1_000L },
        )
        assertEquals(Quote("EN", "A"), repo.nextQuote())
        val afterZen = fetches
        provider = QuoteProvider.CitationLecog
        assertEquals(Quote("FR", "B"), repo.nextQuote())
        assertTrue(fetches > afterZen)
    }

    @Test
    fun parseCitationLecog_readsAuthorParts() {
        val quote = QuoteRepository.parseCitationLecog(
            """{"success":true,"data":{"text":"Hello","author":{"forename":"René","name":"Descartes"}}}""",
        )
        assertEquals(Quote("Hello", "René Descartes"), quote)
    }

    @Test
    fun sanitizeQuoteText_stripsBrTags() {
        assertEquals(
            "Line one Line two",
            QuoteRepository.sanitizeQuoteText("Line one<br>Line two"),
        )
        assertEquals(
            "A B C",
            QuoteRepository.sanitizeQuoteText("A<br/>B<br />C"),
        )
        assertEquals(
            "Hello world",
            QuoteRepository.sanitizeQuoteText("Hello<br>world"),
        )
    }

    @Test
    fun parseCitationLecog_stripsBrInText() {
        val quote = QuoteRepository.parseCitationLecog(
            """{"success":true,"data":{"text":"Je pense,<br>donc je suis.","author":{"forename":"René","name":"Descartes"}}}""",
        )
        assertEquals(Quote("Je pense, donc je suis.", "René Descartes"), quote)
    }
}
