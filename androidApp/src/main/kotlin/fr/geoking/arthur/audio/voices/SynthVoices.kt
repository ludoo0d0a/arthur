package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.tanh
import kotlin.random.Random

/** Minimal voice interface — renders one mono sample; optional block fill. */
interface SynthVoice {
    fun render(sampleIndex: Long, sampleRate: Int): Float
    /** Add [frames] samples into [out] starting at [offset], scaled by [gain]. */
    fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float = 1f) {
        if (gain == 0f || frames <= 0) return
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
        cachedSr = 0 // force coeff refresh
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
        if (frames <= 0 || (currentGain < 1e-5 && targetGain < 1e-5 && gain == 0f)) return
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

/** Soft sine bass with slow attack; phase-accumulated oscillator. */
class SoftBassVoice : SynthVoice {
    private var phase = 0.0
    private var phase2 = 0.0
    private var freq = 55.0
    private var targetFreq = 55.0
    private var age = 0L
    private var vel = 0f
    private var active = false
    private var releasing = false
    private var releaseAge = 0L
    private var cachedSr = 0
    private var gCoeff = 0.0
    private var invSr = 0.0

    val isActive: Boolean get() = active

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        targetFreq = freqHz.toDouble().coerceIn(40.0, 140.0)
        if (!active) {
            freq = targetFreq
            phase = 0.0
            phase2 = 0.0
        }
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
        releasing = false
        releaseAge = 0L
    }

    override fun noteOff() {
        if (active) {
            releasing = true
            releaseAge = 0L
        }
    }

    private fun ensureCoeffs(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        gCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.08))
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureCoeffs(sampleRate)
        freq += (targetFreq - freq) * gCoeff
        val t = age.toDouble() * invSr
        age++
        val attack = (t / 0.015).coerceAtMost(1.0)
        var env = attack * vel * 0.55
        if (releasing) {
            val rt = releaseAge.toDouble() * invSr
            releaseAge++
            env *= exp(-8.0 * rt)
            if (rt > 0.35) {
                active = false
                return 0f
            }
        } else if (t > 4.5) {
            env *= exp(-1.2 * (t - 4.0))
            if (t > 6.0) {
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
    }
}

/** Single piano voice with phase-accumulated harmonic series and release. */
class SoftPianoVoice : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var phase4 = 0.0
    private var freq = 0.0
    private var age = 0L
    private var vel = 0f
    private var active = false
    private var releasing = false
    private var releaseAge = 0L
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0

    val isActive: Boolean get() = active
    val ageSamples: Long get() = age

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
        releasing = false
        releaseAge = 0L
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
        phase4 = 0.0
    }

    override fun noteOff() {
        if (active && !releasing) {
            releasing = true
            releaseAge = 0L
        }
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        val t = age.toDouble() * invSr
        age++
        val attack = (t / 0.006).coerceAtMost(1.0)
        var env = attack * exp(-3.2 * t) * vel
        if (releasing) {
            val rt = releaseAge.toDouble() * invSr
            releaseAge++
            env *= exp(-12.0 * rt)
            if (rt > 0.08) {
                active = false
                return 0f
            }
        } else if (t > 2.8) {
            active = false
            return 0f
        }
        phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * freq * invSr)
        phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * freq * 2.002 * invSr)
        phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * freq * 3.004 * invSr)
        phase4 = SinLut.wrapPhase(phase4 + 2.0 * PI * freq * 4.006 * invSr)
        val h1 = SinLut.sin(phase1)
        val h2 = SinLut.sin(phase2) * 0.32f
        val h3 = SinLut.sin(phase3) * 0.14f
        val h4 = SinLut.sin(phase4) * 0.06f
        return ((h1 + h2 + h3 + h4) * 0.48f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
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
    private var vel = 0f
    private var active = false
    private var releasing = false
    private var releaseAge = 0L
    private var noise = Random(1)
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
        releasing = false
        releaseAge = 0L
        noise = Random((freqHz * 100).toInt())
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
    }

    override fun noteOff() {
        if (active && !releasing) {
            releasing = true
            releaseAge = 0L
        }
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        val t = age.toDouble() * invSr
        age++
        if (!releasing && t > 2.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.004).coerceAtMost(1.0)
        var env = attack * exp(-3.0 * t) * vel
        if (releasing) {
            val rt = releaseAge.toDouble() * invSr
            releaseAge++
            env *= exp(-14.0 * rt)
            if (rt > 0.06) {
                active = false
                return 0f
            }
        }
        val burst = if (t < 0.008) {
            val w = (1.0 - t / 0.008)
            (noise.nextFloat() * 2f - 1f) * 0.18f * w.toFloat()
        } else {
            0f
        }
        phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * freq * invSr)
        phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * freq * 2.0 * invSr)
        phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * freq * 3.0 * invSr)
        val h1 = SinLut.sin(phase1)
        val h2 = SinLut.sin(phase2) * 0.3f
        val h3 = SinLut.sin(phase3) * 0.12f
        return burst + ((h1 + h2 + h3) * 0.4f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
    }
}

