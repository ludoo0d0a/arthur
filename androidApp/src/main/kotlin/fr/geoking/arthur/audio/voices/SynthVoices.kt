package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

/** Minimal voice interface — renders one mono sample; optional block fill. */
interface SynthVoice {
    fun render(sampleIndex: Long, sampleRate: Int): Float
    /** Add [frames] samples into [out] starting at [offset], scaled by [gain]. */
    fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float = 1f) {
        if (gain == 0f || frames <= 0 || !isAudible()) return
        var idx = 0L
        val end = offset + frames
        for (i in offset until end) {
            out[i] += render(idx++, sampleRate) * gain
        }
    }

    fun noteOn(freqHz: Float, velocity: Float = 1f) {}
    fun noteOff() {}
    fun reset() {}
    /** True when the voice may produce non-zero output. */
    fun isAudible(): Boolean = true
}

/** Per-sample multiply factors derived once at note-on / SR change. */
private object EnvMath {
    fun decayMul(ratePerSec: Double, sampleRate: Int): Double =
        exp(-ratePerSec / sampleRate.coerceAtLeast(1))
}

/**
 * Soft chord pad: low gain, tiny detune.
 * Chord changes glide target frequencies to avoid clicks.
 */
class SinePadVoice(
    private val detuneCents: Float = 2.5f,
    private val glideSeconds: Float = 0.06f,
) : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var f1 = 220.0
    private var f2 = 330.0
    private var f3 = 440.0
    private var t1 = 220.0
    private var t2 = 330.0
    private var t3 = 440.0
    private var targetGain = 0.12
    private var currentGain = 0.0
    private var glideSecondsOverride: Float = glideSeconds
    private var cachedSr = 0
    private var fCoeff = 0.0
    private var gCoeff = 0.0
    private var invSr = 0.0

    fun setChord(freqs: List<Float>) {
        if (freqs.isEmpty()) return
        t1 = freqs[0].toDouble()
        t2 = freqs.getOrElse(1) { freqs[0] * 1.5f }.toDouble()
        t3 = freqs.getOrElse(2) { freqs[0] * 2f }.toDouble()
        val det = 2.0.pow(detuneCents / 1200.0)
        t2 *= det
        t3 /= det
        targetGain = 0.12
    }

    fun setGlideSeconds(seconds: Float) {
        glideSecondsOverride = seconds.coerceIn(0.02f, 0.5f)
        cachedSr = 0
    }

    private fun ensureCoeffs(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        val glide = glideSecondsOverride.coerceAtLeast(0.02f)
        fCoeff = 1.0 - exp(-1.0 / (sampleRate * glide))
        gCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.03))
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        ensureCoeffs(sampleRate)
        f1 += (t1 - f1) * fCoeff
        f2 += (t2 - f2) * fCoeff
        f3 += (t3 - f3) * fCoeff
        currentGain += (targetGain - currentGain) * gCoeff
        phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * f1 * invSr)
        phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * f2 * invSr)
        phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * f3 * invSr)
        val s = SinLut.sin(phase1) + 0.4f * SinLut.sin(phase2) + 0.22f * SinLut.sin(phase3)
        return (s * currentGain).toFloat()
    }

    override fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float) {
        if (frames <= 0 || (!isAudible() && gain == 0f)) return
        ensureCoeffs(sampleRate)
        val end = offset + frames
        var i = offset
        while (i < end) {
            f1 += (t1 - f1) * fCoeff
            f2 += (t2 - f2) * fCoeff
            f3 += (t3 - f3) * fCoeff
            currentGain += (targetGain - currentGain) * gCoeff
            phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * f1 * invSr)
            phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * f2 * invSr)
            phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * f3 * invSr)
            val s = SinLut.sin(phase1) + 0.4f * SinLut.sin(phase2) + 0.22f * SinLut.sin(phase3)
            out[i] += (s * currentGain * gain).toFloat()
            i++
        }
    }

    override fun noteOff() {
        targetGain = 0.0
    }

    override fun reset() {
        currentGain = 0.0
        targetGain = 0.0
    }

    override fun isAudible(): Boolean = currentGain > 1e-5 || targetGain > 1e-5
}

