package fr.geoking.arthur.source

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class StillImageDownloaderTest {

    @Test
    fun downloadBytes_sendsOkHttpUserAgent() {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(200).body("image-data").build())
        server.start()

        val url = server.url("/test.jpg").toString()
        val bytes = StillImageDownloader.downloadBytes(url)

        assertEquals("image-data", String(bytes))

        val request = server.takeRequest()
        assertEquals("okhttp/4.12.0", request.headers["User-Agent"])

        server.close()
    }
}
