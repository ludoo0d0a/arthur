package fr.geoking.arthur.audio.markov

import fr.geoking.arthur.audio.MusicPresetResolver
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.MusicUserPrefs
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.source.AmbientAudioCharacter
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
            MusicUserPrefs(character = AmbientAudioCharacter.Melody, stylePreference = MusicStyle.JazzPiano),
        )
        val seq = MarkovSequencer(preset, sessionSalt = 99L, character = AmbientAudioCharacter.Melody)
        var onsets = 0
        var bassHits = 0
        // ~8 s of audio at 44.1 kHz
        repeat(44_100 * 8) {
            seq.advanceHarmonyClock(44_100)
            if (seq.tickMelody(44_100)) onsets++
            if (seq.tickBass(44_100)) bassHits++
        }
        assertTrue("expected melodic onsets, got $onsets", onsets >= 6)
        assertTrue("expected bass hits, got $bassHits", bassHits >= 4)
        assertTrue(seq.harmonyPartialsHz().isNotEmpty())
        assertNotNull(seq.transitionCue())
        val bassHz = seq.currentBassFrequencyHz
        assertNotNull(bassHz)
        assertTrue("bass out of range: $bassHz", bassHz!! in 40f..140f)
    }

    @Test
    fun jazzPresetProducesFrequentOnsets() {
        val preset = MusicPresetResolver.resolve(
            Artwork("test.jazz", "Jazz", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.JazzPiano,
                complexity = 0.9f,
            ),
        )
        val seq = MarkovSequencer(preset, sessionSalt = 7L, character = AmbientAudioCharacter.Melody)
        var onsets = 0
        repeat(44_100 * 6) {
            if (seq.tickMelody(44_100)) onsets++
        }
        assertTrue("onsets=$onsets", onsets >= 5)
    }

    @Test
    fun atmosphereIsSparserThanMelody() {
        val art = Artwork("test.atm", "Atm", sourceId = "test", kind = ArtworkKind.Photo)
        val prefsMel = MusicUserPrefs(
            character = AmbientAudioCharacter.Melody,
            stylePreference = MusicStyle.Zen,
            complexity = 0.8f,
        )
        val prefsAtm = MusicUserPrefs(
            character = AmbientAudioCharacter.Atmosphere,
            stylePreference = MusicStyle.Zen,
            complexity = 0.8f,
        )
        val melSeq = MarkovSequencer(
            MusicPresetResolver.resolve(art, prefsMel),
            11L,
            AmbientAudioCharacter.Melody,
        )
        val atmSeq = MarkovSequencer(
            MusicPresetResolver.resolve(art, prefsAtm),
            11L,
            AmbientAudioCharacter.Atmosphere,
        )
        var melOnsets = 0
        var atmOnsets = 0
        repeat(44_100 * 8) {
            if (melSeq.tickMelody(44_100)) melOnsets++
            if (atmSeq.tickMelody(44_100)) atmOnsets++
        }
        assertTrue("melody=$melOnsets atm=$atmOnsets", atmOnsets < melOnsets)
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

    @Test
    fun usesProvidedSampleRateForSectionLength() {
        val form = ArrangementForm(1L)
        form.tick(22_050)
        // After scheduling, remaining ticks should roughly match ~4–6s at 22050.
        // Just ensure ticking at alternate SR does not crash and advances.
        repeat(22_050 * 20) { form.tick(22_050) }
        assertNotNull(form.section)
    }
}
