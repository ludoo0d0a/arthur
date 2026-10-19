package fr.geoking.arthur.update

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayStoreUpdatesTest {
    @Test
    fun openAppListing_startsMarketDetailsIntent() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val started = PlayStoreUpdates.openAppListing(app)
        assertTrue(started)
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("market", intent.data?.scheme)
        assertEquals("details", intent.data?.host)
        assertTrue(intent.dataString!!.contains("id=${app.packageName}"))
    }
}
