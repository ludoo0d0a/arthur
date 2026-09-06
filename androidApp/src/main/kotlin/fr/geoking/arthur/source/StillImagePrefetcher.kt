package fr.geoking.arthur.source

import fr.geoking.arthur.shared.domain.Artwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Warms [ArtworkImageCache] so Ambient advances hit disk instead of waiting on the network. */
object StillImagePrefetcher {
    suspend fun ensureCached(cache: ArtworkImageCache, artwork: Artwork) {
        val url = artwork.remoteUrl?.takeIf { it.isNotBlank() } ?: return
        if (!artwork.localPath.isNullOrBlank()) return
        if (cache.hasImage(artwork.id)) return
        withContext(Dispatchers.IO) {
            if (cache.hasImage(artwork.id)) return@withContext
            try {
                cache.putImage(artwork.id, StillImageDownloader.downloadBytes(url))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort warm; display path will retry or show placeholder.
            }
        }
    }
}
