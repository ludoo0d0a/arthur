package fr.geoking.arthur.source

/**
 * Thrown when Wi‑Fi-only / cache-only mode blocks a remote still download and no
 * valid cached file exists for [artworkId].
 */
class RemoteStillCacheOnlyMiss(
    val artworkId: String,
) : Exception("Remote still $artworkId unavailable in cache-only mode")
