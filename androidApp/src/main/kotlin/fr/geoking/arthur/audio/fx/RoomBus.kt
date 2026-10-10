package fr.geoking.arthur.audio.fx

import kotlin.math.PI
import kotlin.math.exp

/**
 * Lightweight lounge bus: 1-pole LP on the piano stem + short mono reverb on the master.
 * Designed for ambient CPU budgets (Auto / TV adaptive quality).
 */
class RoomBus {
    private var cachedSr = 0
    private var pianoLp = 0.0
    private var pianoLpCoeff = 0.0
    private var enableReverb = true
    private var wet = 0.15f

    private var comb1 = FloatArray(0)
    private var comb2 = FloatArray(0)
    private var allpass = FloatArray(0)
    private var c1i = 0
    private var c2i = 0
    private var api = 0
    private var combFb = 0.72f

    fun configure(sampleRate: Int, reverb: Boolean, wetAmount: Float = 0.15f) {
        enableReverb = reverb
        wet = wetAmount.coerceIn(0f, 0.35f)
        if (sampleRate == cachedSr && comb1.isNotEmpty()) return
        cachedSr = sampleRate
        // Piano felt roll-off ~5.2 kHz.
        pianoLpCoeff = 1.0 - exp(-2.0 * PI * 5200.0 / sampleRate.coerceAtLeast(1))
        pianoLp = 0.0
        val sr = sampleRate.coerceAtLeast(8_000)
        comb1 = FloatArray((0.029 * sr).toInt().coerceAtLeast(2))
        comb2 = FloatArray((0.037 * sr).toInt().coerceAtLeast(2))
        allpass = FloatArray((0.005 * sr).toInt().coerceAtLeast(2))
        c1i = 0
        c2i = 0
        api = 0
        // ~0.9 s RT60-ish for these delay lengths.
        combFb = 0.74f
        comb1.fill(0f)
        comb2.fill(0f)
        allpass.fill(0f)
    }

    /** Soft high roll-off for the piano stem. */
    fun processPianoLp(sample: Float): Float {
        pianoLp += pianoLpCoeff * (sample - pianoLp)
        return pianoLp.toFloat()
    }

    /** Wet/dry short reverb; passthrough when reverb disabled. */
    fun processRoom(sample: Float): Float {
        if (!enableReverb || comb1.isEmpty()) return sample
        val d1 = comb1[c1i]
        comb1[c1i] = sample + d1 * combFb
        c1i++
        if (c1i >= comb1.size) c1i = 0

        val d2 = comb2[c2i]
        comb2[c2i] = sample + d2 * combFb
        c2i++
        if (c2i >= comb2.size) c2i = 0

        var apIn = (d1 + d2) * 0.5f
        val apDelay = allpass[api]
        val apOut = -apIn + apDelay
        allpass[api] = apIn + apDelay * 0.5f
        api++
        if (api >= allpass.size) api = 0

        return sample * (1f - wet) + apOut * wet
    }

    fun reset() {
        pianoLp = 0.0
        comb1.fill(0f)
        comb2.fill(0f)
        allpass.fill(0f)
        c1i = 0
        c2i = 0
        api = 0
    }
}