/** Soft sine bass with multiplicative envelopes. */
class SoftBassVoice : SynthVoice {
    private var phase = 0.0
    private var phase2 = 0.0
    private var freq = 55.0
    private var targetFreq = 55.0
    private var age = 0L
    private var env = 0.0
    private var peak = 0.0
    private var active = false
    private var releasing = false
    private var cachedSr = 0
    private var gCoeff = 0.0
    private var invSr = 0.0
    private var attackSamples = 1
    private var decayMul = 0.999
    private var releaseMul = 0.99
    private var maxAge = 0L

    val isActive: Boolean get() = active

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        targetFreq = freqHz.toDouble().coerceIn(40.0, 140.0)
        if (!active) {
            freq = targetFreq
            phase = 0.0
            phase2 = 0.0
        }
        peak = velocity.coerceIn(0.1f, 1f) * 0.55
        age = 0L
        env = 0.0
        active = true
        releasing = false
        cachedSr = 0 // refresh attack/decay for current SR on next render
    }

    override fun noteOff() {
        if (active) releasing = true
    }

    private fun ensureCoeffs(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        gCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.08))
        attackSamples = (0.015 * sampleRate).toInt().coerceAtLeast(1)
        decayMul = EnvMath.decayMul(1.2, sampleRate)
        releaseMul = EnvMath.decayMul(8.0, sampleRate)
        maxAge = (6.0 * sampleRate).toLong()
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureCoeffs(sampleRate)
        freq += (targetFreq - freq) * gCoeff
        age++
        if (releasing) {
            env *= releaseMul
            if (env < 1e-4) {
                active = false
                return 0f
            }
        } else if (age <= attackSamples) {
            env = peak * age.toDouble() / attackSamples
        } else {
            env *= decayMul
            if (age > maxAge || env < 1e-4) {
                active = false
                return 0f
            }
        }
        phase = SinLut.wrapPhase(phase + 2.0 * PI * freq * invSr)
        phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * freq * 2.0 * invSr)
        val body = SinLut.sin(phase) + 0.18f * SinLut.sin(phase2)
        return (body * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
        env = 0.0
    }
}

/** Single piano voice — multiplicative decay/release, no per-sample exp(). */
class SoftPianoVoice : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var phase4 = 0.0
    private var freq = 0.0
    private var age = 0L
    private var env = 0.0
    private var peak = 0.0
    private var active = false
    private var releasing = false
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var attackSamples = 1
    private var decayMul = 0.999
    private var releaseMul = 0.99
    private var maxAge = 0L
    private var phaseInc1 = 0.0
    private var phaseInc2 = 0.0
    private var phaseInc3 = 0.0
    private var phaseInc4 = 0.0

    val isActive: Boolean get() = active
    val ageSamples: Long get() = age

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        peak = velocity.coerceIn(0.1f, 1f).toDouble()
        age = 0L
        env = 0.0
        active = true
        releasing = false
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
        phase4 = 0.0
        cachedSr = 0
    }

    override fun noteOff() {
        if (active && !releasing) releasing = true
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        attackSamples = (0.006 * sampleRate).toInt().coerceAtLeast(1)
        decayMul = EnvMath.decayMul(3.2, sampleRate)
        releaseMul = EnvMath.decayMul(12.0, sampleRate)
        maxAge = (2.8 * sampleRate).toLong()
        phaseInc1 = 2.0 * PI * freq * invSr
        phaseInc2 = 2.0 * PI * freq * 2.002 * invSr
        phaseInc3 = 2.0 * PI * freq * 3.004 * invSr
        phaseInc4 = 2.0 * PI * freq * 4.006 * invSr
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        age++
        if (releasing) {
            env *= releaseMul
            if (env < 1e-4) {
                active = false
                return 0f
            }
        } else if (age <= attackSamples) {
            env = peak * age.toDouble() / attackSamples
        } else {
            env *= decayMul
            if (age > maxAge || env < 1e-4) {
                active = false
                return 0f
            }
        }
        phase1 = SinLut.wrapPhase(phase1 + phaseInc1)
        phase2 = SinLut.wrapPhase(phase2 + phaseInc2)
        phase3 = SinLut.wrapPhase(phase3 + phaseInc3)
        phase4 = SinLut.wrapPhase(phase4 + phaseInc4)
        val h1 = SinLut.sin(phase1)
        val h2 = SinLut.sin(phase2) * 0.32f
        val h3 = SinLut.sin(phase3) * 0.14f
        val h4 = SinLut.sin(phase4) * 0.06f
        return ((h1 + h2 + h3 + h4) * 0.48f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
        env = 0.0
    }
}

