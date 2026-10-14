package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.exp

/**
 * Shared envelope: linear attack to the velocity peak, exponential decay towards
 * [sustain] x peak, exponential release. Rates are per second.
 */
class Adsr {
    private enum class Stage { Off, Attack, Decay, Release }

    private var stage = Stage.Off
    private var env = 0.0
    private var peak = 0.0
    private var age = 0L
    private var attackSamples = 1
    private var attackStep = 0.0
    private var decayMul = 0.999
    private var releaseMul = 0.99
    private var sustainLevel = 0.0
    private var maxAge = Long.MAX_VALUE

    private var attackSec = 0.004
    private var decayRate = 3.0
    private var sustain = 0.0
    private var releaseRate = 14.0
    private var maxSec = 0.0
    private var cachedSr = 0

    fun setup(
        attackSeconds: Double,
        decayRate: Double,
        sustain: Double = 0.0,
        releaseRate: Double = 14.0,
        maxSeconds: Double = 0.0,
    ) {
        attackSec = attackSeconds
        this.decayRate = decayRate
        this.sustain = sustain
        this.releaseRate = releaseRate
        maxSec = maxSeconds
        cachedSr = 0
    }

    val isActive: Boolean get() = stage != Stage.Off
    val isReleasing: Boolean get() = stage == Stage.Release
    val level: Double get() = env
    val ageSamples: Long get() = age

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        attackSamples = (attackSec * sampleRate).toInt().coerceAtLeast(1)
        decayMul = exp(-decayRate / sampleRate)
        releaseMul = exp(-releaseRate / sampleRate)
        maxAge = if (maxSec > 0.0) (maxSec * sampleRate).toLong() else Long.MAX_VALUE
    }

    fun trigger(peakLevel: Double) {
        peak = peakLevel
        sustainLevel = sustain * peakLevel
        env = 0.0
        age = 0L
        stage = Stage.Attack
        cachedSr = 0
    }

    fun release() {
        if (stage != Stage.Off) stage = Stage.Release
    }

    fun reset() {
        stage = Stage.Off
        env = 0.0
    }

    /** Advance one sample and return the envelope value (0 when finished). */
    fun next(sampleRate: Int): Double {
        if (stage == Stage.Off) return 0.0
        ensureSr(sampleRate)
        age++
        when (stage) {
            Stage.Attack -> {
                env = peak * age.toDouble() / attackSamples
                if (age >= attackSamples) stage = Stage.Decay
            }
            Stage.Decay -> {
                env = sustainLevel + (env - sustainLevel) * decayMul
                if (age > maxAge || env < 1e-4) {
                    stage = Stage.Off
                    env = 0.0
                }
            }
            Stage.Release -> {
                env *= releaseMul
                if (env < 1e-4) {
                    stage = Stage.Off
                    env = 0.0
                }
            }
            Stage.Off -> Unit
        }
        return env
    }
}

/** Sine LFO with phase in cycles; output -1..1. */
class Lfo(private var phase: Double = 0.0) {
    fun next(rateHz: Double, sampleRate: Int): Float {
        phase += rateHz / sampleRate
        if (phase >= 1.0) phase -= 1.0
        return SinLut.sin(phase * 2.0 * PI)
    }

    fun reset(startPhase: Double = 0.0) {
        phase = startPhase
    }
}

/**
 * Polyphonic wrapper: allocates a free voice, otherwise releases and reuses the oldest
 * (so overlapping notes keep their tails instead of cutting each other off).
 */
class PolyVoicePool<T : SynthVoice>(
    voiceCount: Int,
    factory: () -> T,
) : SynthVoice {
    private val voices: List<T> = List(voiceCount.coerceAtLeast(1)) { factory() }
    private val started = LongArray(voices.size)
    private var counter = 0L

    override fun isAudible(): Boolean = voices.any { it.isAudible() }

    override fun noteOn(freqHz: Float, velocity: Float) {
        var idx = voices.indexOfFirst { !it.isAudible() }
        if (idx < 0) {
            idx = 0
            for (i in 1 until voices.size) if (started[i] < started[idx]) idx = i
        }
        started[idx] = ++counter
        voices[idx].noteOn(freqHz, velocity)
    }

    override fun noteOff() {
        for (v in voices) v.noteOff()
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        var sum = 0f
        for (v in voices) if (v.isAudible()) sum += v.render(sampleIndex, sampleRate)
        return sum
    }

    override fun reset() {
        for (v in voices) v.reset()
    }
}
