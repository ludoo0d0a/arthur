package fr.geoking.arthur.audio.remote

import android.content.Context
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.context.GlobalContext

@Serializable
data class DownloadedMusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val sourceId: String,
    val fileName: String,
    val durationSeconds: Int = 0,
    val license: String = "Royalty-Free",
    val downloadedAt: Long = System.currentTimeMillis(),
)

class DownloadedMusicManager(
    private val context: Context,
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    private val musicDir = File(context.filesDir, "downloaded_music").apply { mkdirs() }
    private val manifestFile = File(musicDir, "manifest.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Synchronized
    fun getDownloadedTracks(): List<DownloadedMusicTrack> {
        if (!manifestFile.exists()) return emptyList()
        return runCatching {
            val body = manifestFile.readText()
            json.decodeFromString<List<DownloadedMusicTrack>>(body)
        }.getOrDefault(emptyList())
    }

    fun isDownloaded(id: String): Boolean {
        return getDownloadedTracks().any { it.id == id } && getTrackFile(id)?.exists() == true
    }

    fun getTrackFile(id: String): File? {
        val track = getDownloadedTracks().firstOrNull { it.id == id } ?: return null
        val file = File(musicDir, track.fileName)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun partialFileFor(trackId: String): File {
        val cleanId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(musicDir, "$cleanId.mp3.partial")
    }

    /**
     * Best-effort remote Content-Length via HEAD (falls back to a 1-byte Range GET).
     * Returns null when the server omits size or rejects the probe.
     */
    fun probeContentLength(url: String, sourceId: String): Long? {
        if (url.isBlank()) return null
        return runCatching {
            val head = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", USER_AGENT)
                .header("X-Source-Id", sourceId)
                .header("Cache-Control", "no-store")
                .build()
            httpClient.newCall(head).execute().use { response ->
                val fromHead = response.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L }
                if (response.isSuccessful && fromHead != null) return@use fromHead
            }
            val range = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("X-Source-Id", sourceId)
                .header("Cache-Control", "no-store")
                .header("Range", "bytes=0-0")
                .build()
            httpClient.newCall(range).execute().use { response ->
                if (response.code !in 200..299 && response.code != 206) return@use null
                parseTotalFromContentRange(response.header("Content-Range"))
                    ?: response.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L }
            }
        }.getOrNull()
    }

    /**
     * Downloads [track] to app files, resuming from any existing `.partial` via HTTP Range.
     * Uses the shared OkHttp client so traffic appears in the network debug bar.
     */
    suspend fun downloadTrack(
        track: RemoteAudioTrack,
        onProgress: (Float) -> Unit = {},
    ): Result<DownloadedMusicTrack> = withContext(Dispatchers.IO) {
        runCatching {
            if (track.streamUrl.isBlank()) {
                error("Empty stream URL for track ${track.id}")
            }
            musicDir.mkdirs()
            val cleanId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "$cleanId.mp3"
            val targetFile = File(musicDir, fileName)
            val partialFile = File(musicDir, "$fileName.partial")

            if (targetFile.exists() && targetFile.length() > 0 && isDownloaded(track.id)) {
                onProgress(1f)
                return@runCatching getDownloadedTracks().first { it.id == track.id }
            }

            downloadResumable(
                url = track.streamUrl,
                sourceId = track.sourceId,
                partialFile = partialFile,
                onProgress = onProgress,
            )

            if (!partialFile.exists() || partialFile.length() == 0L) {
                error("Downloaded file is empty")
            }
            if (targetFile.exists()) targetFile.delete()
            if (!partialFile.renameTo(targetFile)) {
                partialFile.copyTo(targetFile, overwrite = true)
                partialFile.delete()
            }

            val downloadedTrack = DownloadedMusicTrack(
                id = track.id,
                title = track.title,
                artist = track.artist,
                sourceId = track.sourceId,
                fileName = fileName,
                durationSeconds = track.durationSeconds,
                license = track.license,
            )
            saveTrackToManifest(downloadedTrack)
            downloadedTrack
        }
    }

    private fun downloadResumable(
        url: String,
        sourceId: String,
        partialFile: File,
        onProgress: (Float) -> Unit,
    ) {
        var existingBytes = if (partialFile.exists()) partialFile.length() else 0L
        if (existingBytes < 0L) existingBytes = 0L

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("X-Source-Id", sourceId)
            .header("Cache-Control", "no-store")
            .get()
        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val code = response.code
            when {
                code == 206 -> {
                    // Resume append
                }
                code in 200..299 -> {
                    if (existingBytes > 0L) {
                        partialFile.delete()
                        existingBytes = 0L
                    }
                }
                code == 416 -> {
                    if (existingBytes > 0L) {
                        onProgress(1f)
                        return
                    }
                    error("HTTP 416 Requested Range Not Satisfiable for $url")
                }
                else -> error("HTTP Download error $code for $url: ${response.message}")
            }

            val body = response.body
            val contentLength = body.contentLength()
            val totalBytes = when {
                code == 206 && contentLength > 0 -> existingBytes + contentLength
                contentLength > 0 -> contentLength
                else -> -1L
            }

            val append = code == 206 && existingBytes > 0L
            body.byteStream().use { input ->
                FileOutputStream(partialFile, append).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalRead = existingBytes
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (totalBytes > 0) {
                            onProgress((totalRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (totalBytes > 0) onProgress(1f)
        }
    }

    @Synchronized
    fun deleteTrack(id: String) {
        val current = getDownloadedTracks().toMutableList()
        val track = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        val file = File(musicDir, track.fileName)
        runCatching { file.delete() }
        runCatching { partialFileFor(id).delete() }
        runCatching {
            manifestFile.writeText(json.encodeToString(current))
        }
    }

    @Synchronized
    private fun saveTrackToManifest(track: DownloadedMusicTrack) {
        val current = getDownloadedTracks().filterNot { it.id == track.id }.toMutableList()
        current.add(0, track)
        manifestFile.writeText(json.encodeToString(current))
    }

    companion object {
        private const val USER_AGENT = "Arthur-Ambient/1.0 (fr.geoking.arthur)"

        internal fun parseTotalFromContentRange(contentRange: String?): Long? {
            // e.g. "bytes 0-0/12345"
            if (contentRange.isNullOrBlank()) return null
            val total = contentRange.substringAfterLast('/', missingDelimiterValue = "")
            return total.toLongOrNull()?.takeIf { it > 0L }
        }

        private fun defaultHttpClient(): OkHttpClient =
            runCatching { GlobalContext.get().get<OkHttpClient>() }
                .getOrElse {
                    OkHttpClient.Builder()
                        .followRedirects(true)
                        .build()
                }
    }
}
