package fr.geoking.arthur.source

import java.io.File
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheBypassInterceptorTest {

    @Test
    fun forcesNetworkFetchWhenCacheDisabled() {
        val server = MockWebServer()
        server.start()
        val cacheDir = File.createTempFile("http_cache_bypass", "").apply { delete(); mkdirs() }
        val cache = Cache(cacheDir, 1024 * 1024)
        val controller = HttpCacheController(cache)
        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(CacheBypassInterceptor(controller))
            .build()

        try {
            server.enqueue(
                MockResponse.Builder().code(200).addHeader("Cache-Control", "public, max-age=60").body("first").build(),
            )
            server.enqueue(
                MockResponse.Builder().code(200).addHeader("Cache-Control", "public, max-age=60").body("second").build(),
            )

            val url = server.url("/data")
            client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }

            controller.setDisabled(true)
            val second = client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }

            // Without the bypass, the second call would be served from the disk cache and
            // MockWebServer would only ever see one request.
            assertEquals("second", second)
            assertEquals(2, server.requestCount)
        } finally {
            cache.delete()
            server.close()
        }
    }
}
