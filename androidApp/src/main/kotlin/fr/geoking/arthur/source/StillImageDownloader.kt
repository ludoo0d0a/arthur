package fr.geoking.arthur.source

import java.net.HttpURLConnection
import java.net.URL

/** Downloads remote still bytes with a CDN-friendly User-Agent. */
object StillImageDownloader {
    private const val USER_AGENT = "okhttp/4.12.0"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    fun downloadBytes(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "image/*,*/*;q=0.8")
        }
        return try {
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
