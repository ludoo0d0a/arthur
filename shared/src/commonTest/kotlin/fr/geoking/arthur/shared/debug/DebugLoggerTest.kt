package fr.geoking.arthur.shared.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugLoggerTest {

    @Test
    fun testRecordQueriesAndStats() {
        var mockTime = 1000L
        val logger = DebugLogger(clock = { mockTime })

        assertEquals(0, logger.stats.value.totalQueries)
        assertEquals(0, logger.stats.value.activeQueries)

        logger.recordQueryStart()
        assertEquals(1, logger.stats.value.activeQueries)

        logger.recordQueryEnd(
            sourceId = "met",
            url = "https://collectionapi.metmuseum.org/public/collection/v1/search?q=painting",
            durationMs = 120L,
            isCached = false,
            statusCode = 200,
        )

        val stats = logger.stats.value
        assertEquals(1, stats.totalQueries)
        assertEquals(0, stats.activeQueries)
        assertEquals(0, stats.cacheHits)
        assertEquals(1, stats.cacheMisses)
        assertEquals(1, stats.recentQueries.size)

        val item = stats.recentQueries.first()
        assertEquals("met", item.sourceId)
        assertEquals(120L, item.durationMs)
        assertEquals(false, item.isCached)
        assertEquals(200, item.statusCode)
    }

    @Test
    fun testCacheHitTracking() {
        val logger = DebugLogger()

        logger.recordQueryStart()
        logger.recordQueryEnd("met", "https://example.com/1", 10L, isCached = false, statusCode = 200)

        logger.recordQueryStart()
        logger.recordQueryEnd("met", "https://example.com/1", 2L, isCached = true, statusCode = 200)

        val stats = logger.stats.value
        assertEquals(2, stats.totalQueries)
        assertEquals(1, stats.cacheHits)
        assertEquals(1, stats.cacheMisses)
    }

    @Test
    fun testRecordLoadDuration() {
        val logger = DebugLogger()
        logger.recordLoadDuration(450L)
        assertEquals(450L, logger.stats.value.lastLoadDurationMs)
    }

    @Test
    fun testClear() {
        val logger = DebugLogger()
        logger.recordQueryStart()
        logger.recordQueryEnd("met", "https://example.com", 15L, false, 200)
        logger.recordLoadDuration(200L)

        assertTrue(logger.stats.value.totalQueries > 0)
        logger.clear()

        assertEquals(0, logger.stats.value.totalQueries)
        assertEquals(0L, logger.stats.value.lastLoadDurationMs)
        assertTrue(logger.stats.value.recentQueries.isEmpty())
    }
}
