package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.hasDetailContent
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.source.StillImagePrefetcher
import fr.geoking.arthur.source.rememberArtworkImageCache
import fr.geoking.arthur.ui.components.ArtworkRenderer
import fr.geoking.arthur.ui.components.StillArtworkPlaceholder
import fr.geoking.arthur.ui.components.authorForDisplay
import fr.geoking.arthur.ui.components.sourceLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    isLoading: Boolean = false,
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

    val poolIds = remember(rotatePool) { rotatePool.map { it.id } }
    var current by remember(artwork?.id) {
        mutableStateOf(
            when {
                shouldRotate && artwork != null && rotatePool.any { it.id == artwork.id } -> artwork
                shouldRotate -> rotatePool.firstOrNull()
                else -> artwork
            },
        )
    }

    LaunchedEffect(poolIds) {
        val existingId = current?.id
        if (existingId == null) {
            current = when {
                shouldRotate && artwork != null && rotatePool.any { it.id == artwork.id } -> artwork
                shouldRotate -> rotatePool.firstOrNull()
                else -> artwork
            }
        } else {
            val updatedInPool = rotatePool.firstOrNull { it.id == existingId }
            if (updatedInPool != null) {
                current = updatedInPool
            } else if (shouldRotate) {
                current = rotatePool.firstOrNull()
            }
        }
    }
    var rotationEpoch by remember { mutableIntStateOf(0) }
    var seenIds by remember(rotatePool.map { it.id }) { mutableStateOf(emptySet<String>()) }
    val progress = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
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
    val imageCache = rememberArtworkImageCache()
    val shown = if (shouldRotate) current else artwork
    var showDetails by remember { mutableStateOf(false) }
    LaunchedEffect(shown?.id) { showDetails = false }
    val ambientActive = isActive && !showDetails

    LaunchedEffect(shown?.id, poolIds, ambientActive) {
        if (!ambientActive) return@LaunchedEffect
        val currentArt = shown ?: return@LaunchedEffect
        val pool = latestPool
        withContext(Dispatchers.IO) {
            StillImagePrefetcher.ensureCached(imageCache, currentArt)
            if (pool.size < 2) return@withContext
            val index = pool.indexOfFirst { it.id == currentArt.id }.let { if (it < 0) 0 else it }
            val next = pool[AmbientAlbumArt.advanceIndex(index, pool.size)]
            val prev = pool[Math.floorMod(index - 1, pool.size)]
            StillImagePrefetcher.ensureCached(imageCache, next)
            StillImagePrefetcher.ensureCached(imageCache, prev)
            val covered = setOf(currentArt.id, next.id, prev.id)
            val timerCandidate = pool.filter { it.id !in covered }.randomOrNull(Random.Default)
            if (timerCandidate != null) {
                StillImagePrefetcher.ensureCached(imageCache, timerCandidate)
            }
        }
    }

    LaunchedEffect(rotationEpoch, ambientActive, shouldRotate, poolIds, intervalMs) {
        if (!ambientActive || !shouldRotate) {
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

    val shownTitle = shown?.title ?: title
    val sourceLabel = shown?.sourceLabel().orEmpty()
    val authorLabel = shown?.authorForDisplay(sourceLabel).orEmpty()
    val canOpenDetails = shown?.hasDetailContent() == true
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    if (showDetails && shown != null) {
        ArtworkDetailScreen(
            artwork = shown,
            onDismiss = { showDetails = false },
            modifier = modifier,
        )
        return
    }

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
                    isActive = ambientActive,
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
            if (sourceLabel.isNotEmpty()) {
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("ambient_source"),
                )
            }
            if (authorLabel.isNotEmpty()) {
                Text(
                    text = authorLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("ambient_author"),
                )
            }
            Text(
                text = shownTitle,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("ambient_title"),
            )
        }
        if (shouldRotate && ambientActive) {
            val currentIndex = remember(shown?.id, rotatePool) {
                val idx = rotatePool.indexOfFirst { it.id == shown?.id }
                if (idx < 0) 1 else idx + 1
            }
            val totalCount = rotatePool.size
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "$currentIndex / $totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.testTag("ambient_counter"),
                )
                AmbientRotationProgress(
                    progress = { progress.value },
                )
            }
        }
        // Above artwork chrome, below details button so ⋯ stays tappable.
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
        if (canOpenDetails) {
            AmbientDetailsButton(
                onClick = { showDetails = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun AmbientDetailsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.ambient_artwork_details)
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .testTag("ambient_details"),
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
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
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
