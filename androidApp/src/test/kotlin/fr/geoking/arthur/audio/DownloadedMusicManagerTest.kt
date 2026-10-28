package fr.geoking.arthur.audio

import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.remote.DownloadedMusicManager
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadedMusicManagerTest {

    @Test
    fun getDownloadedTracksReturnsEmptyOnNewStore() {
        val manager = DownloadedMusicManager(ApplicationProvider.getApplicationContext())
        val tracks = manager.getDownloadedTracks()
        assertEquals(0, tracks.size)
        assertFalse(manager.isDownloaded("unknown-id"))
        assertNull(manager.getTrackFile("unknown-id"))
    }

    @Test
    fun deleteTrackRemovesTrackFromManifest() {
        val manager = DownloadedMusicManager(ApplicationProvider.getApplicationContext())
        manager.deleteTrack("non-existent-id")
        assertEquals(0, manager.getDownloadedTracks().size)
    }

    @Test
    fun downloadTrackCompletesAndRegistersManifest() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val payload = ByteArray(4096) { it.toByte() }
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body(okio.Buffer().write(payload))
                    .addHeader("Content-Length", payload.size.toString())
                    .build(),
            )

            val manager = DownloadedMusicManager(ApplicationProvider.getApplicationContext())
            val track = RemoteAudioTrack(
                id = "resume-test-track",
                title = "Calm Test",
                artist = "Tester",
                streamUrl = server.url("/track.mp3").toString(),
                durationSeconds = 30,
                sourceId = "test",
            )

            val result = manager.downloadTrack(track)
            assertTrue(result.isSuccess)
            assertTrue(manager.isDownloaded(track.id))
            assertEquals(payload.size.toLong(), manager.getTrackFile(track.id)!!.length())
            assertEquals(1, manager.getDownloadedTracks().size)
        } finally {
            server.close()
        }
    }

    @Test
    fun downloadTrackAppendsPartialWithHttp206() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val fullBody = ByteArray(12_000) { (it % 200).toByte() }
            val firstChunk = fullBody.copyOfRange(0, 5_000)
            val secondChunk = fullBody.copyOfRange(5_000, fullBody.size)

            val manager = DownloadedMusicManager(ApplicationProvider.getApplicationContext())
            val trackId = "partial-resume-track"
            val partial = manager.partialFileFor(trackId)
            partial.parentFile?.mkdirs()
            partial.writeBytes(firstChunk)

            server.enqueue(
                MockResponse.Builder()
                    .code(206)
                    .body(okio.Buffer().write(secondChunk))
                    .headers(
                        Headers.headersOf(
                            "Content-Length",
                            secondChunk.size.toString(),
                            "Content-Range",
                            "bytes ${firstChunk.size}-${fullBody.size - 1}/${fullBody.size}",
                        ),
                    )
                    .build(),
            )

            val track = RemoteAudioTrack(
                id = trackId,
                title = "Partial Resume",
                artist = "Tester",
                streamUrl = server.url("/partial.mp3").toString(),
                sourceId = "test",
            )

            val result = manager.downloadTrack(track)
            assertTrue(result.isSuccess)

            val recorded = server.takeRequest(5, TimeUnit.SECONDS)
            assertEquals("bytes=${firstChunk.size}-", recorded?.headers?.get("Range"))
            assertEquals(fullBody.size.toLong(), manager.getTrackFile(trackId)!!.length())
            assertFalse(partial.exists())
        } finally {
            server.close()
        }
    }
}
