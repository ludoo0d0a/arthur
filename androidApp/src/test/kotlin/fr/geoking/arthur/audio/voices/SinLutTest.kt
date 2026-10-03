package fr.geoking.arthur.audio.voices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SinLutTest {
    @Test
    fun lutMatchesMathSinWithinTolerance() {
        var maxErr = 0f
        val steps = 512
        for (i in 0 until steps) {
            val phase = 2.0 * PI * i / steps
            val err = abs(SinLut.sin(phase) - sin(phase).toFloat())
            maxErr = maxOf(maxErr, err)
        }
        assertTrue("maxErr=$maxErr", maxErr < 0.002f)
    }

    @Test
    fun wrapPhaseStaysInRange() {
        val wrapped = SinLut.wrapPhase(2.0 * PI + 0.1)
        assertTrue(wrapped >= 0.0 && wrapped < 2.0 * PI)
        assertEquals(0.1, wrapped, 1e-9)
    }

    @Test
    fun padRenderIntoMatchesSingleSampleSum() {
        val pad = SinePadVoice(2f)
        pad.setChord(listOf(220f, 277f, 330f))
        // Warm up
        repeat(100) { pad.render(it.toLong(), 48_000) }

        val a = SinePadVoice(2f)
        a.setChord(listOf(220f, 277f, 330f))
        repeat(100) { a.render(it.toLong(), 48_000) }
        val block = FloatArray(64)
        a.renderInto(block, 0, 64, 48_000, 1f)

        val b = SinePadVoice(2f)
        b.setChord(listOf(220f, 277f, 330f))
        repeat(100) { b.render(it.toLong(), 48_000) }
        var maxDiff = 0f
        for (i in 0 until 64) {
            val s = b.render(i.toLong(), 48_000)
            maxDiff = maxOf(maxDiff, abs(block[i] - s))
        }
        assertTrue("block vs sample maxDiff=$maxDiff", maxDiff < 1e-5f)
    }
}
