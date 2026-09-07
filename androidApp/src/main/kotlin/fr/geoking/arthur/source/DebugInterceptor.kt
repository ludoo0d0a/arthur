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
import okhttp3.Response

class DebugInterceptor(
    private val debugLogger: DebugLogger,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sourceId = request.header("X-Source-Id") ?: urlToSourceId(request.url.toString())
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
            )
            throw e
        }
        val duration = System.currentTimeMillis() - startTime
        val isCached = response.networkResponse == null ||
            (response.cacheResponse != null && response.networkResponse?.code == 304)

        debugLogger.recordQueryEnd(
            sourceId = sourceId,
            url = request.url.toString(),
            durationMs = duration,
            isCached = isCached,
            statusCode = response.code,
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
