package fr.geoking.arthur.genart.engines

import fr.geoking.arthur.genart.GenartQuality
import kotlin.math.ceil
import kotlin.math.sin

/** Shared bounded tessellation for the live waves and their fixed preview. */
internal fun wavesSegments(width: Float, quality: GenartQuality): Int {
    val cap = when (quality) {
        GenartQuality.Low -> 128
        GenartQuality.Medium -> 256
        GenartQuality.High -> 384
    }
    if (!width.isFinite() || width <= 0f) return 2
    val spacing = if (quality == GenartQuality.Low) 8f else 6f
    return ceil(width / spacing).toInt().coerceIn(2, cap)
}

/** Two periodic scales give the crest a subtle, continuously looping water-like shape. */
internal fun wavesHeight(x: Float, centerY: Float, amplitude: Float, frequency: Float, phase: Float): Float {
    val angle = x * frequency + phase
    return centerY + amplitude * (sin(angle) + 0.12f * sin(2f * angle + 0.6f))
}
