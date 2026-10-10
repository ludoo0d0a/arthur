package fr.geoking.arthur.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork

private const val CrossfadeDurationPhoneMs = 400
private const val CrossfadeDurationTvMs = 1100

/**
 * Keeps the last successfully displayed artwork visible until [artwork] is ready,
 * then crossfades. On silent skip failure, [lastGood] is left untouched.
 */
@Composable
fun ArtworkTransitionHost(
    artwork: Artwork,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: GenartQuality = GenartQuality.High,
    silentFailure: Boolean = false,
    onDisplayReady: (() -> Unit)? = null,
    onStillFailed: (() -> Unit)? = null,
) {
    var lastGood by remember { mutableStateOf<Artwork?>(null) }
    var revealIncoming by remember { mutableStateOf(false) }
    var visibleId by remember { mutableStateOf<String?>(null) }

    val isTelevision =
        LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val crossfadeMs = if (isTelevision) CrossfadeDurationTvMs else CrossfadeDurationPhoneMs

    LaunchedEffect(artwork.id) {
        if (artwork.id != visibleId) {
            revealIncoming = false
        }
    }

    val back = lastGood
    val showingIncoming =
        revealIncoming || back == null || back.id == artwork.id
    val incomingAlpha by animateFloatAsState(
        targetValue = if (showingIncoming) 1f else 0f,
        animationSpec = tween(
            durationMillis = crossfadeMs,
            easing = FastOutSlowInEasing,
        ),
        label = "artwork_crossfade",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("artwork_transition_host"),
    ) {
        if (back != null && back.id != artwork.id && incomingAlpha < 1f) {
            ArtworkRenderer(
                artwork = back,
                // Prefer a single active generative engine; stills are cheap to keep warm.
                isActive = isActive && !showingIncoming,
                quality = quality,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Incoming is always composed (even at alpha 0) so load / failure effects run.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = incomingAlpha },
        ) {
            ArtworkRenderer(
                artwork = artwork,
                // Keep active while loading so still failure / ready effects always run,
                // even when drawn under lastGood at alpha 0.
                isActive = isActive,
                quality = quality,
                silentFailure = silentFailure,
                onDisplayReady = {
                    revealIncoming = true
                    lastGood = artwork
                    visibleId = artwork.id
                    onDisplayReady?.invoke()
                },
                onStillFailed = {
                    if (silentFailure) {
                        onStillFailed?.invoke()
                    } else {
                        revealIncoming = true
                        visibleId = artwork.id
                        onStillFailed?.invoke()
                        onDisplayReady?.invoke()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