class BowlVoice : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var freq = 0.0
    private var age = 0L
    private var vel = 0f
    private var active = false
    private var sustain = false
    private var sustainPartials: List<Double> = emptyList()
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0

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
            vel = 0.32f
            phase1 = 0.0
            phase2 = 0.0
            phase3 = 0.0
        } else if (!on) {
            sustain = false
        }
    }

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.08f, 0.85f)
        age = 0L
        active = true
        sustain = false
        sustainPartials = emptyList()
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        val t = age.toDouble() * invSr
        age++
        if (!sustain && t > 6.0) {
            active = false
            return 0f
        }
        val attack = (t / 0.03).coerceAtMost(1.0)
        val decay = if (sustain) 0.42 else exp(-0.7 * t)
        val env = attack * decay * vel
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

    fun setRootHz(hz: Float) {
        rootHz = hz.coerceIn(40f, 400f)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        phase = SinLut.wrapPhase(phase + 2.0 * PI * (0.05) * invSr)
        val swell = 0.5 + 0.5 * SinLut.sin(phase + 0.3 * SinLut.sin(phase * 0.4))
        val n = rng.nextFloat() * 2.0 - 1.0
        val cutoff = (0.04 + (rootHz / 400.0) * 0.08).coerceIn(0.03, 0.14)
        lp += cutoff * (n - lp)
        val tone = 0.85 + 0.15 * SinLut.sin(phase * (rootHz / 40.0) * 0.01)
        return (lp * 0.26 * swell * tone).toFloat()
    }

    override fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float) {
        if (frames <= 0 || gain == 0f) return
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        val end = offset + frames
        var i = offset
        while (i < end) {
            phase = SinLut.wrapPhase(phase + 2.0 * PI * 0.05 * invSr)
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

    fun setRootHz(hz: Float) {
        rootHz = hz.coerceIn(40f, 500f)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        phase = SinLut.wrapPhase(phase + 2.0 * PI * 0.07 * invSr)
        val gust = (0.3 + 0.7 * ((SinLut.sin(phase) + 1.0) * 0.5)).coerceIn(0.0, 1.0)
        val n = rng.nextFloat() * 2.0 - 1.0
        val high = n - prev
        prev = n
        val bright = (0.08 + (rootHz / 500.0) * 0.12).coerceIn(0.06, 0.22)
        hp = (1.0 - bright) * hp + bright * high
        val tone = 0.9 + 0.1 * SinLut.sin(phase * rootHz * 0.002 * invSr * sampleRate)
        return (hp * 0.18 * gust * tone).toFloat()
    }

    override fun renderInto(out: FloatArray, offset: Int, frames: Int, sampleRate: Int, gain: Float) {
        if (frames <= 0 || gain == 0f) return
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        val end = offset + frames
        var i = offset
        while (i < end) {
            phase = SinLut.wrapPhase(phase + 2.0 * PI * 0.07 * invSr)
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
    private var vel = 0f
    private var phase = 0.0
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        vel = velocity.coerceIn(0.05f, 1f)
        age = 0L
        active = true
        phase = 0.0
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        val t = age.toDouble() * invSr
        age++
        if (t > 0.12) {
            active = false
            return 0f
        }
        val env = exp(-40.0 * t) * vel
        phase = SinLut.wrapPhase(phase + 2.0 * PI * 90.0 * invSr)
        return (SinLut.sin(phase) * 0.35f * env).toFloat()
    }

    override fun reset() {
        active = false
    }
}

class ChimeClusterVoice : SynthVoice {
    private data class Partial(
        var freq: Double,
        var age: Long,
        var vel: Float,
        var releasing: Boolean = false,
        var releaseAge: Long = 0L,
        var phase: Double = 0.0,
    )

    private val partials = ArrayList<Partial>(8)
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0

    override fun isAudible(): Boolean = partials.isNotEmpty()

    override fun noteOn(freqHz: Float, velocity: Float) {
        for (p in partials) {
            if (!p.releasing) {
                p.releasing = true
                p.releaseAge = 0L
            }
        }
        val base = freqHz.toDouble()
        val ratios = doubleArrayOf(1.0, 1.498, 2.0, 2.757, 3.0)
        for (r in ratios) {
            partials += Partial(base * r, 0L, velocity * (0.6f + 0.4f * (1f / r.toFloat())))
        }
        while (partials.size > 10) {
            partials.removeAt(0)
        }
    }

    fun noteCluster(freqs: List<Float>, velocity: Float = 0.7f) {
        for (p in partials) {
            if (!p.releasing) {
                p.releasing = true
                p.releaseAge = 0L
            }
        }
        for (f in freqs.take(5)) {
            partials += Partial(f.toDouble(), 0L, velocity * 0.7f)
        }
        while (partials.size > 10) {
            partials.removeAt(0)
        }
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (partials.isEmpty()) return 0f
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        var sum = 0.0
        val iter = partials.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val t = p.age.toDouble() * invSr
            p.age++
            var env = (t / 0.012).coerceAtMost(1.0) * exp(-1.6 * t) * p.vel
            if (p.releasing) {
                val rt = p.releaseAge.toDouble() * invSr
                p.releaseAge++
                env *= exp(-10.0 * rt)
                if (rt > 0.12) {
                    iter.remove()
                    continue
                }
            } else if (t > 4.0) {
                iter.remove()
                continue
            }
            p.phase = SinLut.wrapPhase(p.phase + 2.0 * PI * p.freq * invSr)
            sum += SinLut.sin(p.phase) * env * 0.22
        }
        return sum.toFloat()
    }

    override fun noteOff() {
        for (p in partials) {
            p.releasing = true
            p.releaseAge = 0L
        }
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
    private var vel = 0f
    private var active = false
    private var releasing = false
    private var releaseAge = 0L
    private var invSr = 1.0 / 44_100
    private var cachedSr = 0

    override fun isAudible(): Boolean = active

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
        releasing = false
        releaseAge = 0L
        phase1 = 0.0
        phase2 = 0.0
        phase3 = 0.0
    }

    override fun noteOff() {
        if (active && !releasing) {
            releasing = true
            releaseAge = 0L
        }
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        if (sampleRate != cachedSr) {
            cachedSr = sampleRate
            invSr = 1.0 / sampleRate
        }
        val t = age.toDouble() * invSr
        age++
        if (!releasing && t > 1.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.003).coerceAtMost(1.0)
        var env = attack * exp(-4.5 * t) * vel
        if (releasing) {
            val rt = releaseAge.toDouble() * invSr
            releaseAge++
            env *= exp(-16.0 * rt)
            if (rt > 0.05) {
                active = false
                return 0f
            }
        }
        phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * freq * invSr)
        phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * freq * 2.01 * invSr)
        phase3 = SinLut.wrapPhase(phase3 + 2.0 * PI * freq * 4.2 * invSr)
        val h1 = SinLut.sin(phase1)
        val h2 = SinLut.sin(phase2) * 0.25f
        val h3 = SinLut.sin(phase3) * 0.08f
        return ((h1 + h2 + h3) * 0.5f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
    }
}

/** Soft limiter for summed stems. */
fun softLimit(sample: Float): Float = tanh(sample * 1.15).toFloat()
