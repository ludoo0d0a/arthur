package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.ZenAudioEngine
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.hasDetailContent
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.Quote
import fr.geoking.arthur.source.RemoteStillNetworkGate
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.StillImagePrefetcher
import fr.geoking.arthur.source.rememberArtworkImageCache
import fr.geoking.arthur.source.rememberQuoteRepository
import fr.geoking.arthur.source.rememberQuoteSettings
import fr.geoking.arthur.ui.components.ArtworkRenderer
import fr.geoking.arthur.ui.components.StillArtworkPlaceholder
import fr.geoking.arthur.ui.components.authorForDisplay
import fr.geoking.arthur.ui.components.sourceLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import kotlin.math.abs

/**
 * Fullscreen ambient surface.
 *
 * - Control Plane Start: pass the selected [artwork] and the filtered catalog as
 *   [rotationPool] (≥2 items, capped at [AmbientAlbumArt.MAX_AUTO_ROTATION_POOL])
 *   for random rotation every [intervalMs].
 * - Pin only: empty [rotationPool].
 * - Dream / screensaver: pass a multi-item [rotationPool] to rotate the same way.
 * - [onNeedRenewPool]: after every pool id has been shown once, request a fresh
 *   API sample (museums / stock). Parent replaces [rotationPool].
 *
 * Navigation while rotating:
 * - Sound on (phone & TV): one media row — previous / play-pause / next.
 *   Phone also keeps swipe; TV focuses play-pause (D-pad left/right between
 *   controls, OK to activate). Media keys still work as shortcuts.
 * - Sound off on TV: D-pad left/right skips; MediaPlayPause toggles rotation
 * - Sound off on phone: swipe left = next, swipe right = previous
 *
 * Display interval starts only after the still is ready (loader time excluded).
 * The next still is warmed into disk cache before it becomes current.
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
        val displayable = rotationPool.filter { it.isAmbientDisplayable() }
        if (displayable.isNotEmpty() && displayable.all { it.isGenerative }) {
            displayable
        } else {
            displayable.take(AmbientAlbumArt.MAX_AUTO_ROTATION_POOL)
        }
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
    var isPlaying by remember { mutableStateOf(true) }
    var displayReady by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val latestCurrent by rememberUpdatedState(current)
    val latestPool by rememberUpdatedState(rotatePool)
    val renewLatest by rememberUpdatedState(onNeedRenewPool)
    val imageCache = rememberArtworkImageCache()
    val rotationSettings = remember {
        runCatching { GlobalContext.get().get<RotationSettings>() }.getOrNull()
    }
    val networkGate = remember {
        runCatching { GlobalContext.get().get<RemoteStillNetworkGate>() }.getOrNull()
    }
    val invalidStore = remember {
        runCatching { GlobalContext.get().get<InvalidArtworkStore>() }.getOrNull()
    }
    val ambientAudioSettings = remember {
        runCatching { GlobalContext.get().get<AmbientAudioSettings>() }.getOrNull()
    }
    val audioEnabled = ambientAudioSettings?.enabled?.collectAsState()?.value == true
    val showMediaPlayer = audioEnabled
    val tvUsesRootKeys = isTelevision && shouldRotate && !showMediaPlayer
    val context = LocalContext.current
    val zenAudio = remember(ambientAudioSettings) {
        ambientAudioSettings?.let { ZenAudioEngine(context, it) }
    }
    DisposableEffect(zenAudio) {
        onDispose { zenAudio?.destroy() }
    }
    LaunchedEffect(isPlaying, isActive, audioEnabled, current?.id) {
        if (isPlaying && isActive && audioEnabled) {
            current?.let { zenAudio?.setArtwork(it) }
            zenAudio?.start()
        } else {
            zenAudio?.stop()
        }
    }

    fun eligibleIdsForPick(pool: List<Artwork>): Set<String> {
        val invalid = invalidStore?.snapshot().orEmpty()
        val cacheOnly = networkGate?.isCacheOnlyMode() == true
        return pool.mapNotNull { art ->
            if (art.id in invalid) return@mapNotNull null
            if (cacheOnly && !art.isOfflineDisplayable(imageCache)) return@mapNotNull null
            art.id
        }.toSet()
    }

    suspend fun advance(delta: Int) {
        if (!shouldRotate) return
        val pool = latestPool
        val shownId = latestCurrent?.id
        val eligible = eligibleIdsForPick(pool)
        if (eligible.isEmpty()) return
        val index = pool.indexOfFirst { it.id == shownId }.let { if (it < 0) 0 else it }
        var steps = 0
        var idx = index
        var candidate: Artwork
        do {
            idx = if (delta >= 0) {
                AmbientAlbumArt.advanceIndex(idx, pool.size)
            } else {
                Math.floorMod(idx - 1, pool.size)
            }
            candidate = pool[idx]
            steps++
        } while (candidate.id !in eligible && steps < pool.size)
        if (candidate.id !in eligible) return
        val nextArt = candidate
        val allowNetwork = networkGate?.canDownloadRemoteStill() ?: true
        withContext(Dispatchers.IO) {
            StillImagePrefetcher.ensureCached(imageCache, nextArt, allowNetwork = allowNetwork)
        }
        displayReady = false
        current = nextArt
        zenAudio?.setArtwork(nextArt)
        zenAudio?.triggerTransition()
        val nextId = nextArt.id
        if (!nextArt.remoteUrl.isNullOrBlank()) {
            rotationSettings?.recordRecentStillId(nextId)
        }
        val nextSeen = seenIds + nextId
        seenIds = nextSeen
        val unseenCount = poolIds.count { it !in nextSeen }
        if (unseenCount <= 1 || poolIds.all { it in nextSeen }) {
            if (poolIds.all { it in nextSeen }) {
                seenIds = emptySet()
            }
            renewLatest?.invoke()
        }
        rotationEpoch++
    }

    val advanceLatest by rememberUpdatedState(::advance)
    val quoteRepository = rememberQuoteRepository()
    val quoteSettings = rememberQuoteSettings()
    val showQuotes by quoteSettings.showQuotes.collectAsState()
    val quoteProvider by quoteSettings.provider.collectAsState()
    var quotesById by remember { mutableStateOf<Map<String, Quote>>(emptyMap()) }
    val shown = if (shouldRotate) current else artwork
    val slideQuote = if (showQuotes) shown?.id?.let { quotesById[it] } else null
    var showDetails by remember { mutableStateOf(false) }
    LaunchedEffect(shown?.id) {
        displayReady = false
        showDetails = false
    }
    val ambientActive = isActive && !showDetails

    LaunchedEffect(poolIds, artwork?.id, showQuotes, quoteProvider) {
        if (!showQuotes) {
            quotesById = emptyMap()
            return@LaunchedEffect
        }
        val ids = poolIds.ifEmpty { listOfNotNull(artwork?.id) }
        if (ids.isEmpty()) {
            quotesById = emptyMap()
            return@LaunchedEffect
        }
        quotesById = withContext(Dispatchers.IO) {
            runCatching { quoteRepository.quotesForArtworks(ids) }.getOrDefault(emptyMap())
        }
    }

    LaunchedEffect(shown?.id, poolIds, ambientActive) {
        if (!ambientActive) return@LaunchedEffect
        val currentArt = shown ?: return@LaunchedEffect
        val pool = latestPool
        val allowNetwork = networkGate?.canDownloadRemoteStill() ?: true
        withContext(Dispatchers.IO) {
            StillImagePrefetcher.ensureCached(imageCache, currentArt, allowNetwork = allowNetwork)
            if (pool.size < 2) return@withContext
            val index = pool.indexOfFirst { it.id == currentArt.id }.let { if (it < 0) 0 else it }
            val next = pool[AmbientAlbumArt.advanceIndex(index, pool.size)]
            val prev = pool[Math.floorMod(index - 1, pool.size)]
            StillImagePrefetcher.ensureCached(imageCache, next, allowNetwork = allowNetwork)
            StillImagePrefetcher.ensureCached(imageCache, prev, allowNetwork = allowNetwork)
        }
    }

    // Interval counts only while the current slide is visible — not during still download.
    LaunchedEffect(rotationEpoch, ambientActive, shouldRotate, isPlaying, displayReady, poolIds, intervalMs) {
        if (!ambientActive || !shouldRotate || !isPlaying || !displayReady) {
            if (!isPlaying || !ambientActive || !shouldRotate) {
                progress.snapTo(0f)
            }
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
        advanceLatest(+1)
    }

    LaunchedEffect(tvUsesRootKeys) {
        if (tvUsesRootKeys) {
            rootFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(isTelevision, showMediaPlayer) {
        if (isTelevision && showMediaPlayer) {
            playPauseFocusRequester.requestFocus()
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
                if (tvUsesRootKeys) {
                    Modifier
                        .focusRequester(rootFocusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                            when (event.key) {
                                Key.DirectionRight, Key.MediaSkipForward -> {
                                    scope.launch { advanceLatest(+1) }
                                    true
                                }
                                Key.DirectionLeft, Key.MediaSkipBackward -> {
                                    scope.launch { advanceLatest(-1) }
                                    true
                                }
                                Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                                    isPlaying = !isPlaying
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
                    onDisplayReady = { displayReady = true },
                    onStillFailed = {
                        if (shouldRotate) {
                            scope.launch { advanceLatest(+1) }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LaunchedEffect(Unit) { displayReady = true }
                StillArtworkPlaceholder(
                    kind = shown?.kind
                        ?: rotatePool.firstOrNull()?.kind
                        ?: ArtworkKind.Photo,
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
                .padding(28.dp)
                .padding(bottom = if (showMediaPlayer) 72.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val quote = slideQuote
            if (quote != null) {
                Text(
                    text = "\u201C${quote.text}\u201D",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("ambient_quote"),
                )
                if (quote.author.isNotBlank()) {
                    Text(
                        text = "\u2014 ${quote.author}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("ambient_quote_author"),
                    )
                }
            }
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
        // Swipe layer under chrome controls so icons stay tappable.
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
                                        scope.launch { advanceLatest(+1) }
                                    } else {
                                        scope.launch { advanceLatest(-1) }
                                    }
                                }
                                // Half-screen tap kept as fallback; icons are primary.
                                startX < size.width / 2f -> scope.launch { advanceLatest(-1) }
                                else -> scope.launch { advanceLatest(+1) }
                            }
                        }
                    },
            )
        }
        val showTransportChrome = shouldRotate || showMediaPlayer
        if (showTransportChrome) {
            val currentIndex = remember(shown?.id, rotatePool) {
                val idx = rotatePool.indexOfFirst { it.id == shown?.id }
                if (idx < 0) 1 else idx + 1
            }
            val totalCount = rotatePool.size
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shouldRotate) {
                    Text(
                        text = "$currentIndex / $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.testTag("ambient_counter"),
                    )
                }
                if (showMediaPlayer) {
                    AmbientMediaPlayerBar(
                        isPlaying = isPlaying,
                        canSkip = shouldRotate,
                        progress = progress.value,
                        onPrevious = { scope.launch { advanceLatest(-1) } },
                        onPlayPause = { isPlaying = !isPlaying },
                        onNext = { scope.launch { advanceLatest(+1) } },
                        playPauseFocusRequester = if (isTelevision) {
                            playPauseFocusRequester
                        } else {
                            null
                        },
                    )
                }
            }
        }
        // Keep details out of the TV focus path when the media row is present —
        // one horizontal control strip is enough for D-pad.
        if (canOpenDetails && !(isTelevision && showMediaPlayer)) {
            AmbientDetailsButton(
                onClick = { showDetails = true },
                focusable = !isTelevision,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )
        }
        if (!isTelevision && ambientAudioSettings != null) {
            AmbientSoundToggleButton(
                enabled = audioEnabled,
                onToggle = { ambientAudioSettings.setEnabled(!audioEnabled) },
                modifier = Modifier
                    .align(if (canOpenDetails) Alignment.TopStart else Alignment.TopEnd)
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
    focusable: Boolean = true,
) {
    val description = stringResource(R.string.ambient_artwork_details)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .testTag("ambient_details"),
        contentAlignment = Alignment.Center,
    ) {
        AmbientFrostedCircle()
        IconButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .focusProperties { canFocus = focusable }
                .border(
                    width = if (focused) 3.dp else 0.dp,
                    color = if (focused) Color.White else Color.Transparent,
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun AmbientMediaPlayerBar(
    isPlaying: Boolean,
    canSkip: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    playPauseFocusRequester: FocusRequester? = null,
) {
    val previousDescription = stringResource(R.string.ambient_previous)
    val playPauseDescription = stringResource(
        if (isPlaying) R.string.ambient_pause else R.string.ambient_play,
    )
    val nextDescription = stringResource(R.string.ambient_next)
    Box(
        modifier = modifier
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.MediaSkipBackward -> {
                        if (canSkip) onPrevious()
                        true
                    }
                    Key.MediaSkipForward -> {
                        if (canSkip) onNext()
                        true
                    }
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        onPlayPause()
                        true
                    }
                    else -> false
                }
            }
            .testTag("ambient_media_player"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(12.dp)
                    } else {
                        Modifier
                    },
                )
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        ) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 2.dp)
                    .fillMaxWidth(0.75f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .testTag("ambient_media_progress"),
                color = Color.White.copy(alpha = 0.9f),
                trackColor = Color.White.copy(alpha = 0.25f),
            )
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                AmbientMediaControlButton(
                    onClick = onPrevious,
                    enabled = canSkip,
                    contentDescription = previousDescription,
                    testTag = "ambient_media_previous",
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_car_previous),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (canSkip) 0.9f else 0.35f),
                    )
                }
                AmbientMediaControlButton(
                    onClick = onPlayPause,
                    enabled = true,
                    contentDescription = playPauseDescription,
                    testTag = "ambient_media_play_pause",
                    focusRequester = playPauseFocusRequester,
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                        ),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                    )
                }
                AmbientMediaControlButton(
                    onClick = onNext,
                    enabled = canSkip,
                    contentDescription = nextDescription,
                    testTag = "ambient_media_next",
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_car_next),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (canSkip) 0.9f else 0.35f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientMediaControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = CircleShape,
            )
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag),
    ) {
        content()
    }
}

@Composable
private fun AmbientSoundToggleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (enabled) R.string.settings_ambient_sound_enable else R.string.cd_media_player,
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .testTag("ambient_sound_toggle"),
        contentAlignment = Alignment.Center,
    ) {
        AmbientFrostedCircle()
        IconButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (enabled) R.drawable.ic_car_sound_on else R.drawable.ic_car_sound_off,
                ),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun AmbientFrostedCircle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(12.dp)
                } else {
                    Modifier
                },
            )
            .background(Color.White.copy(alpha = 0.18f), CircleShape),
    )
}

private fun Artwork.isAmbientDisplayable(): Boolean =
    isGenerative || !remoteUrl.isNullOrBlank() || !localPath.isNullOrBlank()

/** Offline-ready for cache-only Ambient: generative, local file, or decodable disk cache. */
private fun Artwork.isOfflineDisplayable(cache: fr.geoking.arthur.source.ArtworkImageCache): Boolean {
    if (isGenerative) return true
    if (!localPath.isNullOrBlank()) return true
    return cache.hasDecodableImage(id)
}
