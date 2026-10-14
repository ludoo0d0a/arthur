package fr.geoking.arthur.audio.markov

import fr.geoking.arthur.audio.MusicPresetResolver
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.MusicUserPrefs
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.FakePackOwnership
import fr.geoking.arthur.source.AmbientAudioCharacter
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PhraseCompositionTest {
    private val sr = 44_100

    private fun sequencer(style: MusicStyle, id: String, salt: Long): MarkovSequencer {
        val ownership = FakePackOwnership().also { it.unlockAll() }
        val preset = MusicPresetResolver.resolve(
            Artwork(id, "T", sourceId = "test", kind = ArtworkKind.Photo),
            MusicUserPrefs(
                character = AmbientAudioCharacter.Melody,
                stylePreference = style,
                complexity = 0.9f,
            ),
            ownership,
        )
        return MarkovSequencer(preset, salt, AmbientAudioCharacter.Melody)
    }

    @Test
    fun melodyUsesMoreThanOneOctaveOfRange() {
        val seq = sequencer(MusicStyle.ClassicalPiano, "range.test", 5L)
        val pitches = HashSet<Int>()
        var min = Float.MAX_VALUE
        var max = 0f
        repeat(sr * 120) {
            seq.advanceHarmonyClock(sr)
            if (seq.tickMelody(sr)) {
                val hz = seq.currentMelodyHz!!
                min = minOf(min, hz)
                max = maxOf(max, hz)
                pitches += Math.round(hz)
            }
            seq.tickBass(sr)
        }
        assertTrue("span too narrow: $min..$max", max / min > 1.8f)
        assertTrue("too few distinct pitches: ${pitches.size}", pitches.size >= 8)
    }

    @Test
    fun melodyStaysWithinHardBounds() {
        for (style in listOf(MusicStyle.JazzPiano, MusicStyle.SoftGuitar, MusicStyle.ClassicalPiano)) {
            val seq = sequencer(style, "bounds.$style", 11L)
            repeat(sr * 90) {
                seq.advanceHarmonyClock(sr)
                if (seq.tickMelody(sr)) {
                    val hz = seq.currentMelodyHz!!
                    assertTrue("$style $hz", hz in MarkovSequencer.MELODY_MIN_HZ..MarkovSequencer.MELODY_MAX_HZ)
                }
                seq.tickBass(sr)
            }
        }
    }

    @Test
    fun padVoicingMovesLessThanRootPositionJumps() {
        val seq = sequencer(MusicStyle.JazzPiano, "voice.lead", 3L)
        var prevAvg: Float? = null
        var changes = 0
        var maxJump = 0f
        repeat(sr * 120) {
            seq.advanceHarmonyClock(sr)
            seq.tickBass(sr)
            if (seq.harmonyChanged) {
                seq.harmonyPartialsHz()
                val voiced = seq.voicedPartialsHz()
                assertTrue(voiced.isNotEmpty())
                assertTrue("voicing sorted", voiced == voiced.sorted())
                val avg = voiced.average().toFloat()
                prevAvg?.let { maxJump = maxOf(maxJump, abs(Math.log((avg / it).toDouble()).toFloat())) }
                prevAvg = avg
                changes++
            }
        }
        assertTrue("need several chord changes, got $changes", changes >= 4)
        // Centre of the voicing never leaps more than ~a tritone between chords.
        assertTrue("voicing jumped too far: $maxJump", maxJump < 0.45f)
    }
}
