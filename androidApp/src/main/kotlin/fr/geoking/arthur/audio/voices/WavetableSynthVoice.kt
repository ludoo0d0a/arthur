package fr.geoking.arthur.audio.voices

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Multi-frame wavetable synthesis oscillator.
 * Morphing table position (0..4) smoothly interpolates between 5 distinct wave shapes:
 * 0 = Sine
 * 1 = Triangle
 * 2 = Sawtooth
 * 3 = Square / Pulse
 * 4 = Complex Digital Harmonic Formant
 */
class WavetableSynthVoice(
    private val detuneCents: Float = 3f,
) : SynthVoice {
    companion object {
        const val TABLE_SIZE = 1024
        private const val MASK = TABLE_SIZE - 1
        private const val NUM_TABLES = 5

        /** Precalculated wavetables for rapid lookup and linear interpolation. */
        private val wavetables: Array<FloatArray> = Array(NUM_TABLES) { tableIdx ->
            FloatArray(TABLE_SIZE) { i ->
                val phase = (2.0 * PI * i / TABLE_SIZE)
                when (tableIdx) {
                    0 -> sin(phase).toFloat()
                    1 -> {
                        // Triangle
                        val norm = i.toFloat() / TABLE_SIZE
                        (2f * abs(2f * (norm - kotlin.math.floor(norm + 0.5f))) - 1f)
                    }
                    2 -> {
                        // Sawtooth (bandlimited approximation via first 8 partials)
                        var sum = 0f
                        for (h in 1..8) {
                            sum += (sin(h * phase) / h).toFloat()
                        }
                        (sum * (2f / PI.toFloat())).coerceIn(-1f, 1f)
                    }
                    3 -> {
                        // Square (bandlimited approximation via first 7 odd partials)
                        var sum = 0f
                        for (h in 1..13 step 2) {
                            sum += (sin(h * phase) / h).toFloat()
                        }
                        (sum * (4f / PI.toFloat())).coerceIn(-1f, 1f)
                    }
                    else -> {
                        // Complex digital formant / glass wave
                        val s1 = sin(phase)
                        val s2 = 0.5 * sin(2 * phase + 0.5)
                        val s3 = 0.25 * sin(5 * phase)
                        val s4 = 0.15 * sin(9 * phase)
                        ((s1 + s2 + s3 + s4) * 0.55).toFloat().coerceIn(-1f, 1f)
                    }
                }
            }
        }

        /** Sample raw wavetable with linear interpolation. */
        @JvmStatic
        fun sampleTable(tableIdx: Int, phase: Double): Float {
            val normalized = (phase % (2.0 * PI) + (2.0 * PI)) % (2.0 * PI)
            val idx = normalized * (TABLE_SIZE / (2.0 * PI))
            val i0 = idx.toInt()
            val frac = (idx - i0).toFloat()
            val table = wavetables[tableIdx.coerceIn(0, NUM_TABLES - 1)]
            val a = table[i0 and MASK]
            val b = table[(i0 + 1) and MASK]
            return a + (b - a) * frac
        }

        /** Sample morphed position (0.0 to 4.0) with continuous crossfading. */
        @JvmStatic
        fun sampleMorphed(position: Float, phase: Double): Float {
            val posClamped = position.coerceIn(0f, (NUM_TABLES - 1).toFloat())
            val t0 = posClamped.toInt()
            val t1 = (t0 + 1).coerceAtMost(NUM_TABLES - 1)
            val frac = posClamped - t0
            if (t0 == t1 || frac <= 0.001f) {
                return sampleTable(t0, phase)
            }
            val s0 = sampleTable(t0, phase)
            val s1 = sampleTable(t1, phase)
            return s0 + (s1 - s0) * frac
        }
    }

    private var phase1 = 0.0
    private var phase2 = 0.0
    private var freq = 220.0
    private var targetFreq = 220.0
    private var tablePosition = 0f
    private var targetPosition = 0f
    private var active = false
    private var releasing = false
    private var cachedSr = 0
    private var invSr = 1.0 / 44_100
    private var fCoeff = 0.0
    private var posCoeff = 0.0
    private val lfo = Lfo()
    private val adsr = Adsr().apply {
        setup(attackSeconds = 0.02, decayRate = 1.8, sustain = 0.45, releaseRate = 5.0, maxSeconds = 12.0)
    }

    fun setTablePosition(pos: Float) {
        targetPosition = pos.coerceIn(0f, (NUM_TABLES - 1).toFloat())
    }

    override fun isAudible(): Boolean = active && adsr.isActive

    override fun noteOn(freqHz: Float, velocity: Float) {
        targetFreq = freqHz.toDouble().coerceIn(30.0, 4000.0)
        if (!active) {
            freq = targetFreq
            phase1 = 0.0
            phase2 = 0.0
        }
        adsr.trigger(velocity.coerceIn(0.05f, 1f).toDouble())
        active = true
        releasing = false
        cachedSr = 0
    }

    override fun noteOff() {
        if (active) {
            releasing = true
            adsr.release()
        }
    }

    private fun ensureSr(sampleRate: Int) {
        if (sampleRate == cachedSr) return
        cachedSr = sampleRate
        invSr = 1.0 / sampleRate
        fCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.04))
        posCoeff = 1.0 - exp(-1.0 / (sampleRate * 0.15))
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float {
        if (!active) return 0f
        ensureSr(sampleRate)
        val env = adsr.next(sampleRate)
        if (!adsr.isActive) {
            active = false
            return 0f
        }
        freq += (targetFreq - freq) * fCoeff
        tablePosition += ((targetPosition - tablePosition) * posCoeff).toFloat()

        // Subtle LFO modulation on table position for evolving shimmer
        val posMod = (tablePosition + 0.35f * lfo.next(0.25, sampleRate)).coerceIn(0f, 4f)

        val detuneRatio = 2.0.pow(detuneCents.toDouble() / 1200.0)
        phase1 = SinLut.wrapPhase(phase1 + 2.0 * PI * freq * invSr)
        phase2 = SinLut.wrapPhase(phase2 + 2.0 * PI * freq * detuneRatio * invSr)

        val w1 = sampleMorphed(posMod, phase1)
        val w2 = sampleMorphed((posMod + 0.5f).coerceAtMost(4f), phase2)

        return ((w1 + 0.45f * w2) * 0.40f * env).toFloat()
    }

    override fun reset() {
        active = false
        releasing = false
        adsr.reset()
    }
}

/** Polyphonic Wavetable voice pool. */
class WavetablePool(
    voiceCount: Int = 6,
) : SynthVoice {
    private val pool = PolyVoicePool(voiceCount) { WavetableSynthVoice() }

    fun setTablePosition(pos: Float) {
        // Updated via noteOn or continuous controls
    }

    override fun isAudible(): Boolean = pool.isAudible()

    override fun noteOn(freqHz: Float, velocity: Float) {
        pool.noteOn(freqHz, velocity)
    }

    override fun noteOff() {
        pool.noteOff()
    }

    override fun render(sampleIndex: Long, sampleRate: Int): Float =
        pool.render(sampleIndex, sampleRate)

    override fun reset() {
        pool.reset()
    }
}
