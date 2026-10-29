package fr.geoking.arthur.audio

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneListenBridgePlayerTest {

    @Test
    fun publish_setsTitleAndPlayWhenReady() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        var fromNotification: Boolean? = null
        val player = PhoneListenBridgePlayer(Looper.getMainLooper()) { playing ->
            fromNotification = playing
        }

        player.publish(title = "Zen Track", subtitle = "Artist", playing = true)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Zen Track", player.mediaMetadata.title?.toString())
        assertEquals("Artist", player.mediaMetadata.artist?.toString())
        assertTrue(player.playWhenReady)
        assertEquals(Player.STATE_READY, player.playbackState)

        player.playWhenReady = false
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertFalse(player.playWhenReady)
        assertEquals(false, fromNotification)

        player.release()
    }
}
