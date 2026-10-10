package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.exp
import kotlin.random.Random

/**
 * Compact multi-cycle piano wavetables generated at class-load (no asset files).
 * Two key zones × attack/body cycles; harmonic stacks are piano-like (stretched).
 */
object PianoWavetable {
    const val SIZE = 1024
    private const val MASK = SIZE - 1

    /** Low-key attack (brighter). */
    val lowAttack: FloatArray = buildCycle(
        amps = floatArrayOf(1f, 0.55f, 0.38f, 0.22f, 0.14f, 0.08f, 0.04f),
        stretch = 0.0007,
    )

    /** Low-key body (warmer). */
    val lowBody: FloatArray = buildCycle(
        amps = floatArrayOf(1f, 0.38f, 0.22f, 0.12f, 0.06f, 0.03f, 0.015f),
        stretch = 0.00055,
    )

    /** High-key attack. */
    val highAttack: FloatArray = buildCycle(
        amps = floatArrayOf(1f, 0.48f, 0.28f, 0.14f, 0.07f, 0.03f, 0.01f),
        stretch = 0.00045,
    )

    /** High-key body. */
    val highBody: FloatArray = buildCycle(
        amps = floatArrayOf(1f, 0.32f, 0.16f, 0.07f, 0.03f, 0.012f, 0.005f),
        stretch = 0.0004,
    )

    /** Split around C4 (~261 Hz). */
    const val ZONE_SPLIT_HZ = 261.63f

    fun lookup(table: FloatArray, phase01: Double): Float {
        var p = phase01 % 1.0
        if (p < 0) p += 1.0
        val idx = p * SIZE
        val i0 = idx.toInt()
        val frac = (idx - i0).toFloat()
        val a = table[i0 and MASK]
        val b = table[(i0 + 1) and MASK]
        return a + (b - a) * frac
    }

    private fun buildCycle(amps: FloatArray, stretch: Double): FloatArray {
        val out = FloatArray(SIZE)
        var peak = 0f
        for (i in 0 until SIZE) {
            val x = i.toDouble() / SIZE
            var s = 0.0
            for (n in amps.indices) {
                val harm = n + 1
                val ratio = harm * (1.0 + stretch * harm * harm)
                s += amps[n] * kotlin.math.sin(2.0 * PI * ratio * x)
            }
            out[i] = s.toFloat()
            peak = maxOf(peak, kotlin.math.abs(out[i]))
        }
        if (peak > 1e-6f) {
            val inv = 1f / peak
            for (i in out.indices) out[i] *= inv
        }
        return out
    }
}

/** Wavetable piano voice with hammer click, dual decay, velocity brightness. */
class WavetablePianoVoice : SynthVoice {
    private var phase = 0.0
    private var phaseInc = 0.0
    private var freq = 220.0
    private var age = 0L
    private var env = 0.0
    private var bright = 0.0
    private var peak = 0.0
    private var brightness = 0.7
    private var active = false
    private var releasing = false
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var attackSamples = 1
    private var morphSamples = 1
    private var hammerSamples = 1
    private var decayMul = 0.999
    private var brightDecayMul = 0.999
    private var releaseMul = 0.99
    private var maxAge = 0L
    private var hammerLp = 0.0
    private var hammerLpCoeff = 0.0
    private var noise = Random(1)
    private var attackTable = PianoWavetable.lowAttack
    private var bodyTable = PianoWavetable.lowBody

