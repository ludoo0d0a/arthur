package fr.geoking.arthur.tv

import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.shared.domain.Artwork

/**
 * Same-process handoff of the Control Plane rotation pool into [AmbientActivity].
 * Intent extras cannot carry the full live catalog (remote URLs / ephemeral stock ids).
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

    fun prepare(pool: List<Artwork>, renewSourceIds: List<String>? = null) {
        this.pool = AmbientAlbumArt.sampleRotationPool(pool)
        this.renewSourceIds = renewSourceIds
    }
}
