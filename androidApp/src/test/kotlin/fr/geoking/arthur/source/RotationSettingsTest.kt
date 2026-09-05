package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.auto.AmbientAlbumArt
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RotationSettingsTest {
    @Test
    fun interval_defaultsToTwentySeconds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.intervalMs.value)
    }

    @Test
    fun interval_persists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        settings.setIntervalMs(300_000L)
        assertEquals(300_000L, settings.intervalMs.value)
        assertEquals(300_000L, RotationSettings(context).intervalMs.value)
    }

    @Test
    fun interval_rejectsUnknownValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        settings.setIntervalMs(7_000L)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.intervalMs.value)
    }

    @Test
    fun options_includeTenSecondsThroughThirtyMinutes() {
        assertEquals(
            listOf(
                10_000L,
                20_000L,
                60_000L,
                120_000L,
                180_000L,
                240_000L,
                300_000L,
                600_000L,
                900_000L,
                1_200_000L,
                1_800_000L,
            ),
            RotationSettings.OPTIONS_MS,
        )
    }
}
