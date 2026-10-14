package fr.geoking.arthur.audio.fx

import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

class StereoFxTest {
    private val sr = 48_000

    @Test
    fun reverbTailDecaysAndStaysFinite() {
        val rv = StereoReverb().apply { configure(sr, 0.5f) }
        rv.process(1f)
        var early = 0f
        var late = 0f
        for (i in 1 until sr * 8) {
            rv.process(0f)
            assertTrue(rv.outL.isFinite() && rv.outR.isFinite())
            if (i < sr) early += abs(rv.outL)
            if (i > sr * 6) late += abs(rv.outL)
        }
        assertTrue("reverb should ring", early > 0f)
        assertTrue("tail should decay", late < early * 0.2f)
    }

    @Test
    fun reverbChannelsDecorrelated() {
        val rv = StereoReverb().apply { configure(sr, 0.5f) }
        rv.process(1f)
        var diff = 0f
        repeat(sr) {
            rv.process(0f)
            diff += abs(rv.outL - rv.outR)
        }
        assertTrue(diff > 0.01f)
    }

    @Test
    fun panIsConstantPower() {
        for (p in listOf(-1f, -0.4f, 0f, 0.7f, 1f)) {
            val pan = Pan(p)
            assertTrue(abs(pan.l * pan.l + pan.r * pan.r - 2f) < 1e-4f)
        }
    }

    @Test
    fun pingPongEchoesAfterDelayAndAlternates() {
        val d = PingPongDelay().apply { configure(sr, 0.1f) }
        d.process(1f)
        var firstL = -1
        var firstR = -1
        for (i in 1 until sr) {
            d.process(0f)
            if (firstL < 0 && abs(d.outL) > 0.1f) firstL = i
            if (firstR < 0 && abs(d.outR) > 0.01f) firstR = i
        }
        assertTrue(firstL in (0.09f * sr).toInt()..(0.11f * sr).toInt())
        assertTrue("right echo follows left", firstR > firstL)
    }

    @Test
    fun chorusOutputFinite() {
        val c = StereoChorus().apply { configure(sr) }
        repeat(sr) { i ->
            c.process(kotlin.math.sin(i * 0.05f))
            assertTrue(c.outL.isFinite() && c.outR.isFinite())
        }
    }
}
