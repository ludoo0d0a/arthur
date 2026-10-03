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
    fun noteOff() {}
    fun reset() {}
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
        // Used by engine to stretch Atmosphere chord glides.
        // Stored via mutating a field — see glideSecondsOverride.
        glideSecondsOverride = seconds.coerceIn(0.02f, 0.5f)
    }

    private var glideSecondsOverride: Float = glideSeconds

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        val glide = glideSecondsOverride.coerceAtLeast(0.02f)
        val fCoeff = 1.0 - exp(-1.0 / (sampleRate * glide))
        f1 += (t1 - f1) * fCoeff
        f2 += (t2 - f2) * fCoeff
        f3 += (t3 - f3) * fCoeff
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

    override fun noteOff() {
        targetGain = 0.0
    }

    override fun reset() {
        currentGain = 0.0
        targetGain = 0.0
    }
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

    val isActive: Boolean get() = active

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

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        // Glide toward new chord root (~80 ms).
        val gCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.08))
        freq += (targetFreq - freq) * gCoeff
        val t = age.toDouble() / sampleRate
        age++
        val attack = (t / 0.015).coerceAtMost(1.0)
        var env = attack * vel * 0.55
        if (releasing) {
            val rt = releaseAge.toDouble() / sampleRate
            releaseAge++
            env *= exp(-8.0 * rt).toFloat()
            if (rt > 0.35) {
                active = false
                return 0f
            }
        } else if (t > 4.5) {
            // Soft auto-release if no new note.
            env *= exp(-1.2 * (t - 4.0)).toFloat()
            if (t > 6.0) {
                active = false
                return 0f
            }
        }
        phase += 2.0 * PI * freq / sampleRate
        phase2 += 2.0 * PI * freq * 2.0 / sampleRate
        if (phase > 2.0 * PI) phase -= 2.0 * PI
        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
        val body = sin(phase) + 0.18 * sin(phase2)
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

    val isActive: Boolean get() = active
    val ageSamples: Long get() = age

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

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        val t = age.toDouble() / sampleRate
        age++
        val attack = (t / 0.006).coerceAtMost(1.0)
        var env = attack * exp(-3.2 * t) * vel
        if (releasing) {
            val rt = releaseAge.toDouble() / sampleRate
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
        phase1 += 2.0 * PI * freq / sampleRate
        phase2 += 2.0 * PI * freq * 2.002 / sampleRate
        phase3 += 2.0 * PI * freq * 3.004 / sampleRate
        phase4 += 2.0 * PI * freq * 4.006 / sampleRate
        val h1 = sin(phase1)
        val h2 = sin(phase2) * 0.32
        val h3 = sin(phase3) * 0.14
        val h4 = sin(phase4) * 0.06
        return ((h1 + h2 + h3 + h4) * 0.48 * env).toFloat()
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

    override fun noteOn(freqHz: Float, velocity: Float) {
        val free = voices.firstOrNull { !it.isActive }
        val target = free ?: voices.maxByOrNull { it.ageSamples } ?: voices[0]
        if (target.isActive) target.noteOff()
        // If still active (releasing), steal after a soft cut next sample — re-trigger immediately
        // but noteOff already started a short release; overwrite is ok for pool size limits.
        target.noteOn(freqHz, velocity)
    }

    override fun noteOff() {
        for (v in voices) v.noteOff()
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
        val t = age.toDouble() / sampleRate
        age++
        if (!releasing && t > 2.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.004).coerceAtMost(1.0)
        var env = attack * exp(-3.0 * t) * vel
        if (releasing) {
            val rt = releaseAge.toDouble() / sampleRate
            releaseAge++
            env *= exp(-14.0 * rt)
            if (rt > 0.06) {
                active = false
                return 0f
            }
        }
        // Soft windowed noise burst (avoids crackle).
        val burst = if (t < 0.008) {
            val w = (1.0 - t / 0.008)
            (noise.nextFloat() * 2f - 1f) * 0.18f * w.toFloat()
        } else {
            0f
        }
        phase1 += 2.0 * PI * freq / sampleRate
        phase2 += 2.0 * PI * freq * 2.0 / sampleRate
        phase3 += 2.0 * PI * freq * 3.0 / sampleRate
        val h1 = sin(phase1)
        val h2 = sin(phase2) * 0.3
        val h3 = sin(phase3) * 0.12
        return (burst + ((h1 + h2 + h3) * 0.4 * env)).toFloat()
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
        val t = age.toDouble() / sampleRate
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
            phase1 += 2.0 * PI * fA / sampleRate
            phase2 += 2.0 * PI * fB / sampleRate
            phase3 += 2.0 * PI * fC / sampleRate
            val s = sin(phase1) + 0.4 * sin(phase2) + 0.16 * sin(phase3)
            (s * 0.30 * env).toFloat()
        } else {
            phase1 += 2.0 * PI * freq / sampleRate
            phase2 += 2.0 * PI * freq * 2.76 / sampleRate
            phase3 += 2.0 * PI * freq * 5.40 / sampleRate
            val h1 = sin(phase1)
            val h2 = sin(phase2) * 0.45
            val h3 = sin(phase3) * 0.18
            ((h1 + h2 + h3) * 0.32 * env).toFloat()
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

    fun setRootHz(hz: Float) {
        rootHz = hz.coerceIn(40f, 400f)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        val t = sampleIndex.toDouble() / sampleRate
        val swell = 0.5 + 0.5 * sin(2.0 * PI * (0.05 + 0.02 * sin(0.01 * t)) * t)
        val n = rng.nextFloat() * 2.0 - 1.0
        // Cutoff tracks root: lower roots → darker ocean.
        val cutoff = (0.04 + (rootHz / 400.0) * 0.08).coerceIn(0.03, 0.14)
        lp += cutoff * (n - lp)
        val tone = 0.85 + 0.15 * sin(2.0 * PI * (rootHz / 8.0) * t * 0.01)
        return (lp * 0.26 * swell * tone).toFloat()
    }
}

class WindTextureVoice(
    private val seed: Long = 7L,
) : SynthVoice {
    private val rng = Random(seed)
    private var hp = 0.0
    private var prev = 0.0
    private var rootHz = 220f

    fun setRootHz(hz: Float) {
        rootHz = hz.coerceIn(40f, 500f)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        val t = sampleIndex.toDouble() / sampleRate
        val gust = (0.3 + 0.7 * ((sin(2.0 * PI * 0.07 * t) + 1.0) * 0.5)).coerceIn(0.0, 1.0)
        val n = rng.nextFloat() * 2.0 - 1.0
        val high = n - prev
        prev = n
        val bright = (0.08 + (rootHz / 500.0) * 0.12).coerceIn(0.06, 0.22)
        hp = (1.0 - bright) * hp + bright * high
        val tone = 0.9 + 0.1 * sin(2.0 * PI * rootHz * 0.002 * t)
        return (hp * 0.18 * gust * tone).toFloat()
    }
}

class SoftPulseVoice : SynthVoice {
    private var age = 0L
    private var active = false
    private var vel = 0f
    private var phase = 0.0

    override fun noteOn(freqHz: Float, velocity: Float) {
        vel = velocity.coerceIn(0.05f, 1f)
        age = 0L
        active = true
        phase = 0.0
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
        phase += 2.0 * PI * 90.0 / sampleRate
        val body = sin(phase)
        return (body * 0.35 * env).toFloat()
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

    override fun noteOn(freqHz: Float, velocity: Float) {
        // Soft-release existing partials instead of hard clear.
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
        var sum = 0.0
        val iter = partials.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val t = p.age.toDouble() / sampleRate
            p.age++
            var env = (t / 0.012).coerceAtMost(1.0) * exp(-1.6 * t) * p.vel
            if (p.releasing) {
                val rt = p.releaseAge.toDouble() / sampleRate
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
            p.phase += 2.0 * PI * p.freq / sampleRate
            sum += sin(p.phase) * env * 0.22
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
        val t = age.toDouble() / sampleRate
        age++
        if (!releasing && t > 1.8) {
            active = false
            return 0f
        }
        val attack = (t / 0.003).coerceAtMost(1.0)
        var env = attack * exp(-4.5 * t) * vel
        if (releasing) {
            val rt = releaseAge.toDouble() / sampleRate
            releaseAge++
            env *= exp(-16.0 * rt)
            if (rt > 0.05) {
                active = false
                return 0f
            }
        }
        phase1 += 2.0 * PI * freq / sampleRate
        phase2 += 2.0 * PI * freq * 2.01 / sampleRate
        phase3 += 2.0 * PI * freq * 4.2 / sampleRate
        val h1 = sin(phase1)
        val h2 = sin(phase2) * 0.25
        val h3 = sin(phase3) * 0.08
        return ((h1 + h2 + h3) * 0.5 * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
    }
}

/** Soft limiter for summed stems. */
fun softLimit(sample: Float): Float = tanh(sample * 1.15).toFloat()
