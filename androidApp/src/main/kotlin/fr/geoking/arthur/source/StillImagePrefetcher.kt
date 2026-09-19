package fr.geoking.arthur.source

import fr.geoking.arthur.shared.domain.Artwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Warms [ArtworkImageCache] so Ambient advances hit disk instead of waiting on the network. */
object StillImagePrefetcher {
    suspend fun ensureCached(
        cache: ArtworkImageCache,
        artwork: Artwork,
        allowNetwork: Boolean = true,
    ) {
        val url = artwork.remoteUrl?.takeIf { it.isNotBlank() } ?: return
        if (!artwork.localPath.isNullOrBlank()) return
        if (cache.hasDecodableImage(artwork.id)) return
        if (!allowNetwork) return
        withContext(Dispatchers.IO) {
            if (cache.hasDecodableImage(artwork.id)) return@withContext
            try {
                cache.downloadAndCache(artwork.id, url, allowNetwork = true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort warm; display path will retry or show placeholder.
            }
        }
    }
}
