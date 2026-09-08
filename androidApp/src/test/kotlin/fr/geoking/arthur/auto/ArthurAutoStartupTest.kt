package fr.geoking.arthur.auto

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Boots the real Koin graph the same way Android does at process start (via
 * `ArthurApp`, Robolectric's manifest Application) and exercises the Auto
 * services end to end without a device — a regression net for AA startup
 * crashes (Koin wiring, MediaSessionCompat setup, coroutine kickoff).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArthurAutoStartupTest {

    @Test
    fun mediaService_bootsWithRealKoinGraphAndExposesRoot() {
        val controller = Robolectric.buildService(ArthurMediaService::class.java).create()
        val service = controller.get()
        shadowOf(Looper.getMainLooper()).idle()

        val root = service.onGetRoot("com.google.android.projection.gearhead", 0, null)

        assertEquals(ArthurMediaBrowse.ROOT, root.rootId)

        controller.destroy()
    }
}
