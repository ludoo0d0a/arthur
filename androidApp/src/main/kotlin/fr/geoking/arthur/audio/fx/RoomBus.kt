package fr.geoking.arthur.audio.fx

import kotlin.math.PI
import kotlin.math.exp

/** Piano-stem felt roll-off (1-pole LP ~5.2 kHz). Reverb lives in [StereoReverb]. */
class RoomBus {
    private var cachedSr = 0
    private var pianoLp = 0.0
    private var pianoLpCoeff = 0.0

    fun configure(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        pianoLpCoeff = 1.0 - exp(-2.0 * PI * 5200.0 / sampleRate.coerceAtLeast(1))
        pianoLp = 0.0
    }

    fun processPianoLp(sample: Float): Float {
        pianoLp += pianoLpCoeff * (sample - pianoLp)
        return pianoLp.toFloat()
    }

    fun reset() {
        pianoLp = 0.0
    }
}
