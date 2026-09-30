package fr.geoking.arthur.source

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.MusicStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmbientAudioSettingsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("arthur_ambient_audio", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultValuesAreCorrect() {
        val settings = AmbientAudioSettings(context)
        assertFalse(settings.enabled.value)
        assertEquals(0.5f, settings.volume.value, 0.001f)
        assertEquals(AmbientAudioCharacter.Melody, settings.character.value)
        assertEquals(AmbientAudioMode.CHIME_ONLY, settings.mode.value)
        assertNull(settings.stylePreference.value)
        assertEquals(0.55f, settings.complexity.value, 0.001f)
    }

    @Test
    fun settingsArePersisted() {
        val settings = AmbientAudioSettings(context)
        settings.setEnabled(true)
        settings.setVolume(0.8f)
        settings.setCharacter(AmbientAudioCharacter.Atmosphere)
        settings.setStylePreference(MusicStyle.JazzPiano)
        settings.setComplexity(0.9f)

        val reloaded = AmbientAudioSettings(context)
        assertTrue(reloaded.enabled.value)
        assertEquals(0.8f, reloaded.volume.value, 0.001f)
        assertEquals(AmbientAudioCharacter.Atmosphere, reloaded.character.value)
        assertEquals(AmbientAudioMode.PAD_AND_CHIME, reloaded.mode.value)
        assertEquals(MusicStyle.JazzPiano, reloaded.stylePreference.value)
        assertEquals(0.9f, reloaded.complexity.value, 0.001f)
    }

    @Test
    fun volumeAndComplexityAreClamped() {
        val settings = AmbientAudioSettings(context)
        settings.setVolume(1.5f)
        assertEquals(1.0f, settings.volume.value, 0.001f)
        settings.setVolume(-0.2f)
        assertEquals(0.0f, settings.volume.value, 0.001f)
        settings.setComplexity(2f)
        assertEquals(1.0f, settings.complexity.value, 0.001f)
    }

    @Test
    fun legacyModeMigratesToCharacter() {
        context.getSharedPreferences("arthur_ambient_audio", Context.MODE_PRIVATE)
            .edit()
            .putString("audio_mode", AmbientAudioMode.PAD_AND_CHIME.name)
            .commit()
        val settings = AmbientAudioSettings(context)
        assertEquals(AmbientAudioCharacter.Balanced, settings.character.value)
    }
}
