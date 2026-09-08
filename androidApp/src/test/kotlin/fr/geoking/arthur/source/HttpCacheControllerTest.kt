package fr.geoking.arthur.source

import java.io.File
import okhttp3.Cache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpCacheControllerTest {

    private fun newCache(): Cache {
        val dir = File.createTempFile("http_cache_ctrl", "").apply { delete(); mkdirs() }
        return Cache(dir, 1024 * 1024)
    }

    @Test
    fun disabledDefaultsToFalseAndToggles() {
        val cache = newCache()
        val controller = HttpCacheController(cache)
        try {
            assertFalse(controller.disabled.value)
            controller.setDisabled(true)
            assertTrue(controller.disabled.value)
            controller.setDisabled(false)
            assertFalse(controller.disabled.value)
        } finally {
            cache.delete()
        }
    }

    @Test
    fun clearDoesNotThrowOnEmptyCache() {
        val cache = newCache()
        val controller = HttpCacheController(cache)
        try {
            controller.clear()
            assertEquals(0L, controller.stats().sizeBytes)
        } finally {
            cache.delete()
        }
    }
}