/** 4-voice piano pool with release-before-steal. */
class SoftPianoPool(
    voiceCount: Int = 4,
) : SynthVoice {
    private val voices = Array(voiceCount.coerceAtLeast(1)) { SoftPianoVoice() }

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

class PluckGuitarVoice : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var freq = 0.0
    private var age = 0L
    private var env = 0.0
    private var peak = 0.0
    private var active = false
    private var releasing = false
    private var noise = Random(1)
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var attackSamples = 1
    private var burstSamples = 1
    private var decayMul = 0.999
    private var releaseMul = 0.99
    private var maxAge = 0L
    private var phaseInc1 = 0.0
    private var phaseInc2 = 0.0
    private var phaseInc3 = 0.0

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        peak = velocity.coerceIn(0.1f, 1f).toDouble()
        age = 0L
        env = 0.0
        active = true
        releasing = false
        noise = Random((freqHz * 100).toInt())
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
        cachedSr = 0
    }

    override fun noteOff() {
        if (active && !releasing) releasing = true
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        attackSamples = (0.004 * sampleRate).toInt().coerceAtLeast(1)
        burstSamples = (0.008 * sampleRate).toInt().coerceAtLeast(1)
        decayMul = EnvMath.decayMul(3.0, sampleRate)
        releaseMul = EnvMath.decayMul(14.0, sampleRate)
        maxAge = (2.8 * sampleRate).toLong()
        phaseInc1 = 2.0 * PI * freq * invSr
        phaseInc2 = 2.0 * PI * freq * 2.0 * invSr
        phaseInc3 = 2.0 * PI * freq * 3.0 * invSr
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        age++
        if (releasing) {
            env *= releaseMul
            if (env < 1e-4) {
                active = false
                return 0f
            }
        } else if (age <= attackSamples) {
            env = peak * age.toDouble() / attackSamples
        } else {
            env *= decayMul
            if (age > maxAge || env < 1e-4) {
                active = false
                return 0f
            }
        }
        val burst = if (age <= burstSamples) {
            val w = 1.0 - age.toDouble() / burstSamples
            (noise.nextFloat() * 2f - 1f) * 0.18f * w.toFloat()
        } else {
            0f
        }
        phase1 = SinLut.wrapPhase(phase1 + phaseInc1)
        phase2 = SinLut.wrapPhase(phase2 + phaseInc2)
        phase3 = SinLut.wrapPhase(phase3 + phaseInc3)
        val h1 = SinLut.sin(phase1)
        val h2 = SinLut.sin(phase2) * 0.3f
        val h3 = SinLut.sin(phase3) * 0.12f
        return burst + ((h1 + h2 + h3) * 0.4f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
        env = 0.0
    }
}

