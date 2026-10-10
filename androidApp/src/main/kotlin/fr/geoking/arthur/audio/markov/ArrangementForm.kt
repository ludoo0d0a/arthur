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
 * Section lengths are in musical bars at [tempoBpm] (4/4), not wall-clock seconds.
 * Controls density multipliers for melody / texture emphasis.
 */
class ArrangementForm(
    formSeed: Long,
    private val tempoBpm: Float = 72f,
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
    private var sectionStartBar = 0L
    private var transport: GrooveTransport? = null

    var section: FormSection = FormSection.Intro
        private set

    /** Bars scheduled for the current section (for tests / diagnostics). */
    var currentSectionBars: Int = 0
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

    /** Optional shared groove transport so section lengths stay phase-locked with harmony. */
    fun attachTransport(groove: GrooveTransport?) {
        transport = groove
    }

    /**
     * Jump to Bridge for [bars] measures (default 2).
     * Legacy [seconds] overload converts via tempo when [bars] is null.
     */
    fun forceBridge(sampleRate: Int, bars: Int = 2) {
        section = FormSection.Bridge
        lastSampleRate = sampleRate
        val n = bars.coerceIn(1, 8)
        currentSectionBars = n
        samplesRemaining = barsToSamples(n, sampleRate).coerceAtLeast(1)
    }

    /** @deprecated Prefer [forceBridge] with bar count. */
    fun forceBridge(sampleRate: Int, seconds: Float) {
        val bpm = tempoBpm.coerceAtLeast(30f)
        val secondsPerBar = 4.0 * 60.0 / bpm
        val bars = (seconds / secondsPerBar).toInt().coerceIn(1, 8)
        forceBridge(sampleRate, bars)
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

    /** Samples remaining in the current section (0 if not yet scheduled). */
    fun samplesUntilSectionChange(): Int = samplesRemaining

    /** Fast-forward arrangement clock by [frames] samples. */
    fun skipSamples(frames: Int, sampleRate: Int) {
        if (frames <= 0) return
        lastSampleRate = sampleRate
        var left = frames
        while (left > 0) {
            if (samplesRemaining <= 0) {
                scheduleCurrent(sampleRate)
            }
            if (samplesRemaining <= 0) break
            val step = minOf(left, samplesRemaining)
            samplesRemaining -= step
            left -= step
            if (samplesRemaining == 0) advance(sampleRate)
        }
    }

    private fun advance(sampleRate: Int) {
        index = (index + 1) % cycle.size
        section = cycle[index]
        samplesRemaining = 0
        scheduleCurrent(sampleRate)
    }

    private fun scheduleCurrent(sampleRate: Int) {
        if (samplesRemaining > 0) return
        val bars = when (section) {
            FormSection.Intro -> 2 + random.nextInt(3) // 2–4
            FormSection.A -> 8 + random.nextInt(5) // 8–12 chorus
            FormSection.B -> 4 + random.nextInt(5) // 4–8
            FormSection.Bridge -> 2 + random.nextInt(3) // 2–4
        }
        currentSectionBars = bars
        samplesRemaining = barsToSamples(bars, sampleRate).coerceAtLeast(sampleRate)
        sectionStartBar += bars
    }

    private fun barsToSamples(bars: Int, sampleRate: Int): Int {
        val t = transport
        return if (t != null) {
            t.barsDuration(sectionStartBar, bars)
        } else {
            val bpm = tempoBpm.coerceAtLeast(30f)
            val secondsPerBar = 4.0 * 60.0 / bpm
            (bars * secondsPerBar * sampleRate).toInt().coerceAtLeast(1)
        }
    }
}
