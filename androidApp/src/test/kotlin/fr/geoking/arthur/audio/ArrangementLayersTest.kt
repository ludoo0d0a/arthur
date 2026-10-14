package fr.geoking.arthur.audio

import fr.geoking.arthur.audio.markov.ArrangementForm
import fr.geoking.arthur.audio.voices.SoftKitVoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ArrangementLayersTest {
    @Test
    fun layersFollowTheForm() {
        val form = ArrangementForm(1L, 72f)
        // Starts in the intro: no percussion, no counter-melody, reduced bass.
        assertEquals(0f, form.percMul, 0f)
        assertEquals(0f, form.counterMul, 0f)
        assertTrue(form.bassMul < 1f)
        // Walk the whole cycle and confirm the contrast section brings the counter-melody in.
        var sawCounter = false
        repeat(44_100 * 400) {
            form.tick(44_100)
            if (form.counterMul > 0.9f) sawCounter = true
        }
        assertTrue(sawCounter)
    }

    @Test
    fun softKitDecaysToSilence() {
        val kit = SoftKitVoice()
        kit.hat(0.5f)
        kit.kick(0.5f)
        var peak = 0f
        repeat(44_100) { peak = maxOf(peak, abs(kit.render(it.toLong(), 44_100))) }
        assertTrue("kit should sound", peak > 0.01f)
        assertTrue("kit should finish", !kit.isAudible())
    }
}