class BowlVoice : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var freq = 0.0
    private var age = 0L
    private var env = 0.0
    private var peak = 0.0
    private var active = false
    private var sustain = false
    private var sustainPartials: List<Double> = emptyList()
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var attackSamples = 1
    private var decayMul = 0.999
    private var maxAge = 0L

    override fun isAudible(): Boolean = active

    fun setSustain(freqHz: Float, on: Boolean) {
        setSustainPartials(listOf(freqHz), on)
    }

    fun setSustainPartials(freqs: List<Float>, on: Boolean) {
        sustain = on
        if (on && freqs.isNotEmpty()) {
            sustainPartials = freqs.take(3).map { it.toDouble() }
            freq = sustainPartials[0]
            active = true
            age = 0L
            peak = 0.32
            env = 0.0
            phase1 = 0.0
            phase2 = 0.0
            phase3 = 0.0
            cachedSr = 0
        } else if (!on) {
            sustain = false
        }
    }

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        peak = velocity.coerceIn(0.08f, 0.85f).toDouble()
        age = 0L
        env = 0.0
        active = true
        sustain = false
        sustainPartials = emptyList()
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
        cachedSr = 0
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        attackSamples = (0.03 * sampleRate).toInt().coerceAtLeast(1)
        decayMul = if (sustain) 1.0 else EnvMath.decayMul(0.7, sampleRate)
        maxAge = (6.0 * sampleRate).toLong()
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        age++
        if (age <= attackSamples) {
            env = peak * age.toDouble() / attackSamples
            if (sustain) env *= 0.42 / 0.32 // settle toward sustain level after attack
        } else if (sustain) {
            env += (peak * 0.42 - env) * 0.002
        } else {
            env *= decayMul
            if (age > maxAge || env < 1e-4) {
                active = false
                return 0f
            }
        }
        return if (sustain && sustainPartials.size >= 2) {
            val fA = sustainPartials[0]
            val fB = sustainPartials.getOrElse(1) { fA * 1.5 }
            val fC = sustainPartials.getOrElse(2) { fA * 2.76 }
            phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * fA * invSr)
            phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * fB * invSr)
            phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * fC * invSr)
            val s = SinLut.sin(phase1) + 0.4f * SinLut.sin(phase2) + 0.16f * SinLut.sin(phase3)
            (s * 0.30f * env).toFloat()
        } else {
            phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * freq * invSr)
            phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * freq * 2.76 * invSr)
            phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * freq * 5.40 * invSr)
            val h1 = SinLut.sin(phase1)
            val h2 = SinLut.sin(phase2) * 0.45f
            val h3 = SinLut.sin(phase3) * 0.18f
            ((h1 + h2 + h3) * 0.32f * env).toFloat()
        }
    }

    override fun reset() {
        active = false
        sustain = false
        sustainPartials = emptyList()
        env = 0.0
    }
}

class WaveNoiseVoice(
    private val seed: Long = 42L,
) : SynthVoice {
    private val rng = Random(seed)
    private var lp = 0.0
    private var rootHz = 110f
    private var phase = 0.0
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var phaseInc = 0.0

    fun setRootHz(hz: Float) {
        rootHz = hz.coerceIn(40f, 400f)
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        phaseInc = 2.0 * PI * 0.05 * invSr
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        ensureSr(sampleRate)
        phase = SinLut.wrapPhase(phase + phaseInc)
        val swell = 0.5 + 0.5 * SinLut.sin(phase + 0.3 * SinLut.sin(phase * 0.4))
        val n = rng.nextFloat() * 2.0 - 1.0
        val cutoff = (0.04 + (rootHz / 400.0) * 0.08).coerceIn(0.03, 0.14)
        lp += cutoff * (n - lp)
        val tone = 0.85 + 0.15 * SinLut.sin(phase * (rootHz / 40.0) * 0.01)
        return (lp * 0.26 * swell * tone).toFloat()
    }

    override fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float) {
        if (frames <= 0 || gain == 0f) return
        ensureSr(sampleRate)
        val end = offset + frames
        var i = offset
        while (i < end) {
            phase = SinLut.wrapPhase(phase + phaseInc)
            val swell = 0.5 + 0.5 * SinLut.sin(phase + 0.3 * SinLut.sin(phase * 0.4))
            val n = rng.nextFloat() * 2.0 - 1.0
            val cutoff = (0.04 + (rootHz / 400.0) * 0.08).coerceIn(0.03, 0.14)
            lp += cutoff * (n - lp)
            val tone = 0.85 + 0.15 * SinLut.sin(phase * (rootHz / 40.0) * 0.01)
            out[i] += (lp * 0.26 * swell * tone * gain).toFloat()
            i++
        }
    }
}

