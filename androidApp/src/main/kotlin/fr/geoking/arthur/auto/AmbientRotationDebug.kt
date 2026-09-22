package fr.geoking.arthur.auto

/**
 * Snapshot of Ambient rotation diagnostics for Android Auto developer mode.
 */
data class AmbientRotationDebug(
    val querySourceIds: List<String>,
    val cacheCount: Int,
    val liveCount: Int,
    val poolSize: Int,
    val seenCount: Int,
    val unseenCount: Int,
    val consecutiveAutoRotations: Int,
    val isPlaying: Boolean,
    val currentArtworkId: String?,
    val generation: Long,
    val queryLaunched: Boolean,
)
