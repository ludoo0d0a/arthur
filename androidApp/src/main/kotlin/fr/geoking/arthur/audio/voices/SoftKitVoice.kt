package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.exp
import kotlin.random.Random

/**
 * Very soft brush kit for lounge styles: shaker/hat tick, brush swish and a felt kick.
 * Everything is short, filtered and quiet so it supports the groove without drawing attention.
 */
class SoftKitVoice : SynthVoice {
    private val noise = Random(7)
    private var cachedSr = 0

    // Hat / swish: high-passed noise with exponential decay.
    private var noiseEnv = 0.0
    private var hatDecayMul = 0.99
    private var swishDecayMul = 0.999
    private var hpLp = 0f

    // Kick: sine with falling pitch.
    private var kickEnv = 0.0
    private var kickPhase = 0.0
    private var kickFreq = 0.0
    private var kickDecayMul = 0.999
    private var kickPitchMul = 0.9995

    override fun isAudible(): Boolean = noiseEnv > 1e-4 || kickEnv > 1e-4

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        hatDecayMul = exp(-60.0 / sampleRate)
        swishDecayMul = exp(-14.0 / sampleRate)
        kickDecayMul = exp(-18.0 / sampleRate)
        kickPitchMul = exp(-22.0 / sampleRate)
    }

    fun hat(velocity: Float) {
        noiseEnv = velocity.coerceIn(0f, 1f).toDouble()
        useSwish = false
    }

    fun swish(velocity: Float) {
        noiseEnv = velocity.coerceIn(0f, 1f).toDouble()
        useSwish = true
    }

    fun kick(velocity: Float) {
        kickEnv = velocity.coerceIn(0f, 1f).toDouble()
        kickFreq = 78.0
        kickPhase = 0.0
    }

    private var useSwish = false

    override fun noteOn(freqHz: Float, velocity: Float) = hat(velocity)

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!isAudible()) return 0f
        ensureSr(sampleRate)
        var out = 0f
        if (noiseEnv > 1e-4) {
            val n = noise.nextFloat() * 2f - 1f
            // One-pole high-pass: keeps the "tsss", drops the thump.
            hpLp += 0.25f * (n - hpLp)
            val hp = n - hpLp
            out += hp * noiseEnv.toFloat() * if (useSwish) 0.5f else 0.35f
            noiseEnv *= if (useSwish) swishDecayMul else hatDecayMul
        }
        if (kickEnv > 1e-4) {
            kickPhase = SinLut.wrapPhase(kickPhase + 2.0 * PI * kickFreq / sampleRate)
            out += SinLut.sin(kickPhase) * kickEnv.toFloat() * 0.6f
            kickEnv *= kickDecayMul
            kickFreq = 44.0 + (kickFreq - 44.0) * kickPitchMul
        }
        return out
    }

    override fun reset() {
        noiseEnv = 0.0
        kickEnv = 0.0
    }
}
