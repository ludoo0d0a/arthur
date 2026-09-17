package fr.geoking.arthur.source

import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.DeviantArtSource
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.HarvardSource
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.PexelsVideoSource
import fr.geoking.arthur.shared.source.PixabayVideoSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.SmithsonianSource
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okio.Buffer
import okio.IOException

/** Max bytes peeked off a response body for logging; never consumes the real stream. */
private const val MAX_PEEK_BYTES = 64L * 1024
private const val MAX_BODY_CHARS = 8_192

class DebugInterceptor(
    private val debugLogger: DebugLogger,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sourceId = request.header("X-Source-Id") ?: urlToSourceId(request.url.toString())
        val host = request.url.host
        val requestHeaders = request.headers.toMultimap()
        val (requestBody, requestBodyTruncated) = readRequestBodySnapshot(request)

        debugLogger.recordQueryStart()
        val startTime = System.currentTimeMillis()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            debugLogger.recordQueryEnd(
                sourceId = sourceId,
                url = request.url.toString(),
                durationMs = duration,
                isCached = false,
                statusCode = null,
                method = request.method,
                host = host,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                requestBodyTruncated = requestBodyTruncated,
                errorMessage = e.message ?: e::class.simpleName,
                requestSizeBytes = requestBody?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L,
            )
            throw e
        }
        val duration = System.currentTimeMillis() - startTime
        val isCached = response.networkResponse == null ||
            (response.cacheResponse != null && response.networkResponse?.code == 304)
        val (responseBody, responseBodyTruncated) = readResponseBodySnapshot(response)

        debugLogger.recordQueryEnd(
            sourceId = sourceId,
            url = request.url.toString(),
            durationMs = duration,
            isCached = isCached,
            statusCode = response.code,
            method = request.method,
            host = host,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            requestBodyTruncated = requestBodyTruncated,
            responseHeaders = response.headers.toMultimap(),
            responseBody = responseBody,
            responseBodyTruncated = responseBodyTruncated,
            requestSizeBytes = requestBody?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L,
            responseSizeBytes = responseBody?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L,
        )
        return response
    }

    private fun urlToSourceId(url: String): String = when {
        "metmuseum.org" in url -> MetSource.ID
        "rijksmuseum.nl" in url -> RijksmuseumSource.ID
        "artic.edu" in url -> ArticSource.ID
        "clevelandart.org" in url -> ClevelandSource.ID
        "europeana.eu" in url -> EuropeanaSource.ID
        "harvardartmuseums.org" in url -> HarvardSource.ID
        "si.edu" in url -> SmithsonianSource.ID
        "louvre.fr" in url -> LouvreSource.ID
        "wikimedia.org" in url -> WikimediaStreetArtSource.ID
        "pexels.com" in url -> PexelsSource.ID
        "unsplash.com" in url -> UnsplashSource.ID
        "deviantart.com" in url -> DeviantArtSource.ID
        "pixabay.com" in url -> PixabayVideoSource.ID
        "coverr.co" in url -> CoverrSource.ID
        else -> "network"
    }
}

/** Forces a fresh network fetch (bypassing the OkHttp response cache) while [HttpCacheController.disabled] is set. */
class CacheBypassInterceptor(
    private val cacheController: HttpCacheController,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return if (cacheController.disabled.value) {
            chain.proceed(request.newBuilder().header("Cache-Control", "no-cache").build())
        } else {
            chain.proceed(request)
        }
    }
}

class ForceCacheNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val cacheControl = response.header("Cache-Control").orEmpty()
        if (response.isSuccessful && chain.request().method == "GET" && "no-store" !in cacheControl) {
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=3600")
                .build()
        }
        return response
    }
}

private fun readRequestBodySnapshot(request: okhttp3.Request): Pair<String?, Boolean> {
    val body = request.body ?: return null to false
    if (body.isOneShot() || !isTextualBody(body.contentType())) return null to false
    return try {
        val buffer = Buffer()
        body.writeTo(buffer)
        truncateBody(buffer.readString(Charsets.UTF_8))
    } catch (_: IOException) {
        null to false
    } catch (_: Exception) {
        null to false
    }
}

private fun readResponseBodySnapshot(response: Response): Pair<String?, Boolean> {
    if (!isTextualBody(response.body?.contentType())) return null to false
    return try {
        val text = response.peekBody(MAX_PEEK_BYTES).string()
        truncateBody(text)
    } catch (_: IOException) {
        null to false
    } catch (_: Exception) {
        null to false
    }
}

private fun isTextualBody(mediaType: MediaType?): Boolean {
    if (mediaType == null) return true
    val type = mediaType.type.lowercase()
    val subtype = mediaType.subtype.lowercase()
    return type == "text" || subtype.contains("json") || subtype.contains("xml") || subtype.contains("html")
}

private fun truncateBody(text: String): Pair<String, Boolean> {
    if (text.length <= MAX_BODY_CHARS) return text to false
    return "${text.take(MAX_BODY_CHARS)}\n… [truncated ${text.length - MAX_BODY_CHARS} chars]" to true
}
