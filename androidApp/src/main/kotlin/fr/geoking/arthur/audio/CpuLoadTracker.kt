package fr.geoking.arthur.audio

/** Adaptive synth quality based on recent render cost. */
enum class SynthQuality {
    /** All stems as designed. */
    Full,
    /** Drop ornaments / second texture / second pad. */
    Reduced,
    /** Minimal: single pad, bass, melody lead only. */
    Minimal,
}

/**
 * Tracks render cost vs chunk duration and exposes a quality tier for adaptive synth load.
 */
class CpuLoadTracker(
    private val emaAlpha: Float = 0.12f,
) {
    /** Exponential moving average of (renderNs / chunkNs), typically 0..~1.5. */
    var loadEma: Float = 0f
        private set

    var quality: SynthQuality = SynthQuality.Full
        private set

    fun observe(renderNs: Long, chunkFrames: Int, sampleRate: Int) {
        if (chunkFrames <= 0 || sampleRate <= 0 || renderNs < 0L) return
        val chunkNs = chunkFrames.toLong() * 1_000_000_000L / sampleRate
        if (chunkNs <= 0L) return
        val ratio = (renderNs.toDouble() / chunkNs.toDouble()).toFloat().coerceIn(0f, 4f)
        loadEma = if (loadEma <= 0f) ratio else loadEma * (1f - emaAlpha) + ratio * emaAlpha
        quality = when {
            loadEma >= 0.92f -> SynthQuality.Minimal
            loadEma >= 0.72f -> SynthQuality.Reduced
            else -> SynthQuality.Full
        }
    }

    fun reset() {
        loadEma = 0f
        quality = SynthQuality.Full
    }
}
