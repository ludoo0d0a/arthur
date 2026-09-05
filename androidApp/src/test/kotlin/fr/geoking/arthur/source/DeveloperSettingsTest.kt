package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeveloperSettingsTest {
    @Test
    fun simulatePremium_defaultsTrue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_developer", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = DeveloperSettings(context)
        assertTrue(settings.simulatePremium.value)
    }

    @Test
    fun simulatePremium_persists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_developer", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = DeveloperSettings(context)
        settings.setSimulatePremium(false)
        assertEquals(false, settings.simulatePremium.value)
        assertEquals(false, DeveloperSettings(context).simulatePremium.value)
    }
}
