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
    fun intervals_defaultToTwentySeconds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.phoneIntervalMs.value)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.tvIntervalMs.value)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.autoIntervalMs.value)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.intervalMs.value)
    }

    @Test
    fun intervals_persistIndependently() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        settings.setPhoneIntervalMs(60_000L)
        settings.setTvIntervalMs(300_000L)
        settings.setAutoIntervalMs(600_000L)

        assertEquals(60_000L, settings.phoneIntervalMs.value)
        assertEquals(300_000L, settings.tvIntervalMs.value)
        assertEquals(600_000L, settings.autoIntervalMs.value)

        val reloaded = RotationSettings(context)
        assertEquals(60_000L, reloaded.phoneIntervalMs.value)
        assertEquals(300_000L, reloaded.tvIntervalMs.value)
        assertEquals(600_000L, reloaded.autoIntervalMs.value)
    }

    @Test
    fun intervals_rejectUnknownValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        settings.setPhoneIntervalMs(7_000L)
        settings.setTvIntervalMs(7_000L)
        settings.setAutoIntervalMs(7_000L)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.phoneIntervalMs.value)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.tvIntervalMs.value)
        assertEquals(AmbientAlbumArt.ROTATION_INTERVAL_MS, settings.autoIntervalMs.value)
    }

    @Test
    fun phoneInterval_fallsBackToLegacyKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE)
        prefs.edit().clear().putLong("interval_ms", 120_000L).commit()

        val settings = RotationSettings(context)
        assertEquals(120_000L, settings.phoneIntervalMs.value)
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

    @Test
    fun wifiOnly_defaultsOffAndPersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        assertEquals(false, settings.wifiOnlyRemoteStills.value)
        settings.setWifiOnlyRemoteStills(true)
        assertEquals(true, RotationSettings(context).wifiOnlyRemoteStills.value)
    }

    @Test
    fun recentStillRing_keepsMostRecentFirstAndCaps() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("arthur_rotation", Context.MODE_PRIVATE).edit().clear().commit()
        val settings = RotationSettings(context)
        for (i in 1..15) {
            settings.recordRecentStillId("art-$i")
        }
        val recent = settings.recentStillIds()
        assertEquals(RotationSettings.RECENT_RING_SIZE, recent.size)
        assertEquals("art-15", recent.first())
        assertEquals(false, recent.contains("art-1"))
        assertEquals(false, recent.contains("art-2"))
        assertEquals(false, recent.contains("art-3"))
    }
}
