package fr.geoking.arthur.tv

import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.isGenerative

/**
 * Same-process handoff of the Control Plane rotation pool into [AmbientActivity].
 * Intent extras cannot carry the full live catalog (remote URLs / ephemeral stock ids).
 *
 * Also keeps the in-memory playlist session so the phone FAB can resume the same pack
 * at the artwork where playback stopped.
 *
 * [renewSourceIds] — when non-null, Ambient reloads these Sources after a full pool
 * cycle so museum/stock variety comes from a fresh API search sample, not a fixed ID list.
 */
object AmbientRotationLaunch {
    @Volatile
    var pool: List<Artwork> = emptyList()
        private set

    @Volatile
    var renewSourceIds: List<String>? = null
        private set

    /** Stable pack identity (`family|subId`) for resume matching. */
    @Volatile
    var packKey: String? = null
        private set

    @Volatile
    var currentArtworkId: String? = null
        private set

    @Volatile
    var seenIds: Set<String> = emptySet()
        private set

    data class Session(
        val artwork: Artwork?,
        val pool: List<Artwork>,
        val renewSourceIds: List<String>?,
        val seenIds: Set<String>,
    )

    /**
     * Bind the upcoming launch to [key]. Switching packs clears the previous playlist
     * so a new sample is loaded; rebinding the same pack keeps the session for resume.
     */
    fun bindPack(key: String) {
        if (packKey == key) return
        packKey = key
        pool = emptyList()
        renewSourceIds = null
        currentArtworkId = null
        seenIds = emptySet()
    }

    /** Clear pack binding so the next launch always samples a fresh pool. */
    fun resetPack() {
        packKey = null
        pool = emptyList()
        renewSourceIds = null
        currentArtworkId = null
        seenIds = emptySet()
    }

    fun prepare(
        pool: List<Artwork>,
        renewSourceIds: List<String>? = null,
        currentArtworkId: String? = null,
        seenIds: Set<String> = emptySet(),
    ) {
        val seedArt = currentArtworkId?.let { id -> pool.firstOrNull { it.id == id } }
        val withinCap = pool.size <= AmbientAlbumArt.MAX_PLAYLIST_SIZE &&
            (pool.all { it.isGenerative } || pool.size <= AmbientAlbumArt.MAX_AUTO_ROTATION_POOL)
        // Keep exact order when resuming an already-sampled playlist (seed already in list).
        this.pool = if (withinCap && seedArt != null) {
            pool
        } else {
            AmbientAlbumArt.sampleRotationPool(pool, seed = seedArt)
        }
        this.renewSourceIds = renewSourceIds
        val poolIds = this.pool.map { it.id }.toSet()
        this.currentArtworkId = when {
            currentArtworkId != null && currentArtworkId in poolIds -> currentArtworkId
            this.currentArtworkId in poolIds -> this.currentArtworkId
            else -> this.pool.firstOrNull()?.id
        }
        this.seenIds = if (seenIds.isNotEmpty()) {
            seenIds.intersect(poolIds)
        } else {
            this.seenIds.intersect(poolIds)
        }
    }

    /** Update the remembered playback position while Ambient is running. */
    fun updatePlayback(
        artworkId: String,
        pool: List<Artwork>,
        seenIds: Set<String> = this.seenIds,
        renewSourceIds: List<String>? = this.renewSourceIds,
    ) {
        this.pool = pool
        this.currentArtworkId = artworkId
        this.seenIds = seenIds
        this.renewSourceIds = renewSourceIds
    }

    /** Resume state when [key] matches the live session and a playlist is available. */
    fun matchingSession(key: String): Session? {
        if (packKey != key || pool.size < 2) return null
        val artwork = pool.firstOrNull { it.id == currentArtworkId } ?: pool.firstOrNull()
        return Session(
            artwork = artwork,
            pool = pool,
            renewSourceIds = renewSourceIds,
            seenIds = seenIds,
        )
    }
}
