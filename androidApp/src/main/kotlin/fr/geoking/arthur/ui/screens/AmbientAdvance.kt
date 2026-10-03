package fr.geoking.arthur.ui.screens

import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.StillImagePrefetcher

/**
 * Picks the next rotation candidate that is display-ready (generative/video/local/cached).
 * Returns null when no eligible candidate can be prepared.
 */
internal suspend fun pickNextReadyArtwork(
    pool: List<Artwork>,
    fromId: String?,
    delta: Int,
    skipIds: Set<String>,
    eligibleIds: Set<String>,
    imageCache: ArtworkImageCache,
    allowNetwork: Boolean,
): Artwork? {
    if (pool.isEmpty() || eligibleIds.isEmpty()) return null
    val index = pool.indexOfFirst { it.id == fromId }.let { if (it < 0) 0 else it }
    var steps = 0
    var idx = index
    while (steps < pool.size) {
        idx = if (delta >= 0) {
            AmbientAlbumArt.advanceIndex(idx, pool.size)
        } else {
            Math.floorMod(idx - 1, pool.size)
        }
        val candidate = pool[idx]
        steps++
        if (candidate.id !in eligibleIds) continue
        if (candidate.id in skipIds) continue
        val ready = ensureArtworkReadyForAdvance(
            artwork = candidate,
            imageCache = imageCache,
            allowNetwork = allowNetwork,
        )
        if (ready) return candidate
    }
    return null
}

internal suspend fun ensureArtworkReadyForAdvance(
    artwork: Artwork,
    imageCache: ArtworkImageCache,
    allowNetwork: Boolean,
): Boolean {
    if (artwork.isGenerative) return true
    if (artwork.kind == ArtworkKind.Video) {
        return !artwork.remoteUrl.isNullOrBlank() || !artwork.localPath.isNullOrBlank()
    }
    if (!artwork.localPath.isNullOrBlank()) return true
    StillImagePrefetcher.ensureCached(imageCache, artwork, allowNetwork = allowNetwork)
    return imageCache.hasDecodableImage(artwork.id) || !artwork.localPath.isNullOrBlank()
}
