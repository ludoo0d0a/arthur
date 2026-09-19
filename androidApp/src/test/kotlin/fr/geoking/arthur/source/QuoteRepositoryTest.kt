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
            clock = { 1_000L },
        )
        assertEquals(Quote("First", "A"), repo.nextQuote())
        assertEquals(Quote("Second", "B"), repo.nextQuote())
        assertEquals(Quote("First", "A"), repo.nextQuote())
        assertEquals(1, fetches)
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
        )
        assertNull(repo.nextQuote())
    }
}
