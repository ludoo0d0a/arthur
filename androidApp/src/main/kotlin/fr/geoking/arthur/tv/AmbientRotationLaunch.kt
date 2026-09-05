package fr.geoking.arthur.tv

import fr.geoking.arthur.shared.domain.Artwork

/**
 * Same-process handoff of the Control Plane rotation pool into [AmbientActivity].
 * Intent extras cannot carry the full live catalog (remote URLs / ephemeral stock ids).
 */
object AmbientRotationLaunch {
    @Volatile
    var pool: List<Artwork> = emptyList()
        private set

    fun prepare(pool: List<Artwork>) {
        this.pool = pool
    }
}
