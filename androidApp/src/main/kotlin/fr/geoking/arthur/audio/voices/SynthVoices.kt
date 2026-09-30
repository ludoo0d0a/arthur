package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/** Minimal voice interface — renders one mono sample given absolute sample index. */
interface SynthVoice {
    fun render(sampleIndex: Long, sampleRate: Int): Float
    fun noteOn(freqHz: Float, velocity: Float = 1f) {}
    fun reset() {}
}

/**
 * Soft chord pad: low gain, tiny detune, no amplitude LFO throb.
 * Chord changes are gain-smoothed to avoid clicks.
 */
class SinePadVoice(
    private val detuneCents: Float = 2.5f,
) : SynthVoice {
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phase3 = 0.0
    private var f1 = 220.0
    private var f2 = 330.0
    private var f3 = 440.0
    private var targetGain = 0.12
    private var currentGain = 0.0

    fun setChord(freqs: List<Float>) {
        if (freqs.isEmpty()) return
        f1 = freqs[0].toDouble()
        f2 = freqs.getOrElse(1) { freqs[0] * 1.5f }.toDouble()
        f3 = freqs.getOrElse(2) { freqs[0] * 2f }.toDouble()
        val det = 2.0.pow(detuneCents / 1200.0)
        f2 *= det
        f3 /= det
        targetGain = 0.12
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        // ~30 ms gain smoothing
        val coeff = 1.0 - exp(-1.0 / (sampleRate * 0.03))
        currentGain += (targetGain - currentGain) * coeff
        phase1 += 2.0 * PI * f1 / sampleRate
        phase2 += 2.0 * PI * f2 / sampleRate
        phase3 += 2.0 * PI * f3 / sampleRate
        wrap()
        val s = sin(phase1) + 0.4 * sin(phase2) + 0.22 * sin(phase3)
        return (s * currentGain).toFloat()
    }

    private fun wrap() {
        val twoPi = 2.0 * PI
        if (phase1 > twoPi) phase1 -= twoPi
        if (phase2 > twoPi) phase2 -= twoPi
        if (phase3 > twoPi) phase3 -= twoPi
    }

    override fun reset() {
        currentGain = 0.0
        targetGain = 0.0
    }
}

/** Single piano voice with true-ish harmonic series and clear decay. */
class SoftPianoVoice : SynthVoice {
    private var freq = 0.0
    private var age = 0L
    private var vel = 0f
    private var active = false

    val isActive: Boolean get() = active
    val ageSamples: Long get() = age

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        val t = age.toDouble() / sampleRate
        age++
        if (t > 2.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.004).coerceAtMost(1.0)
        val decay = exp(-3.2 * t)
        val env = attack * decay * vel
        // Near-harmonic series with tiny stretch (not 2.01 beating)
        val h1 = sin(2.0 * PI * freq * t)
        val h2 = sin(2.0 * PI * freq * 2.002 * t) * 0.32
        val h3 = sin(2.0 * PI * freq * 3.004 * t) * 0.14
        val h4 = sin(2.0 * PI * freq * 4.006 * t) * 0.06
        return ((h1 + h2 + h3 + h4) * 0.48 * env).toFloat()
    }

    override fun reset() {
        active = false
    }
}

/** 4-voice piano pool with simple oldest-voice stealing. */
class SoftPianoPool(
    voiceCount: Int = 4,
) : SynthVoice {
    private val voices = Array(voiceCount.coerceAtLeast(1)) { SoftPianoVoice() }

    override fun noteOn(freqHz: Float, velocity: Float) {
        val free = voices.firstOrNull { !it.isActive }
        val target = free ?: voices.maxByOrNull { it.ageSamples } ?: voices[0]
        target.noteOn(freqHz, velocity)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        var sum = 0f
        for (v in voices) {
            sum += v.render(sampleIndex, sampleRate)
        }
        return sum
    }

    override fun reset() {
        for (v in voices) v.reset()
    }
}

class PluckGuitarVoice : SynthVoice {
    private var freq = 0.0
    private var age = 0L
    private var vel = 0f
    private var active = false
    private var noise = Random(1)

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
        noise = Random((freqHz * 100).toInt())
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        val t = age.toDouble() / sampleRate
        age++
        if (t > 2.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.003).coerceAtMost(1.0)
        val decay = exp(-3.0 * t)
        val env = attack * decay * vel
        val burst = if (t < 0.01) (noise.nextFloat() * 2f - 1f) * 0.4f else 0f
        val h1 = sin(2.0 * PI * freq * t)
        val h2 = sin(2.0 * PI * freq * 2.0 * t) * 0.3
        val h3 = sin(2.0 * PI * freq * 3.0 * t) * 0.12
        return (burst + ((h1 + h2 + h3) * 0.4 * env)).toFloat()
    }

    override fun reset() {
        active = false
    }
}

class BowlVoice : SynthVoice {
    private var freq = 0.0
    private var age = 0L
    private var vel = 0f
    private var active = false
    private var sustain = false

