package fr.geoking.arthur.source

import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/** Downloads remote still images directly to disk using chunked streaming I/O and split chunk support. */
object StillImageDownloader {
    private const val USER_AGENT = "okhttp/4.12.0"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB chunk buffer
    private const val CHUNK_SIZE = 512 * 1024L // 512 KB chunk size for range requests

    // A "still image" is a photo/artwork preview, not a raw asset — a source
    // returning something far larger than this is misbehaving (seen: a 268 MB
    // response that OOM'd the app when fully buffered downstream).
    private const val MAX_IMAGE_BYTES = 25L * 1024 * 1024

    fun downloadToFile(
        url: String,
        targetFile: File,
        errorLogger: ErrorLogger? = null,
        sourceId: String = "image_download",
    ): File {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
        var responseCode: Int? = null
        try {
            targetFile.parentFile?.mkdirs()

            downloadSingleStream(url, tempFile) { code ->
                responseCode = code
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw java.io.IOException("Downloaded file is empty or missing")
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            return targetFile
        } catch (e: Throwable) {
            tempFile.delete()
            val category = ErrorClassifier.classify(responseCode, e)
            errorLogger?.log(
                sourceId = sourceId,
                category = category,
                message = e.message ?: "Failed to download image",
                details = e.stackTraceToString().take(300),
                url = url,
                statusCode = responseCode,
                throwable = e,
            )
            throw e
        }
    }

    /** Downloads in split range chunks if server supports Range requests and file is large. */
    fun downloadInChunks(
        url: String,
        targetFile: File,
        totalLength: Long,
        errorLogger: ErrorLogger? = null,
        sourceId: String = "image_download",
    ): Boolean {
        if (totalLength > MAX_IMAGE_BYTES) {
            errorLogger?.log(
                sourceId = sourceId,
                category = ErrorClassifier.classify(null, null),
                message = "Image too large ($totalLength bytes > $MAX_IMAGE_BYTES max)",
                url = url,
            )
            return false
        }
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
        try {
            targetFile.parentFile?.mkdirs()
            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.setLength(totalLength)
                var start = 0L
                while (start < totalLength) {
                    val end = (start + CHUNK_SIZE - 1).coerceAtMost(totalLength - 1)
                    var chunkConnection: HttpURLConnection? = null
                    try {
                        chunkConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                            connectTimeout = CONNECT_TIMEOUT_MS
                            readTimeout = READ_TIMEOUT_MS
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", USER_AGENT)
                            setRequestProperty("Accept", "image/*,*/*;q=0.8")
                            setRequestProperty("Range", "bytes=$start-$end")
                        }
                        val code = chunkConnection.responseCode
                        if (code != 206) {
                            return false
                        }
                        chunkConnection.inputStream.use { input ->
                            raf.seek(start)
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                raf.write(buffer, 0, bytesRead)
                            }
                        }
                    } finally {
                        chunkConnection?.disconnect()
                    }
                    start = end + 1
                }
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            return true
        } catch (e: Throwable) {
            tempFile.delete()
            errorLogger?.log(
                sourceId = sourceId,
                category = ErrorClassifier.classify(null, e),
                message = e.message ?: "Chunk download failed",
                url = url,
                throwable = e,
            )
            return false
        }
    }

    private fun downloadSingleStream(
        url: String,
        tempFile: File,
        onResponseCode: ((Int) -> Unit)? = null,
    ) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "image/*,*/*;q=0.8")
            }
            val responseCode = connection.responseCode
            onResponseCode?.invoke(responseCode)
            if (responseCode !in 200..299) {
                val responseMsg = runCatching { connection.responseMessage }.getOrNull()?.takeIf { it.isNotBlank() }
                val msgDetails = if (responseMsg != null) " $responseMsg" else ""
                throw java.io.IOException("HTTP $responseCode$msgDetails while downloading image")
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_IMAGE_BYTES) {
                throw java.io.IOException(
                    "Image too large ($declaredLength bytes > $MAX_IMAGE_BYTES max) at $url",
                )
            }
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalRead += bytesRead
                        if (totalRead > MAX_IMAGE_BYTES) {
                            throw java.io.IOException(
                                "Image exceeded $MAX_IMAGE_BYTES byte limit while streaming from $url",
                            )
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    fun downloadBytes(
        url: String,
        errorLogger: ErrorLogger? = null,
        sourceId: String = "image_download",
    ): ByteArray {
        val tempFile = File.createTempFile("still_dl_", ".tmp")
        try {
            downloadToFile(url, tempFile, errorLogger, sourceId)
            return tempFile.readBytes()
        } finally {
            tempFile.delete()
        }
    }
}
