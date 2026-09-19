package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource
import fr.geoking.arthur.ui.components.MuseumTopic
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PhotoTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ScreensaverSettingsTest {
    @Test
    fun defaultPack_defaultsToNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_screensaver", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = ScreensaverSettings(context)
        assertNull(settings.defaultPack.value)
    }

    @Test
    fun defaultPack_persistsSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_screensaver", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = ScreensaverSettings(context)
        val selection = PackSelection(PackFamily.Photo, "nature")
        settings.setDefaultPack(selection)

        assertEquals(selection, settings.defaultPack.value)
        assertEquals(selection, ScreensaverSettings(context).defaultPack.value)
    }

    @Test
    fun defaultPack_clearsSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_screensaver", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = ScreensaverSettings(context)
        val selection = PackSelection(PackFamily.Genart, "fractal")
        settings.setDefaultPack(selection)
        assertEquals(selection, settings.defaultPack.value)

        settings.setDefaultPack(null)
        assertNull(settings.defaultPack.value)
        assertNull(ScreensaverSettings(context).defaultPack.value)
    }

    @Test
    fun defaultPack_migratesLegacyMuseumWikimediaToPhoto() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_screensaver", Context.MODE_PRIVATE).edit()
            .putString("default_pack_family", PackFamily.Museum.name)
            .putString("default_pack_sub_id", WikimediaStreetArtSource.ID)
            .commit()
        val settings = ScreensaverSettings(context)
        assertEquals(
            PackSelection(PackFamily.Photo, PhotoTopic.Wikimedia.testTagSuffix),
            settings.defaultPack.value,
        )
    }

    @Test
    fun defaultPack_migratesLegacyMuseumRandomToMet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_screensaver", Context.MODE_PRIVATE).edit()
            .putString("default_pack_family", PackFamily.Museum.name)
            .putString("default_pack_sub_id", "random")
            .commit()
        val settings = ScreensaverSettings(context)
        assertEquals(
            PackSelection(PackFamily.Museum, MuseumTopic.Met.testTagSuffix),
            settings.defaultPack.value,
        )
    }
}
