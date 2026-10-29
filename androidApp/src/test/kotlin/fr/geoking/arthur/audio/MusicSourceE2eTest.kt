package fr.geoking.arthur.audio

import fr.geoking.arthur.BuildConfig
import fr.geoking.arthur.shared.audio.remote.ArchiveOrgMusicSource
import fr.geoking.arthur.shared.audio.remote.IncompetechMusicSource
import fr.geoking.arthur.shared.audio.remote.JamendoMusicSource
import fr.geoking.arthur.shared.audio.remote.PixabayMusicSource
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Live e2e against royalty-free music providers: search, resolve stream URL, download a short
 * sample under `androidApp/build/e2e-samples/music/`.
 *
 * Opt-in so default CI does not hit rate limits:
 * `./gradlew :androidApp:testFullDebugUnitTest -Pe2eMusicSources=true --tests '*MusicSourceE2eTest'`
 */
class MusicSourceE2eTest {

    private lateinit var client: HttpClient
    private lateinit var samplesDir: File

    @Before
    fun setUp() {
        assumeTrue(
            "Pass -Pe2eMusicSources=true (or E2E_MUSIC_SOURCES=1) to run live music e2e",
            e2eEnabled(),
        )
        client = HttpClient(OkHttp)
        samplesDir = File("build/e2e-samples/music").also { it.mkdirs() }
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.close()
    }

    @Test
    fun jamendo_searchesAndDownloadsSample() = runBlocking {
        val clientId = BuildConfig.JAMENDO_CLIENT_ID
        assumeTrue("JAMENDO_CLIENT_ID missing", clientId.isNotBlank())

        val source = JamendoMusicSource(
            httpGet = { url -> musicHttpGet(url) },
            clientId = clientId,
        )
        val tracks = source.fetchZenTracks("ambient", limit = 3).getOrThrow()
        assertTrue("Jamendo catalog empty", tracks.isNotEmpty())
        downloadSample(tracks.first(), "jamendo")
    }

    @Test
    fun archiveOrg_searchesOpenCollectionsAndDownloadsSample() = runBlocking {
        val source = ArchiveOrgMusicSource(httpGet = { url -> musicHttpGet(url) })
        val tracks = source.fetchZenTracks("ambient meditation", limit = 5).getOrThrow()
        assertTrue("Archive.org open-collection catalog empty", tracks.isNotEmpty())
        val track = tracks.first { it.streamUrl.endsWith(".mp3", ignoreCase = true) }
        assertTrue(
            "Archive.org track missing file size from metadata: ${track.id}",
            (track.fileSizeBytes ?: 0L) > 0L,
        )
        downloadSample(track, "archive_org")
    }

    @Test
    fun incompetech_curatedTracksDownload() = runBlocking {
        val source = IncompetechMusicSource()
        val tracks = source.fetchZenTracks(limit = 1).getOrThrow()
        assertTrue(tracks.isNotEmpty())
        downloadSample(tracks.first(), "incompetech")
    }

    @Test
    fun pixabayMusic_reportsClearFailureWhenApiDenied() = runBlocking {
        val apiKey = BuildConfig.PIXABAY_API_KEY
        assumeTrue("PIXABAY_API_KEY missing", apiKey.isNotBlank())

        val source = PixabayMusicSource(
            httpGet = { url -> musicHttpGet(url) },
            apiKey = apiKey,
        )
        val result = source.fetchZenTracks("calm", limit = 1)
        // Public /api/audio/ is denied for standard keys — assert we fail clearly, not with JSON token noise.
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message.orEmpty()
            assertTrue(
                "Expected HTTP/access error (not kotlinx JSON token), got: $msg",
                msg.contains("403") ||
                    msg.contains("Access denied", ignoreCase = true) ||
                    msg.contains("expected JSON") ||
                    msg.contains("HTTP"),
            )
            assertTrue(
                "Must not surface kotlinx unexpected token for Pixabay",
                !msg.contains("Unexpected JSON token", ignoreCase = true),
            )
        } else {
            // If Pixabay ever opens the audio API, treat success as a pass.
            assertTrue(result.getOrThrow().isNotEmpty())
        }
    }

    private suspend fun musicHttpGet(url: String): String {
        val response = client.get(url) {
            header("User-Agent", "Arthur-Ambient/1.0 (fr.geoking.arthur; e2e)")
            header("X-Source-Id", "music_e2e")
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}: ${body.take(160)}")
        }
        return body
    }

    private suspend fun downloadSample(track: RemoteAudioTrack, prefix: String) {
        assertTrue("$prefix streamUrl blank for ${track.id}", track.streamUrl.isNotBlank())
        val bytes = client.get(track.streamUrl) {
            header("User-Agent", "Arthur-Ambient/1.0 (fr.geoking.arthur; e2e)")
            header("Range", "bytes=0-${MIN_AUDIO_BYTES + 1023}")
        }.bodyAsBytes()
        assertTrue(
            "$prefix ${track.id}: download too small (${bytes.size})",
            bytes.size >= MIN_AUDIO_BYTES,
        )
        val safeName = track.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(samplesDir, "$safeName.mp3").also { it.writeBytes(bytes) }
        println(
            "$prefix sample → ${out.absolutePath} (${bytes.size} bytes" +
                track.fileSizeBytes?.let { ", remoteSize=$it" }.orEmpty() +
                ") «${track.title}»",
        )
    }

    companion object {
        private const val MIN_AUDIO_BYTES = 2_000

        private fun e2eEnabled(): Boolean {
            val prop = System.getProperty("e2e.musicSources").orEmpty()
            if (prop.equals("true", ignoreCase = true) || prop == "1") return true
            val env = System.getenv("E2E_MUSIC_SOURCES").orEmpty()
            return env.equals("true", ignoreCase = true) || env == "1"
        }
    }
}