class WindTextureVoice(
    private val seed: Long = 7L,
) : SynthVoice {
    private val rng = Random(seed)
    private var hp = 0.0
    private var prev = 0.0
    private var rootHz = 220f
    private var phase = 0.0
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var phaseInc = 0.0

    fun setRootHz(hz: Float) {
        rootHz = hz.coerceIn(40f, 500f)
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        phaseInc = 2.0 * PI * 0.07 * invSr
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        ensureSr(sampleRate)
        phase = SinLut.wrapPhase(phase + phaseInc)
        val gust = (0.3 + 0.7 * ((SinLut.sin(phase) + 1.0) * 0.5)).coerceIn(0.0, 1.0)
        val n = rng.nextFloat() * 2.0 - 1.0
        val high = n - prev
        prev = n
        val bright = (0.08 + (rootHz / 500.0) * 0.12).coerceIn(0.06, 0.22)
        hp = (1.0 - bright) * hp + bright * high
        val tone = 0.9 + 0.1 * SinLut.sin(phase * 0.3)
        return (hp * 0.18 * gust * tone).toFloat()
    }

    override fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float) {
        if (frames <= 0 || gain == 0f) return
        ensureSr(sampleRate)
        val end = offset + frames
        var i = offset
        while (i < end) {
            phase = SinLut.wrapPhase(phase + phaseInc)
            val gust = (0.3 + 0.7 * ((SinLut.sin(phase) + 1.0) * 0.5)).coerceIn(0.0, 1.0)
            val n = rng.nextFloat() * 2.0 - 1.0
            val high = n - prev
            prev = n
            val bright = (0.08 + (rootHz / 500.0) * 0.12).coerceIn(0.06, 0.22)
            hp = (1.0 - bright) * hp + bright * high
            val tone = 0.9 + 0.1 * SinLut.sin(phase * 0.3)
            out[i] += (hp * 0.18 * gust * tone * gain).toFloat()
            i++
        }
    }
}

class SoftPulseVoice : SynthVoice {
    private var age = 0L
    private var active = false
    private var env = 0.0
    private var phase = 0.0
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var decayMul = 0.99
    private var maxAge = 0L
    private var phaseInc = 0.0

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        env = velocity.coerceIn(0.05f, 1f).toDouble()
        age = 0L
        active = true
        phase = 0.0
        cachedSr = 0
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
            decayMul = EnvMath.decayMul(40.0, sampleRate)
            maxAge = (0.12 * sampleRate).toLong()
            phaseInc = 2.0 * PI * 90.0 * invSr
        }
        age++
        env *= decayMul
        if (age > maxAge || env < 1e-4) {
            active = false
            return 0f
        }
        phase = SinLut.wrapPhase(phase + phaseInc)
        return (SinLut.sin(phase) * 0.35f * env).toFloat()
    }

    override fun reset() {
        active = false
        env = 0.0
    }
}

class ChimeClusterVoice : SynthVoice {
    private data class Partial(
        var freq: Double,
        var age: Long,
        var env: Double,
        var peak: Float,
        var releasing: Boolean = false,
        var phase: Double = 0.0,
        var phaseInc: Double = 0.0,
        var decayMul: Double = 0.999,
        var releaseMul: Double = 0.99,
        var attackSamples: Int = 1,
        var maxAge: Long = 0L,
    )

    private val partials = ArrayList<Partial>(8)
    private var cachedSr = 0
    private var sampleRateCached = 44_100

