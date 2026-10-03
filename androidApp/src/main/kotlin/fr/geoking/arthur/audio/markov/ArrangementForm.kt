package fr.geoking.arthur.audio.markov

import kotlin.random.Random

enum class FormSection {
    Intro,
    A,
    B,
    Bridge,
}

/**
 * Seeded arrangement cycle: Intro → A → A → B → A → Bridge → A…
 * Controls density multipliers for melody / texture emphasis.
 */
class ArrangementForm(
    formSeed: Long,
) {
    private val random = Random(formSeed)
    private val cycle = listOf(
        FormSection.Intro,
        FormSection.A,
        FormSection.A,
        FormSection.B,
        FormSection.A,
        FormSection.Bridge,
        FormSection.A,
    )
    private var index = 0
    private var samplesRemaining = 0
    private var lastSampleRate = 44_100

    var section: FormSection = FormSection.Intro
        private set

    /** Melody gain multiplier for the current section. */
    val melodyMul: Float
        get() = when (section) {
            FormSection.Intro -> 0.4f
            FormSection.A -> 1f
            FormSection.B -> 0.55f
            FormSection.Bridge -> 0.2f
        }

    /** Texture gain multiplier. */
    val textureMul: Float
        get() = when (section) {
            FormSection.Intro -> 0.8f
            FormSection.A -> 0.7f
            FormSection.B -> 1f
            FormSection.Bridge -> 1.3f
        }

    fun forceBridge(sampleRate: Int, seconds: Float = 2.5f) {
        section = FormSection.Bridge
        lastSampleRate = sampleRate
        samplesRemaining = (seconds * sampleRate).toInt().coerceAtLeast(1)
    }

    fun tick(sampleRate: Int) {
        lastSampleRate = sampleRate
        if (samplesRemaining > 0) {
            samplesRemaining--
            if (samplesRemaining == 0) advance(sampleRate)
            return
        }
        // First tick: schedule Intro length.
        scheduleCurrent(sampleRate)
    }

    private fun advance(sampleRate: Int) {
        index = (index + 1) % cycle.size
        section = cycle[index]
        samplesRemaining = 0
        scheduleCurrent(sampleRate)
    }

    private fun scheduleCurrent(sampleRate: Int) {
        if (samplesRemaining > 0) return
        val seconds = when (section) {
            FormSection.Intro -> 4.0 + random.nextDouble() * 2.0
            FormSection.A -> 10.0 + random.nextDouble() * 6.0
            FormSection.B -> 8.0 + random.nextDouble() * 4.0
            FormSection.Bridge -> 3.0 + random.nextDouble() * 2.0
        }
        samplesRemaining = (seconds * sampleRate).toInt().coerceAtLeast(sampleRate)
    }
}
