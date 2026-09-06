package fr.geoking.arthur.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import fr.geoking.arthur.shared.domain.ArtworkKind

/**
 * Silent looping ambient video for phone / TV canvases.
 * Auto stays still-only and must not host this composable.
 * Displays error placeholder with warning when playback or loading fails.
 */
@Composable
fun AmbientVideoPlayer(
    url: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasError by remember(url) { mutableStateOf(false) }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = false
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    hasError = true
                }
            })
        }
    }

    DisposableEffect(url) {
        hasError = false
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        onDispose { }
    }

    LaunchedEffect(isActive, hasError) {
        if (isActive && !hasError) {
            player.play()
        } else {
            player.pause()
        }
    }

    DisposableEffect(url) {
        onDispose { player.release() }
    }

    if (hasError) {
        StillArtworkPlaceholder(
            kind = ArtworkKind.Video,
            showWarning = true,
            modifier = modifier,
        )
    } else {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
            modifier = modifier.fillMaxSize(),
        )
    }
}
