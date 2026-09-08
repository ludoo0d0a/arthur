package fr.geoking.arthur.source

import fr.geoking.arthur.shared.debug.DebugLogger
import java.io.File
import java.io.IOException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DebugInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var debugLogger: DebugLogger
    private lateinit var client: OkHttpClient
    private var serverClosed = false

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        debugLogger = DebugLogger()
        client = OkHttpClient.Builder()
            .addInterceptor(DebugInterceptor(debugLogger))
            .build()
    }

    @After
    fun tearDown() {
        if (!serverClosed) server.close()
    }

    @Test
    fun capturesHeadersHostAndJsonBody() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Test-Header", "abc")
                .body("""{"name":"mona lisa","tags":["art","famous"]}""")
                .build(),
        )

        val request = Request.Builder()
            .url(server.url("/objects/1"))
            .header("X-Source-Id", "met")
            .build()
        client.newCall(request).execute().use { it.body?.string() }

        val log = debugLogger.stats.value.recentQueries.single()
        assertEquals("met", log.sourceId)
        assertEquals(server.url("/objects/1").host, log.host)
        assertEquals(200, log.statusCode)
        assertFalse(log.isCached)
        assertTrue(log.responseHeaders["X-Test-Header"]?.contains("abc") == true)
        assertTrue(log.responseBody!!.contains("mona lisa"))
        assertFalse(log.responseBodyTruncated)
    }

    @Test
    fun skipsBodyCaptureForBinaryContentType() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "image/jpeg")
                .body("binary-ish-bytes")
                .build(),
        )

        client.newCall(Request.Builder().url(server.url("/img.jpg")).build()).execute().use { it.body?.string() }

        val log = debugLogger.stats.value.recentQueries.single()
        assertNull(log.responseBody)
    }

    @Test
    fun recordsErrorMessageWhenConnectionFails() {
        val unreachableUrl = server.url("/unreachable")
        server.close()
        serverClosed = true

        try {
            client.newCall(Request.Builder().url(unreachableUrl).build()).execute()
            fail("expected IOException")
        } catch (_: IOException) {
            // expected: connection refused after the server was shut down
        }

        val log = debugLogger.stats.value.recentQueries.single()
        assertNull(log.statusCode)
        assertTrue(log.errorMessage != null)
    }

    @Test
    fun detectsCacheHitOnSecondRequestServedFromDisk() {
        val cacheDir = File.createTempFile("http_cache_test", "").apply { delete(); mkdirs() }
        val cache = Cache(cacheDir, 1024 * 1024)
        val cachingClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(DebugInterceptor(debugLogger))
            .build()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Cache-Control", "public, max-age=60")
                    .body("""{"ok":true}""")
                    .build(),
            )

            val url = server.url("/cacheable")
            // Only one response is enqueued: the second call must be served from the disk
            // cache without hitting the network, or MockWebServer would fail with no response.
            cachingClient.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }
            cachingClient.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }

            val logs = debugLogger.stats.value.recentQueries
            assertEquals(2, logs.size)
            assertEquals(1, logs.count { it.isCached })
            assertEquals(1, logs.count { !it.isCached })
        } finally {
            cache.delete()
        }
    }
}