    override fun isAudible(): Boolean = partials.isNotEmpty()

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            sampleRateCached = sampleRate
        }
    }

    private fun newPartial(freq: Double, velocity: Float): Partial =
        Partial(
            freq = freq,
            age = 0L,
            env = 0.0,
            peak = velocity,
            // phaseInc/decay left 0 → bound on first render at the active sample rate
        )

    override fun noteOn(freqHz: Float, velocity: Float) {
        for (p in partials) {
            if (!p.releasing) p.releasing = true
        }
        val base = freqHz.toDouble()
        val ratios = doubleArrayOf(1.0, 1.498, 2.0, 2.757, 3.0)
        for (r in ratios) {
            partials += newPartial(base * r, velocity * (0.6f + 0.4f * (1f / r.toFloat())))
        }
        while (partials.size > 10) partials.removeAt(0)
    }

    fun noteCluster(freqs: List<Float>, velocity: Float = 0.7f) {
        for (p in partials) {
            if (!p.releasing) p.releasing = true
        }
        for (f in freqs.take(5)) {
            partials += newPartial(f.toDouble(), velocity * 0.7f)
        }
        while (partials.size > 10) partials.removeAt(0)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (partials.isEmpty()) return 0f
        ensureSr(sampleRate)
        var sum = 0.0
        val iter = partials.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            // Lazily bind SR-dependent coeffs (noteOn may precede first render).
            if (p.phaseInc == 0.0) {
                p.phaseInc = 2.0 * PI * p.freq / sampleRate
                p.decayMul = EnvMath.decayMul(1.6, sampleRate)
                p.releaseMul = EnvMath.decayMul(10.0, sampleRate)
                p.attackSamples = (0.012 * sampleRate).toInt().coerceAtLeast(1)
                p.maxAge = (4.0 * sampleRate).toLong()
            }
            p.age++
            if (p.releasing) {
                p.env *= p.releaseMul
                if (p.env < 1e-4) {
                    iter.remove()
                    continue
                }
            } else if (p.age <= p.attackSamples) {
                p.env = p.peak * p.age.toDouble() / p.attackSamples
            } else {
                p.env *= p.decayMul
                if (p.age > p.maxAge || p.env < 1e-4) {
                    iter.remove()
                    continue
                }
            }
            p.phase = SinLut.wrapPhase(p.phase + p.phaseInc)
            sum += SinLut.sin(p.phase) * p.env * 0.22
        }
        return sum.toFloat()
    }

    override fun noteOff() {
        for (p in partials) p.releasing = true
    }

    override fun reset() {
        partials.clear()
    }
}

class KalimbaPluckVoice : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var freq = 0.0
    private var age = 0L
    private var env = 0.0
    private var peak = 0.0
    private var active = false
    private var releasing = false
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0
    private var attackSamples = 1
    private var decayMul = 0.999
    private var releaseMul = 0.99
    private var maxAge = 0L
    private var phaseInc1 = 0.0
    private var phaseInc2 = 0.0
    private var phaseInc3 = 0.0

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        peak = velocity.coerceIn(0.1f, 1f).toDouble()
        age = 0L
        env = 0.0
        active = true
        releasing = false
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
        cachedSr = 0
    }

    override fun noteOff() {
        if (active && !releasing) releasing = true
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        attackSamples = (0.003 * sampleRate).toInt().coerceAtLeast(1)
        decayMul = EnvMath.decayMul(4.5, sampleRate)
        releaseMul = EnvMath.decayMul(16.0, sampleRate)
        maxAge = (1.8 * sampleRate).toLong()
        phaseInc1 = 2.0 * PI * freq * invSr
        phaseInc2 = 2.0 * PI * freq * 2.01 * invSr
        phaseInc3 = 2.0 * PI * freq * 4.2 * invSr
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        age++
        if (releasing) {
            env *= releaseMul
            if (env < 1e-4) {
                active = false
                return 0f
            }
        } else if (age <= attackSamples) {
            env = peak * age.toDouble() / attackSamples
        } else {
            env *= decayMul
            if (age > maxAge || env < 1e-4) {
                active = false
                return 0f
            }
        }
        phase1 = SinLut.wrapPhase(phase1 + phaseInc1)
        phase2 = SinLut.wrapPhase(phase2 + phaseInc2)
        phase3 = SinLut.wrapPhase(phase3 + phaseInc3)
        val h1 = SinLut.sin(phase1)
        val h2 = SinLut.sin(phase2) * 0.25f
        val h3 = SinLut.sin(phase3) * 0.08f
        return ((h1 + h2 + h3) * 0.5f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
        env = 0.0
    }
}

/**
 * Fast rational soft clip (replaces tanh in the hot path).
 * Smooth knee, hard-bounded to [-1, 1].
 */
fun softLimit(sample: Float): Float {
    val s = sample * 1.15f
    return (s / (1f + abs(s) * 0.35f)).coerceIn(-1f, 1f)
}
