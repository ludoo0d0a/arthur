package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

/**
 * Granular synthesizer voice: produces an evolving cloud of overlapping micro-grains
 * with Hann windowing, pitch jitter, table scanning, and spatial stereo distribution.
 */
class GranularSynthVoice(
    private val maxGrains: Int = 16,
    private val seed: Long = 1337L,
) : SynthVoice {

    private class Grain(
        var active: Boolean = false,
        var age: Int = 0,
        var duration: Int = 1000,
        var phase: Double = 0.0,
        var phaseInc: Double = 0.0,
        var panL: Float = 0.707f,
        var panR: Float = 0.707f,
        var tablePos: Float = 0f,
        var gain: Float = 1f,
    )

    private val grains = Array(maxGrains) { Grain() }
    private val rng = Random(seed)
    private var rootFreq = 220.0
    private var targetFreq = 220.0
    private var active = false
    private var spawnClock = 0
    private var spawnInterval = 800 // samples between grains
    private var cachedSr = 0
    private var invSr = 1.0 / 44_100
    private var fCoeff = 0.0
    private val adsr = Adsr().apply {
        setup(attackSeconds = 0.05, decayRate = 1.2, sustain = 0.6, releaseRate = 4.0, maxSeconds = 15.0)
    }

    var outPanL: Float = 0.707f
        private set
    var outPanR: Float = 0.707f
        private set

    override fun isAudible(): Boolean = active && adsr.isActive

    override fun noteOn(freqHz: Float, velocity: Float) {
        targetFreq = freqHz.toDouble().coerceIn(30.0, 3000.0)
        if (!active) {
            rootFreq = targetFreq
        }
        adsr.trigger(velocity.coerceIn(0.1f, 1f).toDouble())
        active = true
        spawnClock = 0
        cachedSr = 0
    }

    override fun noteOff() {
        if (active) {
            adsr.release()
        }
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        fCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.06))
        spawnInterval = (sampleRate / 25).coerceAtLeast(100) // ~25 grains/sec
    }

    private fun spawnGrain(sampleRate: Int) {
        val free = grains.firstOrNull { !it.active } ?: return
        val grainDurMs = 30.0 + rng.nextDouble() * 90.0 // 30ms..120ms
        val durSamples = (grainDurMs * 0.001 * sampleRate).toInt().coerceAtLeast(64)

        // Pitch jitter: ±12 cents for lush ambient diffusion
        val jitterCents = (rng.nextDouble() * 24.0) - 12.0
        val jitterRatio = 2.0.pow(jitterCents / 1200.0)
        val grainFreq = rootFreq * jitterRatio

        // Random pan per grain
        val panAngle = (rng.nextDouble() * 0.5 * PI).toFloat()
        val panL = kotlin.math.cos(panAngle)
        val panR = kotlin.math.sin(panAngle)

        // Wavetable morph position scatter
        val tablePos = (rng.nextDouble() * 4.0).toFloat()

        free.active = true
        free.age = 0
        free.duration = durSamples
        free.phase = rng.nextDouble() * 2.0 * PI
        free.phaseInc = 2.0 * PI * grainFreq * invSr
        free.panL = panL
        free.panR = panR
        free.tablePos = tablePos
        free.gain = (0.6f + rng.nextFloat() * 0.4f)
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        val env = adsr.next(sampleRate)
        if (!adsr.isActive) {
            active = false
            return 0f
        }
        rootFreq += (targetFreq - rootFreq) * fCoeff

        spawnClock++
        if (spawnClock >= spawnInterval) {
            spawnClock = 0
            spawnGrain(sampleRate)
        }

        var sumL = 0f
        var sumR = 0f

        for (g in grains) {
            if (!g.active) continue
            g.age++
            if (g.age >= g.duration) {
                g.active = false
                continue
            }

            // Hann window
            val w = 0.5f * (1f - cos((2.0 * PI * g.age / g.duration)).toFloat())

            g.phase = SinLut.wrapPhase(g.phase + g.phaseInc)
            val sample = WavetableSynthVoice.sampleMorphed(g.tablePos, g.phase)

            val grainVal = sample * w * g.gain
            sumL += grainVal * g.panL
            sumR += grainVal * g.panR
        }

        val scaledEnv = (0.35f * env).toFloat()
        outPanL = sumL * scaledEnv
        outPanR = sumR * scaledEnv

        val monoSum = (sumL + sumR) * 0.5f
        return monoSum * scaledEnv
    }

    override fun reset() {
        active = false
        adsr.reset()
        for (g in grains) g.active = false
    }
}
