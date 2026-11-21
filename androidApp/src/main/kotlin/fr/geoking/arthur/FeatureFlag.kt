package fr.geoking.arthur

/**
 * Compile-time product gates for surfaces that are not ready to ship.
 * Flip a flag to re-enable without restoring deleted code.
 */
object FeatureFlag {
    /** Procedural / neural / downloaded music + ambient audio UI and engines. */
    val musicEnabled: Boolean = false

    /** Android Auto car app + media browse surfaces. */
    val androidAutoEnabled: Boolean = false
}
