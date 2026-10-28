package fr.geoking.arthur.audio.remote

import android.content.Context
import fr.geoking.arthur.shared.audio.remote.RemoteAudioTrack
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

class DownloadedMusicManager(private val context: Context) {
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

    suspend fun downloadTrack(
        track: RemoteAudioTrack,
        onProgress: (Float) -> Unit = {},
    ): Result<DownloadedMusicTrack> = withContext(Dispatchers.IO) {
        runCatching {
            musicDir.mkdirs()
            val cleanId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "$cleanId.mp3"
            val targetFile = File(musicDir, fileName)

            val url = URL(track.streamUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Arthur-Ambient/1.0")
            connection.connect()

            if (connection.responseCode !in 200..299) {
                error("HTTP Download error ${connection.responseCode}: ${connection.responseMessage}")
            }

            val contentLength = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            onProgress((totalRead.toFloat() / contentLength).coerceIn(0f, 1f))
                        }
                    }
                }
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

    @Synchronized
    fun deleteTrack(id: String) {
        val current = getDownloadedTracks().toMutableList()
        val track = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        val file = File(musicDir, track.fileName)
        runCatching { file.delete() }
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
}
