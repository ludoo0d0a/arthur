package fr.geoking.arthur.source

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun downloadToFile_streamsToFileCorrectly() {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(200).body("sample-file-content").build())
        server.start()

        val targetFile = File.createTempFile("test_file_", ".img")
        try {
            val url = server.url("/photo.jpg").toString()
            val result = StillImageDownloader.downloadToFile(url, targetFile)

            assertEquals(targetFile.absolutePath, result.absolutePath)
            assertTrue(targetFile.exists())
            assertEquals("sample-file-content", targetFile.readText())
        } finally {
            targetFile.delete()
            server.close()
        }
    }

    @Test
    fun downloadToFile_throwsIOExceptionWithHttpStatusCode_whenHttpError() {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(404).build())
        server.start()

        val targetFile = File.createTempFile("test_file_404_", ".img")
        try {
            val url = server.url("/missing.jpg").toString()
            try {
                StillImageDownloader.downloadToFile(url, targetFile)
                org.junit.Assert.fail("Expected IOException")
            } catch (e: java.io.IOException) {
                assertTrue(e.message?.contains("404") == true)
                assertTrue(e.message?.contains("HTTP 404") == true)
            }
        } finally {
            targetFile.delete()
            server.close()
        }
    }

    @Test
    fun downloadInChunks_handlesRangeRequests() {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(206).body("chunk-part-1").build())
        server.start()

        val targetFile = File.createTempFile("test_chunk_", ".img")
        try {
            val url = server.url("/chunked.jpg").toString()
            val success = StillImageDownloader.downloadInChunks(
                url = url,
                targetFile = targetFile,
                totalLength = 12L,
            )

            assertTrue(success)
            assertTrue(targetFile.exists())
            assertEquals("chunk-part-1", targetFile.readText())

            val request = server.takeRequest()
            assertTrue(request.headers["Range"]?.startsWith("bytes=0-") == true)
        } finally {
            targetFile.delete()
            server.close()
        }
    }
}
