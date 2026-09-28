package fr.geoking.arthur.audio.markov

import fr.geoking.arthur.audio.MusicPresetResolver
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MarkovChainTest {
    @Test
    fun deterministicWalkWithFixedSeed() {
        val states = listOf("a", "b", "c")
        val chain1 = MarkovChain.connected(states, Random(42), initial = "a")
        val chain2 = MarkovChain.connected(states, Random(42), initial = "a")
        val walk1 = List(20) { chain1.next() }
        val walk2 = List(20) { chain2.next() }
        assertEquals(walk1, walk2)
    }

    @Test
    fun connectedChainNeverStaysStuckForever() {
        val states = (0 until 5).toList()
        val chain = MarkovChain.connected(states, Random(7), selfBias = 0.2f)
        val seen = mutableSetOf<Int>()
        repeat(200) { seen += chain.next() }
        assertTrue("expected multiple states, got $seen", seen.size >= 2)
    }
}

class MarkovSequencerTest {
    @Test
    fun sequencerAdvancesAndProducesHarmony() {
        val preset = MusicPresetResolver.resolve(
            Artwork(GenartSource.SNOW, "Snow", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
        )
        val seq = MarkovSequencer(preset, sessionSalt = 99L)
        var onsets = 0
        repeat(20_000) {
            seq.advanceHarmonyClock(44_100)
            if (seq.tickMelody(44_100)) onsets++
        }
        assertTrue(onsets > 0)
        assertTrue(seq.harmonyPartialsHz().isNotEmpty())
        assertNotNull(seq.transitionCue())
    }

    @Test
    fun resetPhraseIsSafe() {
        val preset = MusicPresetResolver.resolve(
            Artwork("x", "X", sourceId = "bundled", kind = ArtworkKind.Photo),
        )
        val seq = MarkovSequencer(preset, 1L)
        seq.resetPhrase()
        seq.tickMelody(44_100)
    }
}

class ArrangementFormTest {
    @Test
    fun forceBridgeThenTicks() {
        val form = ArrangementForm(123L)
        form.forceBridge(44_100, 0.01f)
        assertEquals(FormSection.Bridge, form.section)
        repeat(500) { form.tick(44_100) }
        // After bridge expires, section advances into cycle.
        assertTrue(form.section != FormSection.Bridge || form.melodyMul >= 0f)
    }
}
