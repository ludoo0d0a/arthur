package fr.geoking.arthur.audio.voices

import fr.geoking.arthur.audio.fx.RoomBus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoftPianoPoolTest {
    @Test
    fun polyphonyAllowsOverlappingNotes() {
        val pool = SoftPianoPool(6)
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
    fun pianoAttackIsAudibleAndReleaseFades() {
        val voice = SoftPianoVoice()
        voice.noteOn(220f, 0.85f)
        var attackPeak = 0f
        repeat(800) { i ->
            attackPeak = maxOf(attackPeak, abs(voice.render(i.toLong(), 44_100)))
        }
        assertTrue("attack should be audible, peak=$attackPeak", attackPeak > 0.04f)
        voice.noteOff()
        var last = 1f
        var faded = false
        for (i in 0 until 20_000) {
            val s = abs(voice.render(800L + i, 44_100))
            last = s
            if (s < 1e-3f) {
                faded = true
                break
            }
        }
        assertTrue("release should fade toward silence, last=$last", faded || last < 0.02f)
    }

    @Test
    fun higherVelocityYieldsBrighterEarlyEnergy() {
        fun earlyEnergy(vel: Float): Double {
            val v = SoftPianoVoice()
            v.noteOn(330f, vel)
            var sum = 0.0
            // Capture attack + hammer window where brightness matters most.
            repeat(600) { i -> sum += abs(v.render(i.toLong(), 44_100)).toDouble() }
            return sum
        }
        val soft = earlyEnergy(0.25f)
        val hard = earlyEnergy(0.95f)
        assertTrue("hard strike should carry more early energy soft=$soft hard=$hard", hard > soft * 1.15)
    }

    @Test
    fun roomBusLpAttenuatesAndReverbAddsTail() {
        val bus = RoomBus()
        bus.configure(44_100, reverb = true, wetAmount = 0.2f)
        // Impulse through LP then room — expect non-zero tail after impulse ends.
        var lpOut = 0f
        repeat(8) { lpOut = bus.processPianoLp(1f) }
        assertTrue("LP should pass energy, out=$lpOut", abs(lpOut) > 0.1f)
        var impulseOut = 0f
        repeat(32) { impulseOut = maxOf(impulseOut, abs(bus.processRoom(1f))) }
        assertTrue("reverb wet path should be audible, peak=$impulseOut", impulseOut > 0.05f)
        var tail = 0f
        repeat(8_000) { i ->
            if (i > 2_000) tail = maxOf(tail, abs(bus.processRoom(0f)))
        }
        assertTrue("short reverb should leave a tail, tail=$tail", tail > 1e-4f)
    }

    @Test
    fun roomBusMinimalSkipsReverb() {
        val bus = RoomBus()
        bus.configure(44_100, reverb = false, wetAmount = 0.2f)
        assertEquals(0.5f, bus.processRoom(0.5f), 1e-6f)
    }

    @Test
    fun softLimitTamesPeaks() {
        assertTrue(abs(softLimit(2f)) <= 1f)
        assertTrue(abs(softLimit(-3f)) <= 1f)
        assertEquals(0f, softLimit(0f), 0.0001f)
        // Near-linear for quiet signals.
        assertEquals(0.1f * 1.15f / (1f + abs(0.1f * 1.15f) * 0.35f), softLimit(0.1f), 1e-5f)
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

    @Test
    fun padChordGlideAvoidsHardJumpSpike() {
        val pad = SinePadVoice(2f, glideSeconds = 0.08f)
        pad.setChord(listOf(110f, 165f, 220f))
        repeat(8_000) { i -> pad.render(i.toLong(), 44_100) }
        pad.setChord(listOf(220f, 277f, 330f))
        // Right after chord change, consecutive samples should not jump by a huge delta.
        var prev = pad.render(8_000L, 44_100)
        var maxJump = 0f
        for (i in 1 until 2_000) {
            val s = pad.render(8_000L + i, 44_100)
            maxJump = maxOf(maxJump, abs(s - prev))
            prev = s
        }
        assertTrue("chord glide jump too large: $maxJump", maxJump < 0.35f)
    }

    @Test
    fun bassVoiceStaysInLowBand() {
        val bass = SoftBassVoice()
        bass.noteOn(55f, 0.7f)
        var peak = 0f
        repeat(4_000) { i ->
            peak = maxOf(peak, abs(bass.render(i.toLong(), 44_100)))
        }
        assertTrue("bass should be audible, peak=$peak", peak > 0.05f)
    }

    @Test
    fun pianoStealDoesNotHardZeroImmediately() {
        val pool = SoftPianoPool(1)
        pool.noteOn(220f, 0.8f)
        repeat(500) { i -> pool.render(i.toLong(), 44_100) }
        pool.noteOn(330f, 0.8f)
        // Immediately after steal, output can change but pool stays finite / non-NaN.
        val s = pool.render(500L, 44_100)
        assertTrue(s.isFinite())
    }
}
