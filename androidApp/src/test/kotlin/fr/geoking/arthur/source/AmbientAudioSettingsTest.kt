package fr.geoking.arthur.source

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(AmbientAudioMode.PAD_AND_CHIME, settings.mode.value)
    }

    @Test
    fun settingsArePersisted() {
        val settings = AmbientAudioSettings(context)
        settings.setEnabled(true)
        settings.setVolume(0.8f)
        settings.setMode(AmbientAudioMode.CHIME_ONLY)

        val reloaded = AmbientAudioSettings(context)
        assertTrue(reloaded.enabled.value)
        assertEquals(0.8f, reloaded.volume.value, 0.001f)
        assertEquals(AmbientAudioMode.CHIME_ONLY, reloaded.mode.value)
    }

    @Test
    fun volumeIsClamped() {
        val settings = AmbientAudioSettings(context)
        settings.setVolume(1.5f)
        assertEquals(1.0f, settings.volume.value, 0.001f)
        settings.setVolume(-0.2f)
        assertEquals(0.0f, settings.volume.value, 0.001f)
    }
}
