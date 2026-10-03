package fr.geoking.arthur.ui.components

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.source.ArtworkImageCache
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class LoadStillBitmapTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun loadStillBitmap_failsFast_whenNoLocalAndRemoteBlank() {
        val cache = ArtworkImageCache(context)
        val result = loadStillBitmap(
            artworkId = "no-local-no-remote",
            localPath = null,
            remoteUrl = "",
            imageCache = cache,
            networkGate = null,
            errorLogger = null,
            invalidStore = null,
        )
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("No image URL") == true,
        )
    }
}
