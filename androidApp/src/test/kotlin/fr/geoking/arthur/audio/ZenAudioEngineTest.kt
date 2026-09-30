package fr.geoking.arthur.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.source.AmbientAudioCharacter
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
        context.getSharedPreferences("arthur_ambient_audio", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        settings = AmbientAudioSettings(context)
    }

    @Test
    fun engineLifecycleWhenDisabledDoesNotStart() {
        settings.setEnabled(false)
        val engine = ZenAudioEngine(context, settings)
        engine.start()
        engine.setArtwork(Artwork("a", "A", sourceId = "x", kind = ArtworkKind.Photo))
        engine.triggerChime()
        engine.triggerTransition()
        engine.stop()
        engine.destroy()
    }

    @Test
    fun engineLifecycleWhenEnabledStartsAndStops() {
        settings.setEnabled(true)
        settings.setCharacter(AmbientAudioCharacter.Balanced)
        val engine = ZenAudioEngine(context, settings)
        engine.setArtwork(Artwork("genart.waves", "Waves", sourceId = "genart", kind = ArtworkKind.Genart))
        engine.start()
        engine.triggerTransition()
        engine.stop()
        engine.destroy()
    }

    @Test
    fun setArtworkAndTransitionSafeWhenStopped() {
        settings.setEnabled(true)
        val engine = ProceduralMusicEngine(context, settings)
        engine.setArtwork(Artwork("b", "B", sourceId = "y", kind = ArtworkKind.Painting))
        engine.triggerTransition()
        engine.destroy()
    }
}
