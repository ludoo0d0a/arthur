package fr.geoking.arthur.audio.voices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EnvelopeTest {
    private val sr = 44_100

    @Test
    fun adsrAttackDecayReleaseFinishes() {
        val e = Adsr().apply { setup(0.01, 3.0, sustain = 0.0, releaseRate = 20.0, maxSeconds = 5.0) }
        e.trigger(0.8)
        var peak = 0.0
        repeat(sr / 10) { peak = maxOf(peak, e.next(sr)) }
        assertTrue("peak=$peak", peak in 0.7..0.81)
        e.release()
        repeat(sr) { e.next(sr) }
        assertFalse(e.isActive)
    }

    @Test
    fun adsrSustainHoldsLevel() {
        val e = Adsr().apply { setup(0.005, 8.0, sustain = 0.5, releaseRate = 10.0) }
        e.trigger(1.0)
        repeat(sr * 2) { e.next(sr) }
        assertTrue(e.isActive)
        assertEquals(0.5, e.level, 0.02)
    }

    @Test
    fun polyPoolKeepsOverlappingNotesAudible() {
        val pool = PolyVoicePool(4) { PluckGuitarVoice() }
        pool.noteOn(220f, 0.6f)
        repeat(2000) { pool.render(it.toLong(), sr) }
        pool.noteOn(330f, 0.6f)
        // First note must not be cut: the pool keeps producing sound for both.
        var energy = 0f
        repeat(2000) { energy += abs(pool.render(it.toLong(), sr)) }
        assertTrue(energy > 0.5f)
        assertTrue(pool.isAudible())
    }

    @Test
    fun lfoStaysInRange() {
        val l = Lfo()
        repeat(sr) { assertTrue(abs(l.next(0.5, sr)) <= 1.001f) }
    }
}
