package fr.geoking.arthur.audio.voices

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WavetablePianoTest {
    @Test
    fun wavetableTablesAreNormalized() {
        for (table in listOf(
            PianoWavetable.lowAttack,
            PianoWavetable.lowBody,
            PianoWavetable.highAttack,
            PianoWavetable.highBody,
        )) {
            assertTrue(table.size == PianoWavetable.SIZE)
            var peak = 0f
            for (s in table) peak = maxOf(peak, abs(s))
            assertTrue("peak=$peak", peak in 0.9f..1.01f)
        }
    }

    @Test
    fun voiceAttackIsAudibleAndReleases() {
        val v = WavetablePianoVoice()
        v.noteOn(261.63f, 0.8f)
        var peak = 0f
        repeat(1_000) { i -> peak = maxOf(peak, abs(v.render(i.toLong(), 44_100))) }
        assertTrue("attack peak=$peak", peak > 0.04f)
        v.noteOff()
        var faded = false
        for (i in 0 until 24_000) {
            if (abs(v.render(1_000L + i, 44_100)) < 1e-3f) {
                faded = true
                break
            }
        }
        assertTrue(faded)
    }

    @Test
    fun poolAllowsPolyphony() {
        val pool = WavetablePianoPool(6)
        pool.noteOn(220f, 0.7f)
        pool.noteOn(277f, 0.65f)
        pool.noteOn(330f, 0.6f)
        var peak = 0f
        repeat(2_000) { i -> peak = maxOf(peak, abs(pool.render(i.toLong(), 44_100))) }
        assertTrue("poly peak=$peak", peak > 0.08f)
    }

    @Test
    fun higherVelocityYieldsMoreEarlyEnergy() {
        fun energy(vel: Float): Double {
            val v = WavetablePianoVoice()
            v.noteOn(330f, vel)
            var sum = 0.0
            repeat(500) { i -> sum += abs(v.render(i.toLong(), 44_100)).toDouble() }
            return sum
        }
        assertTrue(energy(0.95f) > energy(0.25f) * 1.15)
    }
}
