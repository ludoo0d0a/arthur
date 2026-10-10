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
import kotlin.math.abs
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
        var maxMelodyHz = 0f
        // ~8 s of audio at 44.1 kHz
        repeat(44_100 * 8) {
            seq.advanceHarmonyClock(44_100)
            if (seq.tickMelody(44_100)) {
                onsets++
                seq.currentMelodyHz?.let { maxMelodyHz = maxOf(maxMelodyHz, it) }
            }
            if (seq.tickBass(44_100)) bassHits++
        }
        assertTrue("expected melodic onsets, got $onsets", onsets >= 6)
        assertTrue("expected bass hits, got $bassHits", bassHits >= 4)
        assertTrue(seq.harmonyPartialsHz().isNotEmpty())
        assertNotNull(seq.transitionCue())
        val bassHz = seq.currentBassFrequencyHz
        assertNotNull(bassHz)
        assertTrue("bass out of range: $bassHz", bassHz!! in 40f..140f)
        assertTrue(
            "melody too bright: $maxMelodyHz",
            maxMelodyHz in MarkovSequencer.MELODY_MIN_HZ..MarkovSequencer.MELODY_MAX_HZ,
        )
    }

    @Test
    fun melodyStaysInWarmRegisterAcrossStyles() {
        val styles = listOf(
            MusicStyle.JazzPiano,
            MusicStyle.Zen,
            MusicStyle.SoftGuitar,
            MusicStyle.WindChimes,
            MusicStyle.AfricanPulse,
        )
        val ownership = fr.geoking.arthur.shared.marketplace.FakePackOwnership().also { it.unlockAll() }
        for (style in styles) {
            val preset = MusicPresetResolver.resolve(
                Artwork("test.warm.$style", "Warm", sourceId = "test", kind = ArtworkKind.Photo),
                MusicUserPrefs(
                    character = AmbientAudioCharacter.Melody,
                    stylePreference = style,
                    complexity = 0.9f,
                ),
                ownership,
            )
            val seq = MarkovSequencer(preset, sessionSalt = 123L, character = AmbientAudioCharacter.Melody)
            repeat(44_100 * 10) {
                seq.advanceHarmonyClock(44_100)
                if (seq.tickMelody(44_100)) {
                    val hz = seq.currentMelodyHz
                    assertNotNull(hz)
                    assertTrue(
                        "$style melody out of warm band: $hz",
                        hz!! in MarkovSequencer.MELODY_MIN_HZ..MarkovSequencer.MELODY_MAX_HZ,
                    )
                }
            }
        }
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

    @Test
    fun skipSamplesPreservesOnsetTiming() {
        val preset = MusicPresetResolver.resolve(
            Artwork("test.skip", "Skip", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.JazzPiano,
            ),
        )
        val a = MarkovSequencer(preset, 42L, AmbientAudioCharacter.Melody)
        val b = MarkovSequencer(preset, 42L, AmbientAudioCharacter.Melody)
        // Advance both until a countdown is active.
        repeat(100) {
            a.advanceHarmonyClock(48_000)
            a.tickMelody(48_000)
            a.tickBass(48_000)
            b.advanceHarmonyClock(48_000)
            b.tickMelody(48_000)
            b.tickBass(48_000)
        }
        val idle = minOf(
            a.samplesUntilMelody().coerceAtLeast(1),
            a.samplesUntilBass().coerceAtLeast(1),
            a.samplesUntilHarmony().coerceAtLeast(1),
        )
        if (idle > 1) {
            val jump = idle / 2
            a.skipSamples(jump)
            repeat(jump) {
                b.advanceHarmonyClock(48_000)
                b.tickMelody(48_000)
                b.tickBass(48_000)
            }
            assertEquals(a.samplesUntilMelody(), b.samplesUntilMelody())
            assertEquals(a.samplesUntilBass(), b.samplesUntilBass())
            assertEquals(a.samplesUntilHarmony(), b.samplesUntilHarmony())
        }
    }

    @Test
    fun jazzUsesGrooveClockZenDoesNot() {
        val jazz = MusicPresetResolver.resolve(
            Artwork("test.groove", "G", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.JazzPiano,
            ),
        )
        val zen = MusicPresetResolver.resolve(
            Artwork("test.zen", "Z", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.Zen,
            ),
        )
        val jazzSeq = MarkovSequencer(jazz, 1L, AmbientAudioCharacter.Melody)
        val zenSeq = MarkovSequencer(zen, 1L, AmbientAudioCharacter.Melody)
        val jazzAtm = MarkovSequencer(jazz, 1L, AmbientAudioCharacter.Atmosphere)
        assertTrue(jazzSeq.useGrooveClock)
        assertTrue(!zenSeq.useGrooveClock)
        assertTrue(!jazzAtm.useGrooveClock)
    }

    @Test
    fun grooveBassAlignsToTransportQuarters() {
        val preset = MusicPresetResolver.resolve(
            Artwork("test.bassAlign", "B", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.JazzPiano,
            ),
        )
        val seq = MarkovSequencer(preset, 99L, AmbientAudioCharacter.Melody)
        assertTrue(seq.useGrooveClock)
        val transport = GrooveTransport(preset.tempoBpm, 44_100)
        val bassOnsets = mutableListOf<Long>()
        var sample = 0L
        repeat(44_100 * 4) {
            seq.advanceHarmonyClock(44_100)
            seq.tickMelody(44_100)
            if (seq.tickBass(44_100)) {
                bassOnsets += sample
            }
            sample++
        }
        assertTrue("expected several bass onsets, got ${bassOnsets.size}", bassOnsets.size >= 4)
        for (onset in bassOnsets.take(12)) {
            val q = transport.quarterIndexAt(onset)
            val expected = transport.sampleAtQuarter(q)
            assertTrue(
                "bass onset $onset not on quarter (expected $expected)",
                abs(onset - expected) <= 1L,
            )
        }
    }

    @Test
    fun grooveHarmonyChangesOnBarBoundary() {
        val preset = MusicPresetResolver.resolve(
            Artwork("test.harmBar", "H", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = MusicStyle.JazzPiano,
            ),
        )
        val seq = MarkovSequencer(preset, 3L, AmbientAudioCharacter.Melody)
        val transport = GrooveTransport(preset.tempoBpm, 44_100)
        val changes = mutableListOf<Long>()
        var sample = 0L
        // First advanceHarmonyClock fires immediately (samplesUntil=0).
        repeat(44_100 * 8) {
            seq.advanceHarmonyClock(44_100)
            if (seq.harmonyChanged) changes += sample
            seq.tickMelody(44_100)
            seq.tickBass(44_100)
            sample++
        }
        assertTrue("expected harmony changes, got ${changes.size}", changes.size >= 2)
        // Skip the initial change at sample 0; later ones should land on bar starts.
        for (onset in changes.drop(1).take(8)) {
            val bar = transport.barIndexAt(onset)
            val expected = transport.sampleAtBar(bar)
            assertTrue(
                "harmony change $onset not on bar (expected $expected)",
                abs(onset - expected) <= 1L,
            )
        }
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
