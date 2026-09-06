package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.ui.components.ArtworkRenderer
import fr.geoking.arthur.ui.components.StillArtworkPlaceholder
import kotlin.math.abs
import kotlin.random.Random

/**
 * Fullscreen ambient surface.
 *
 * - Control Plane Start: pass the selected [artwork] and the filtered catalog as
 *   [rotationPool] (≥2 items) for random rotation every [intervalMs].
 * - Pin only: empty [rotationPool].
 * - Dream / screensaver: pass a multi-item [rotationPool] to rotate the same way.
 * - [onNeedRenewPool]: after every pool id has been shown once, request a fresh
 *   API sample (museums / stock). Parent replaces [rotationPool].
 *
 * Navigation while rotating:
 * - Phone: tap left half = previous, tap right half = next; swipe left = next,
 *   swipe right = previous
 * - TV: D-pad / arrow left & right
 *
 * Missing or unloadable assets render the category placeholder + warning — never a
 * silent swap to an unrelated genart engine (e.g. pond ripples).
 */
@Composable
fun AmbientScreenContent(
    title: String,
    modifier: Modifier = Modifier,
    artwork: Artwork? = null,
    rotationPool: List<Artwork> = emptyList(),
    isActive: Boolean = true,
    intervalMs: Long = AmbientAlbumArt.ROTATION_INTERVAL_MS,
    onNeedRenewPool: (() -> Unit)? = null,
) {
    val rotatePool = remember(rotationPool) {
        rotationPool.filter { it.isAmbientDisplayable() }
    }
    val shouldRotate = rotatePool.size >= 2
    val configuration = LocalConfiguration.current
    val isTelevision =
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

    var current by remember(artwork?.id, rotatePool.map { it.id }, shouldRotate) {
        mutableStateOf(
            when {
                shouldRotate && artwork != null && rotatePool.any { it.id == artwork.id } -> artwork
                shouldRotate -> rotatePool.first()
                else -> artwork
            },
        )
    }
    var rotationEpoch by remember { mutableIntStateOf(0) }
    var seenIds by remember(rotatePool.map { it.id }) { mutableStateOf(emptySet<String>()) }
    val progress = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    val poolIds = remember(rotatePool) { rotatePool.map { it.id } }
    val latestCurrent by rememberUpdatedState(current)
    val latestPool by rememberUpdatedState(rotatePool)
    val renewLatest by rememberUpdatedState(onNeedRenewPool)

    fun advance(delta: Int, random: Boolean) {
        if (!shouldRotate) return
        val pool = latestPool
        val shownId = latestCurrent?.id
        val index = pool.indexOfFirst { it.id == shownId }.let { if (it < 0) 0 else it }
        current = if (random) {
            pool.filter { it.id != shownId }.randomOrNull(Random.Default)
                ?: pool.random(Random.Default)
        } else if (delta >= 0) {
            pool[AmbientAlbumArt.advanceIndex(index, pool.size)]
        } else {
            pool[Math.floorMod(index - 1, pool.size)]
        }
        val nextId = current?.id
        if (nextId != null) {
            val nextSeen = seenIds + nextId
            seenIds = nextSeen
            if (renewLatest != null && poolIds.all { it in nextSeen }) {
                seenIds = emptySet()
                renewLatest?.invoke()
            }
        }
        rotationEpoch++
    }

    val advanceLatest by rememberUpdatedState(::advance)

    LaunchedEffect(rotationEpoch, isActive, shouldRotate, poolIds, intervalMs) {
        if (!isActive || !shouldRotate) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = intervalMs.toInt().coerceAtLeast(1),
                easing = LinearEasing,
            ),
        )
        advanceLatest(+1, true)
    }

    LaunchedEffect(isTelevision, shouldRotate) {
        if (isTelevision && shouldRotate) {
            focusRequester.requestFocus()
        }
    }

    val shown = if (shouldRotate) current else artwork
    val shownTitle = shown?.title ?: title
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ambient_screen")
            .then(
                if (shouldRotate && isTelevision) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                            when (event.key) {
                                Key.DirectionRight, Key.MediaSkipForward -> {
                                    advanceLatest(+1, false)
                                    true
                                }
                                Key.DirectionLeft, Key.MediaSkipBackward -> {
                                    advanceLatest(-1, false)
                                    true
                                }
                                else -> false
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            shown != null -> {
                ArtworkRenderer(
                    artwork = shown,
                    isActive = isActive,
                    quality = GenartQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                StillArtworkPlaceholder(
                    kind = ArtworkKind.Genart,
                    showWarning = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.ambient_title),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = shownTitle,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("ambient_title"),
            )
        }
        if (shouldRotate && isActive) {
            AmbientRotationProgress(
                progress = { progress.value },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(28.dp),
            )
        }
        // Topmost layer so taps/swipes aren't eaten by artwork / overlays.
        if (shouldRotate && !isTelevision) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(rotationEpoch, poolIds, swipeThresholdPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            var totalDragX = 0f
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: break
                                if (change.changedToUp()) break
                                totalDragX += change.positionChange().x
                                change.consume()
                            }
                            when {
                                abs(totalDragX) >= swipeThresholdPx -> {
                                    if (totalDragX < 0f) {
                                        advanceLatest(+1, false)
                                    } else {
                                        advanceLatest(-1, false)
                                    }
                                }
                                startX < size.width / 2f -> advanceLatest(-1, false)
                                else -> advanceLatest(+1, false)
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun AmbientRotationProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.ambient_rotation_progress)
    Box(
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = description }
            .testTag("ambient_rotation_progress"),
        contentAlignment = Alignment.Center,
    ) {
        val frosted = Modifier
            .matchParentSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(12.dp)
                } else {
                    Modifier
                },
            )
            .background(Color.White.copy(alpha = 0.18f), CircleShape)
        Box(modifier = frosted)
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.28f),
            strokeWidth = 2.5.dp,
        )
    }
}

private fun Artwork.isAmbientDisplayable(): Boolean =
    isGenerative || !remoteUrl.isNullOrBlank() || !localPath.isNullOrBlank()
