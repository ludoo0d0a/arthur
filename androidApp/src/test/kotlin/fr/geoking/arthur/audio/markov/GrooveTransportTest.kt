package fr.geoking.arthur.audio.markov

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrooveTransportTest {
    @Test
    fun offbeatOffsetMatchesSwingRatio() {
        val bpm = 120f
        val sr = 48_000
        val swing = 0.67f
        val t = GrooveTransport(bpm, sr, swingRatio = swing)
        val eighthStraight = (60.0 / bpm * 0.5 * sr)
        val expectedOffbeat = (swing * eighthStraight).toLong()
        assertEquals(0L, t.sampleAtEighth(0))
        assertEquals(expectedOffbeat, t.sampleAtEighth(1))
        assertEquals((2 * eighthStraight).toLong(), t.sampleAtEighth(2))
        val expectedThird = (2 * eighthStraight + swing * eighthStraight).toLong()
        assertEquals(expectedThird, t.sampleAtEighth(3))
    }

    @Test
    fun twoEighthsEqualOneQuarter() {
        val t = GrooveTransport(96f, 44_100, swingRatio = 0.67f)
        repeat(16) { q ->
            val q0 = t.sampleAtQuarter(q.toLong())
            val q1 = t.sampleAtQuarter(q + 1L)
            val e0 = t.sampleAtEighth(q * 2L)
            val e2 = t.sampleAtEighth(q * 2L + 2)
            assertEquals(q0, e0)
            assertEquals(q1, e2)
            val sum = t.eighthDuration(q * 2L) + t.eighthDuration(q * 2L + 1)
            assertEquals(t.quarterDuration(q.toLong()), sum)
        }
    }

    @Test
    fun barDurationIsFourQuarters() {
        val t = GrooveTransport(80f, 44_100)
        val bar = t.barsDuration(0, 1)
        var sum = 0
        for (q in 0 until 4) sum += t.quarterDuration(q.toLong())
        assertEquals(bar, sum)
    }

    @Test
    fun samplesUntilNextEighthFromMidGrid() {
        val t = GrooveTransport(100f, 48_000, swingRatio = 0.67f)
        val off = t.sampleAtEighth(1)
        assertEquals(0, t.samplesUntilNextEighth(off))
        val mid = off / 2
        val until = t.samplesUntilNextEighth(mid)
        assertTrue(until > 0)
        assertEquals(off, mid + until)
    }

    @Test
    fun indexAtHelpersTrackGrid() {
        val t = GrooveTransport(90f, 44_100)
        val s = t.sampleAtEighth(5)
        assertEquals(5L, t.eighthIndexAt(s))
        assertEquals(2L, t.quarterIndexAt(s))
    }
}