    val isActive: Boolean get() = active
    val ageSamples: Long get() = age

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        val vel = velocity.coerceIn(0.1f, 1f)
        freq = freqHz.toDouble()
        peak = vel.toDouble()
        brightness = 0.45 + 0.55 * vel
        age = 0L
        env = 0.0
        bright = 1.0
        active = true
        releasing = false
        hammerLp = 0.0
        noise = Random((freqHz * 91).toInt().coerceAtLeast(1))
        phase = 0.0
        if (freqHz < PianoWavetable.ZONE_SPLIT_HZ) {
            attackTable = PianoWavetable.lowAttack
            bodyTable = PianoWavetable.lowBody
        } else {
            attackTable = PianoWavetable.highAttack
            bodyTable = PianoWavetable.highBody
        }
        cachedSr = 0
    }

    override fun noteOff() {
        if (active && !releasing) releasing = true
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        attackSamples = (0.008 * sampleRate).toInt().coerceAtLeast(1)
        morphSamples = (0.045 * sampleRate).toInt().coerceAtLeast(1)
        hammerSamples = (0.008 * sampleRate).toInt().coerceAtLeast(1)
        decayMul = EnvMathPublic.decayMul(1.7, sampleRate)
        brightDecayMul = EnvMathPublic.decayMul(4.5, sampleRate)
        releaseMul = EnvMathPublic.decayMul(6.0, sampleRate)
        maxAge = (4.8 * sampleRate).toLong()
        hammerLpCoeff = 1.0 - exp(-2.0 * PI * 2400.0 * invSr)
        phaseInc = freq * invSr // cycles per sample in [0,1) domain
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        age++
        if (releasing) {
            env *= releaseMul
            bright *= brightDecayMul
            if (env < 1e-4) {
                active = false
                return 0f
            }
        } else if (age <= attackSamples) {
            env = peak * age.toDouble() / attackSamples
        } else {
            env *= decayMul
            bright *= brightDecayMul
            if (age > maxAge || env < 1e-4) {
                active = false
                return 0f
            }
        }
        phase += phaseInc
        if (phase >= 1.0) phase -= 1.0
        val morph = (age.toDouble() / morphSamples).coerceIn(0.0, 1.0)
        val morphBright = morph * (1.0 - 0.35 * brightness) // hard strike stays brighter longer
        val a = PianoWavetable.lookup(attackTable, phase)
        val b = PianoWavetable.lookup(bodyTable, phase)
        val wave = a * (1f - morphBright.toFloat()) + b * morphBright.toFloat()
        var hammer = 0f
        if (age <= hammerSamples) {
            val w = 1.0 - age.toDouble() / hammerSamples
            val raw = noise.nextFloat() * 2f - 1f
            hammerLp += hammerLpCoeff * (raw - hammerLp)
            hammer = (hammerLp * 0.18 * peak * brightness * w).toFloat()
        }
        val brightF = bright.toFloat().coerceIn(0f, 1f)
        // Soft high shelf: blend in a bit of differentiated wave for brightness.
        val edged = wave + (wave - b) * 0.25f * brightF * brightness.toFloat()
        return edged * 0.42f * env.toFloat() + hammer
    }

    override fun reset() {
        active = false
        releasing = false
        env = 0.0
        bright = 0.0
        hammerLp = 0.0
    }
}

/** Wavetable piano pool with release-before-steal. */
class WavetablePianoPool(
    voiceCount: Int = 6,
) : SynthVoice {
    private val voices = Array(voiceCount.coerceAtLeast(1)) { WavetablePianoVoice() }

    override fun isAudible(): Boolean = voices.any { it.isActive }

    override fun noteOn(freqHz: Float, velocity: Float) {
        val free = voices.firstOrNull { !it.isActive }
        val target = free ?: voices.maxByOrNull { it.ageSamples } ?: voices[0]
        if (target.isActive) target.noteOff()
        target.noteOn(freqHz, velocity)
    }

    override fun noteOff() {
        for (v in voices) v.noteOff()
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!isAudible()) return 0f
        var sum = 0f
        for (v in voices) {
            if (v.isActive) sum += v.render(sampleIndex, sampleRate)
        }
        return sum
    }

    override fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float) {
        if (!isAudible() || gain == 0f || frames <= 0) return
        for (v in voices) {
            if (v.isActive) v.renderInto(out, offset, frames, sampleRate, gain)
        }
    }

    override fun reset() {
        for (v in voices) v.reset()
    }
}

/** Package-visible decay helper (mirrors private EnvMath in SynthVoices). */
internal object EnvMathPublic {
    fun decayMul(ratePerSec: Double, sampleRate: Int): Double =
        exp(-ratePerSec / sampleRate.coerceAtLeast(1))
}
