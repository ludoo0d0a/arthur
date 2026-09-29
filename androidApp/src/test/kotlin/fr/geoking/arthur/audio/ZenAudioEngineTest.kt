package fr.geoking.arthur.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.source.AmbientAudioMode
import fr.geoking.arthur.source.AmbientAudioSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ZenAudioEngineTest {
    private lateinit var context: Context
    private lateinit var settings: AmbientAudioSettings

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settings = AmbientAudioSettings(context)
    }

    @Test
    fun engineLifecycleWhenDisabledDoesNotStart() {
        settings.setEnabled(false)
        val engine = ZenAudioEngine(context, settings)
        engine.start()
        engine.triggerChime()
        engine.stop()
        engine.destroy()
    }

    @Test
    fun engineLifecycleWhenEnabledStartsAndStops() {
        settings.setEnabled(true)
        settings.setMode(AmbientAudioMode.PAD_AND_CHIME)
        val engine = ZenAudioEngine(context, settings)
        engine.start()
        engine.triggerChime()
        engine.stop()
        engine.destroy()
    }

    @Test
    fun generateProfileProducesDeterministicUniqueResults() {
        val artwork1 = Artwork(
            id = "genart_aurora_01",
            title = "Aurora Borealis",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        val artwork2 = Artwork(
            id = "photo_ocean_02",
            title = "Deep Blue Ocean",
            sourceId = "unsplash",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/ocean.jpg",
        )

        val profile1 = ZenAudioEngine.generateProfile(artwork1)
        val profile1Repeat = ZenAudioEngine.generateProfile(artwork1)
        val profile2 = ZenAudioEngine.generateProfile(artwork2)

        // Deterministic repeat check
        assertEquals(profile1.bpm, profile1Repeat.bpm, 0.001)
        assertEquals(profile1.scaleNotes.size, profile1Repeat.scaleNotes.size)
        for (i in profile1.scaleNotes.indices) {
            assertEquals(profile1.scaleNotes[i], profile1Repeat.scaleNotes[i], 0.01f)
        }

        // Difference across distinct artworks (bpm or detuneFactor)
        assertTrue(profile1.chordFrequencies.isNotEmpty())
        assertTrue(profile2.chordFrequencies.isNotEmpty())
        assertNotEquals(profile1.bpm, profile2.bpm, 0.001)
    }

    @Test
    fun setArtworkUpdatesActiveProfileAndSupportsChimes() {
        settings.setEnabled(true)
        val engine = ZenAudioEngine(context, settings)
        val artwork = Artwork(
            id = "genart_sunshine_01",
            title = "Golden Sunshine",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        engine.setArtwork(artwork)
        engine.start()
        engine.triggerChime()
        engine.stop()
        engine.destroy()
    }
}
