package fr.geoking.arthur.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.source.AmbientAudioMode
import fr.geoking.arthur.source.AmbientAudioSettings
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
}
