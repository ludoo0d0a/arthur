package fr.geoking.arthur.audio

import fr.geoking.arthur.audio.voices.GranularSynthVoice
import fr.geoking.arthur.audio.voices.WavetablePool
import fr.geoking.arthur.audio.voices.WavetableSynthVoice
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.AudioPackCatalog
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.FakePackOwnership
import fr.geoking.arthur.shared.marketplace.PackOwnership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WavetableSynthTest {

    @Test
    fun wavetableVoice_rendersNonZeroSamplesWhenTriggered() {
        val voice = WavetableSynthVoice()
        voice.setTablePosition(2.5f)
        voice.noteOn(220f, 0.8f)

        assertTrue(voice.isAudible())

        var hasNonZero = false
        for (i in 0 until 500) {
            val sample = voice.render(i.toLong(), 44100)
            if (abs(sample) > 0.001f) {
                hasNonZero = true
                break
            }
        }
        assertTrue("Wavetable voice should produce non-zero samples", hasNonZero)

        voice.noteOff()
        voice.reset()
    }

    @Test
    fun granularVoice_rendersCloudsAndPanOutputs() {
        val voice = GranularSynthVoice()
        voice.noteOn(440f, 0.7f)

        assertTrue(voice.isAudible())

        var hasNonZero = false
        for (i in 0 until 2000) {
            val sample = voice.render(i.toLong(), 44100)
            if (abs(sample) > 0.0001f) {
                hasNonZero = true
            }
        }
        assertTrue("Granular cloud should generate non-zero grain output", hasNonZero)
        assertTrue("Granular outPanL should be non-negative", voice.outPanL >= 0f)
        assertTrue("Granular outPanR should be non-negative", voice.outPanR >= 0f)

        voice.noteOff()
        voice.reset()
    }

    @Test
    fun musicPresetResolver_resolvesWavetableStyle() {
        val artwork = Artwork(
            id = "genart.scifi.01",
            title = "#10 - Wavetable Test",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        val prefs = MusicUserPrefs(stylePreference = MusicStyle.Wavetable)
        val fakeOwnership = FakePackOwnership().also { it.unlockAll() }

        val preset = MusicPresetResolver.resolve(artwork, prefs, fakeOwnership)

        assertEquals(MusicStyle.Wavetable, preset.style)
        assertTrue(preset.trackMix.bed > 0f)
        assertTrue(preset.trackMix.harmony > 0f)
        assertTrue(preset.trackMix.melody > 0f)
        assertEquals(0.48f, ProceduralMusicEngine.reverbWetFor(MusicStyle.Wavetable, SynthQuality.Full), 0.01f)
        assertEquals(0.95f, ProceduralMusicEngine.roomSizeFor(MusicStyle.Wavetable), 0.01f)
    }

    @Test
    fun audioPackCatalog_includesWavetablePack() {
        val packDef = AudioPackCatalog.packDef(AudioPackCatalog.WAVETABLE)
        assertNotNull(packDef)
        assertEquals("wavetable", packDef!!.primaryStyleSuffix)
        assertTrue(packDef.styleSuffixes.contains("wavetable"))

        val sellableId = MarketplaceCatalog.sellablePackIdForAudioPack(AudioPackCatalog.WAVETABLE)
        assertEquals("audio_wavetable", sellableId)
    }
}
