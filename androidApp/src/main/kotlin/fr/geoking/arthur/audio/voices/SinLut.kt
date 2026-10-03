package fr.geoking.arthur.audio.voices

import kotlin.math.PI

/**
 * Shared sine wavetable (2048) with linear interpolation.
 * Phase is in radians; wraps via bit mask on the table index domain.
 */
object SinLut {
    const val SIZE = 2048
    private const val MASK = SIZE - 1
    private val table = FloatArray(SIZE) { i ->
        // Qualify math.sin — bare sin() would recurse into SinLut.sin before table exists.
        kotlin.math.sin(2.0 * PI * i / SIZE).toFloat()
    }

    /** Fast sin for phase in radians (any range). */
    @JvmStatic
    fun sin(phaseRad: Double): Float {
        val twoPi = 2.0 * PI
        var p = phaseRad % twoPi
        if (p < 0) p += twoPi
        val idx = p * (SIZE / twoPi)
        val i0 = idx.toInt()
        val frac = (idx - i0).toFloat()
        val a = table[i0 and MASK]
        val b = table[(i0 + 1) and MASK]
        return a + (b - a) * frac
    }

    /** Advance phase in radians and wrap to [0, 2π). */
    @JvmStatic
    fun wrapPhase(phase: Double): Double {
        val twoPi = 2.0 * PI
        var p = phase
        // Fast path for typical oscillator increments.
        if (p >= twoPi) {
            p -= twoPi
            if (p >= twoPi) p %= twoPi
        } else if (p < 0) {
            p = (p % twoPi) + twoPi
        }
        return p
    }
}
