package fr.geoking.arthur.source

import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.ArtUniverse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UniverseSettingsTest {

    @Test
    fun startsUnset_thenPersistsActiveUniverseAndSwitchFlag() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("arthur_universe", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val settings = UniverseSettings(context)
        assertNull(settings.activeUniverse.value)
        assertFalse(settings.hasSwitchedOnce.value)

        settings.setActiveUniverse(ArtUniverse.Gallery, isFirstSelection = true)
        assertEquals(ArtUniverse.Gallery, settings.activeUniverse.value)
        assertFalse(settings.hasSwitchedOnce.value)

        settings.setActiveUniverse(ArtUniverse.Generative)
        assertEquals(ArtUniverse.Generative, settings.activeUniverse.value)
        assertTrue(settings.hasSwitchedOnce.value)

        val reloaded = UniverseSettings(context)
        assertEquals(ArtUniverse.Generative, reloaded.activeUniverse.value)
        assertTrue(reloaded.hasSwitchedOnce.value)
    }
}
