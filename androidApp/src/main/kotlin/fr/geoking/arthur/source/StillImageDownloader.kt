package fr.geoking.arthur.source

import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import java.net.HttpURLConnection
import java.net.URL

/** Downloads remote still bytes with a CDN-friendly User-Agent. */
object StillImageDownloader {
    private const val USER_AGENT = "Arthur/1.0 (Android; fr.geoking.arthur)"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    fun downloadBytes(
        url: String,
        errorLogger: ErrorLogger? = null,
        sourceId: String = "image_download",
    ): ByteArray {
        var responseCode: Int? = null
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "image/*,*/*;q=0.8")
            }
            responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errMessage = "HTTP $responseCode while downloading image"
                val category = ErrorClassifier.classify(responseCode, null)
                errorLogger?.log(
                    sourceId = sourceId,
                    category = category,
                    message = errMessage,
                    url = url,
                    statusCode = responseCode,
                )
                throw java.io.IOException(errMessage)
            }
            return connection.inputStream.use { it.readBytes() }
        } catch (e: Throwable) {
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
        } finally {
            connection?.disconnect()
        }
    }
}
