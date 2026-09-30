package fr.geoking.arthur.audio.voices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoftPianoPoolTest {
    @Test
    fun polyphonyAllowsOverlappingNotes() {
        val pool = SoftPianoPool(4)
        pool.noteOn(261.63f, 0.8f)
        pool.noteOn(329.63f, 0.7f)
        pool.noteOn(392.00f, 0.6f)
        var peak = 0f
        repeat(2000) { i ->
            val s = pool.render(i.toLong(), 44_100)
            peak = maxOf(peak, abs(s))
        }
        assertTrue("expected audible output, peak=$peak", peak > 0.05f)
    }

    @Test
    fun softLimitTamesPeaks() {
        assertTrue(abs(softLimit(2f)) <= 1f)
        assertEquals(0f, softLimit(0f), 0.0001f)
    }

    @Test
    fun padHasNoLfoThrobSpike() {
        val pad = SinePadVoice(2f)
        pad.setChord(listOf(220f, 277f, 330f))
        val samples = FloatArray(44_100) { i -> pad.render(i.toLong(), 44_100) }
        // After settle, amplitude envelope of |sample| should not swing like old 0.12Hz LFO (65%–100%).
        val window = 2000
        val mid = samples.size / 2
        var sum = 0.0
        for (i in mid until mid + window) sum += abs(samples[i])
        val mean = (sum / window).toFloat()
        assertTrue("pad should be audible after settle, mean=$mean", mean > 0.01f)
        var maxDev = 0f
        for (i in mid until mid + window) {
            maxDev = maxOf(maxDev, abs(abs(samples[i]) - mean))
        }
        // Continuous pad without LFO: short-window deviation stays modest vs mean.
        assertTrue("unexpected throb maxDev=$maxDev mean=$mean", maxDev < mean * 1.5f + 0.05f)
    }
}
