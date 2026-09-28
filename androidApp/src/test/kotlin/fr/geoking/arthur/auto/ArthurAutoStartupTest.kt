package fr.geoking.arthur.auto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/**
 * Boots the real Koin graph the same way Android does at process start (via
 * `ArthurApp`, Robolectric's manifest Application) and exercises the Auto
 * services end to end without a device — a regression net for AA startup
 * crashes (Koin wiring, Media3 MediaLibrarySession setup, coroutine kickoff).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArthurAutoStartupTest {

    @Test
    fun mediaService_bootsWithRealKoinGraph() {
        val controller = Robolectric.buildService(ArthurMediaService::class.java).create()
        val service = controller.get()
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull(service)
        assertEquals(ArthurMediaBrowse.ROOT, ArthurMediaService.ROOT)

        controller.destroy()
    }
}
