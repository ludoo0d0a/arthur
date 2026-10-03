package fr.geoking.arthur.auto

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmbientMediaPlayerTest {
    private val art = Artwork(
        id = "genart.waves",
        title = "Waves",
        sourceId = "genart",
        kind = ArtworkKind.Genart,
    )

    private val noopCallbacks = object : AmbientMediaPlayer.Callbacks {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean) = Unit
        override fun onSkip(delta: Int) = Unit
        override fun onPlayMediaId(mediaId: String) = Unit
    }

    @Test
    fun publish_withoutDuration_leavesDurationUnset() {
        val player = AmbientMediaPlayer(Looper.getMainLooper(), noopCallbacks)
        player.publish(
            art = art,
            artworkUri = Uri.parse("content://test/art"),
            subtitle = "GeoKing",
            playing = true,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(C.TIME_UNSET, player.duration)
    }

    @Test
    fun publish_withDuration_exposesDurationMs() {
        val player = AmbientMediaPlayer(Looper.getMainLooper(), noopCallbacks)
        player.publish(
            art = art,
            artworkUri = Uri.parse("content://test/art"),
            subtitle = "GeoKing",
            playing = true,
            durationMs = 20_000L,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(20_000L, player.duration)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    fun setPlaying_false_freezesProgressPosition() {
        val player = AmbientMediaPlayer(Looper.getMainLooper(), noopCallbacks)
        player.publish(
            art = art,
            artworkUri = Uri.parse("content://test/art"),
            subtitle = "GeoKing",
            playing = true,
            durationMs = 20_000L,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        player.setPlaying(false)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val frozen = player.currentPosition
        assertEquals(frozen, player.currentPosition)
        Thread.sleep(30)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(frozen, player.currentPosition)
    }

    @Test
    fun publish_withTitleOverride_exposesQuoteAsTitle() {
        val player = AmbientMediaPlayer(Looper.getMainLooper(), noopCallbacks)
        player.publish(
            art = art,
            artworkUri = Uri.parse("content://test/art"),
            title = "“Be yourself”",
            subtitle = "— Oscar Wilde",
            playing = true,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val meta = player.mediaMetadata
        assertEquals("“Be yourself”", meta.title?.toString())
        assertEquals("— Oscar Wilde", meta.artist?.toString())
    }
}