    fun setSustain(freqHz: Float, on: Boolean) {
        sustain = on
        if (on) {
            freq = freqHz.toDouble()
            active = true
            age = 0L
            vel = 0.35f
        }
    }

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
        sustain = false
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        val t = age.toDouble() / sampleRate
        age++
        if (!sustain && t > 6.0) {
            active = false
            return 0f
        }
        val attack = (t / 0.02).coerceAtMost(1.0)
        val decay = if (sustain) 0.45 else exp(-0.7 * t)
        val env = attack * decay * vel
        val h1 = sin(2.0 * PI * freq * t)
        val h2 = sin(2.0 * PI * freq * 2.76 * t) * 0.45
        val h3 = sin(2.0 * PI * freq * 5.40 * t) * 0.18
        return ((h1 + h2 + h3) * 0.32 * env).toFloat()
    }

    override fun reset() {
        active = false
        sustain = false
    }
}

class WaveNoiseVoice(
    private val seed: Long = 42L,
) : SynthVoice {
    private val rng = Random(seed)
    private var lp = 0.0

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        val t = sampleIndex.toDouble() / sampleRate
        val swell = 0.5 + 0.5 * sin(2.0 * PI * (0.05 + 0.02 * sin(0.01 * t)) * t)
        val n = rng.nextFloat() * 2.0 - 1.0
        lp += 0.08 * (n - lp)
        return (lp * 0.28 * swell).toFloat()
    }
}

class WindTextureVoice(
    private val seed: Long = 7L,
) : SynthVoice {
    private val rng = Random(seed)
    private var hp = 0.0
    private var prev = 0.0

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        val t = sampleIndex.toDouble() / sampleRate
        val gust = (0.3 + 0.7 * ((sin(2.0 * PI * 0.07 * t) + 1.0) * 0.5)).coerceIn(0.0, 1.0)
        val n = rng.nextFloat() * 2.0 - 1.0
        val high = n - prev
        prev = n
        hp = 0.9 * hp + 0.1 * high
        return (hp * 0.2 * gust).toFloat()
    }
}

class SoftPulseVoice : SynthVoice {
    private var age = 0L
    private var active = false
    private var vel = 0f

    override fun noteOn(freqHz: Float, velocity: Float) {
        vel = velocity.coerceIn(0.05f, 1f)
        age = 0L
        active = true
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        val t = age.toDouble() / sampleRate
        age++
        if (t > 0.12) {
            active = false
            return 0f
        }
        val env = exp(-40.0 * t) * vel
        val body = sin(2.0 * PI * 90.0 * t)
        return (body * 0.35 * env).toFloat()
    }

    override fun reset() {
        active = false
    }
}

class ChimeClusterVoice : SynthVoice {
    private data class Partial(var freq: Double, var age: Long, var vel: Float)

    private val partials = ArrayList<Partial>(6)

    override fun noteOn(freqHz: Float, velocity: Float) {
        val base = freqHz.toDouble()
        partials.clear()
        val ratios = doubleArrayOf(1.0, 1.498, 2.0, 2.757, 3.0)
        for (r in ratios) {
            partials += Partial(base * r, 0L, velocity * (0.6f + 0.4f * (1f / r.toFloat())))
        }
    }

    fun noteCluster(freqs: List<Float>, velocity: Float = 0.7f) {
        partials.clear()
        for (f in freqs.take(5)) {
            partials += Partial(f.toDouble(), 0L, velocity)
        }
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (partials.isEmpty()) return 0f
        var sum = 0.0
        val iter = partials.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val t = p.age.toDouble() / sampleRate
            p.age++
            if (t > 4.0) {
                iter.remove()
                continue
            }
            val attack = (t / 0.01).coerceAtMost(1.0)
            val decay = exp(-1.6 * t)
            sum += sin(2.0 * PI * p.freq * t) * attack * decay * p.vel * 0.25
        }
        return sum.toFloat()
    }

    override fun reset() {
        partials.clear()
    }
}

class KalimbaPluckVoice : SynthVoice {
    private var freq = 0.0
    private var age = 0L
    private var vel = 0f
    private var active = false

    override fun noteOn(freqHz: Float, velocity: Float) {
        freq = freqHz.toDouble()
        vel = velocity.coerceIn(0.1f, 1f)
        age = 0L
        active = true
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        val t = age.toDouble() / sampleRate
        age++
        if (t > 1.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.002).coerceAtMost(1.0)
        val decay = exp(-4.5 * t)
        val env = attack * decay * vel
        val h1 = sin(2.0 * PI * freq * t)
        val h2 = sin(2.0 * PI * freq * 2.01 * t) * 0.25
        val h3 = sin(2.0 * PI * freq * 4.2 * t) * 0.08
        return ((h1 + h2 + h3) * 0.5 * env).toFloat()
    }

    override fun reset() {
        active = false
    }
}

/** Soft limiter for summed stems. */
fun softLimit(sample: Float): Float = tanh(sample * 1.15).toFloat()
