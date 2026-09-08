package fr.geoking.arthur.ui.components

import fr.geoking.arthur.shared.debug.DebugLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugLoggerTest {

    @Test
    fun debugLogger_recordsCacheHitsAndMisses() {
        val logger = DebugLogger()
        logger.recordQueryEnd(
            sourceId = "met",
            url = "https://api.metmuseum.org/objects/1",
            durationMs = 120L,
            isCached = true,
            statusCode = 200,
        )
        logger.recordQueryEnd(
            sourceId = "met",
            url = "https://api.metmuseum.org/objects/2",
            durationMs = 350L,
            isCached = false,
            statusCode = 200,
        )

        val stats = logger.stats.value
        assertEquals(2, stats.totalQueries)
        assertEquals(1, stats.cacheHits)
        assertEquals(1, stats.cacheMisses)
        assertEquals(2, stats.recentQueries.size)

        val misses = stats.recentQueries.filter { !it.isCached }
        assertEquals(1, misses.size)
        assertFalse(misses.first().isCached)
        assertEquals("https://api.metmuseum.org/objects/2", misses.first().url)

        val hits = stats.recentQueries.filter { it.isCached }
        assertEquals(1, hits.size)
        assertTrue(hits.first().isCached)
    }

    @Test
    fun debugLogger_recordsLoadDuration() {
        val logger = DebugLogger()
        logger.recordLoadDuration(450L)

        val stats = logger.stats.value
        assertEquals(450L, stats.lastLoadDurationMs)
    }

    @Test
    fun debugLogger_recordsRichNetworkLogDetails() {
        val logger = DebugLogger()
        logger.recordQueryEnd(
            sourceId = "met",
            url = "https://api.metmuseum.org/objects/1?q=mona",
            durationMs = 120L,
            isCached = false,
            statusCode = 200,
            host = "api.metmuseum.org",
            requestHeaders = mapOf("Accept" to listOf("application/json")),
            responseHeaders = mapOf("Content-Type" to listOf("application/json")),
            responseBody = """{"title":"Mona Lisa"}""",
        )

        val item = logger.stats.value.recentQueries.single()
        assertEquals("api.metmuseum.org", item.host)
        assertEquals(listOf("application/json"), item.requestHeaders["Accept"])
        assertEquals(listOf("application/json"), item.responseHeaders["Content-Type"])
        assertTrue(item.responseBody!!.contains("Mona Lisa"))
        assertFalse(item.responseBodyTruncated)
    }
}
