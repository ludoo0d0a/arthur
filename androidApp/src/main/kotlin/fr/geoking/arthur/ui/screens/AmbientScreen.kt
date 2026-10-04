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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import fr.geoking.arthur.ui.components.ArtworkTransitionHost
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
 * - [onNeedRenewPool]: when within [AmbientAlbumArt.POOL_RENEW_LEAD] of the playlist
 *   end (around the 19th/20th of a source page), request the next API sample.
 *   Parent should append new ids to [rotationPool].
 *
 * Navigation while rotating:
 * - Sound on (phone & TV): bottom icons — phone: play/pause + mute + info; TV: mute + info.
 *   Phone keeps swipe; TV focuses mute (OK toggles sound). D-pad left/right skips,
 *   up opens info. Media keys still work as shortcuts.
 * - Sound off on TV: D-pad left/right skips; MediaPlayPause toggles rotation; up = info
 * - Sound off on phone: bottom play/pause + mute (+ info); swipe left/right skips
 * - Phone chrome stays above the nav bar; quotes respect the status bar.
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
            displayable.take(AmbientAlbumArt.MAX_PLAYLIST_SIZE)
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
    // Preserve seen ids across playlist appends; only reset on a full pool replace.
    var seenIds by remember { mutableStateOf(emptySet<String>()) }
    /** Session-only ids that failed to display; avoided by [advance] until pool replace. */
    var failedIds by remember { mutableStateOf(emptySet<String>()) }
    var previousPoolIds by remember { mutableStateOf(emptyList<String>()) }
    var renewIssuedAtPoolSize by remember { mutableIntStateOf(-1) }
    LaunchedEffect(poolIds) {
        val prev = previousPoolIds.toSet()
        val next = poolIds.toSet()
        previousPoolIds = poolIds
        if (prev.isEmpty()) return@LaunchedEffect
        val isAppend = next.containsAll(prev) && next.size >= prev.size
        seenIds = if (isAppend) seenIds.intersect(next) else emptySet()
        if (!isAppend) {
            renewIssuedAtPoolSize = -1
            failedIds = emptySet()
        }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var displayReady by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val rootFocusRequester = remember { FocusRequester() }
    val soundFocusRequester = remember { FocusRequester() }
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
    // audioEnabled implies settings are present (null settings → audioEnabled is false).
    val bottomAudioSettings = ambientAudioSettings?.takeIf { audioEnabled }
    val showBottomChrome = bottomAudioSettings != null
    val tvUsesRootKeys = isTelevision && shouldRotate && !showBottomChrome
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

    suspend fun advance(delta: Int, skipId: String? = null) {
        if (!shouldRotate) return
        val pool = latestPool
        val shownId = latestCurrent?.id
        val eligible = eligibleIdsForPick(pool)
        if (eligible.isEmpty()) return
        if (skipId != null) {
            failedIds = failedIds + skipId
        }
        val allowNetwork = networkGate?.canDownloadRemoteStill() ?: true
        val nextArt = withContext(Dispatchers.IO) {
            pickNextReadyArtwork(
                pool = pool,
                fromId = shownId,
                delta = delta,
                skipIds = failedIds,
                eligibleIds = eligible,
                imageCache = imageCache,
                allowNetwork = allowNetwork,
            )
        } ?: return
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
        val poolSize = poolIds.size
        if (AmbientAlbumArt.shouldPrefetchNextPool(nextSeen.size, poolSize) &&
            renewIssuedAtPoolSize != poolSize
        ) {
            renewIssuedAtPoolSize = poolSize
            renewLatest?.invoke()
        }
        rotationEpoch++
    }

    val advanceLatest by rememberUpdatedState<suspend (Int, String?) -> Unit>(
        newValue = { delta, skipId -> advance(delta, skipId) },
    )
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
        advanceLatest(+1, null)
    }

    LaunchedEffect(tvUsesRootKeys, showDetails) {
        if (tvUsesRootKeys && !showDetails) {
            rootFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(isTelevision, showBottomChrome, shown?.id, showDetails) {
        if (isTelevision && showBottomChrome && !showDetails) {
            soundFocusRequester.requestFocus()
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
                                    scope.launch { advanceLatest(+1, null) }
                                    true
                                }
                                Key.DirectionLeft, Key.MediaSkipBackward -> {
                                    scope.launch { advanceLatest(-1, null) }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (canOpenDetails) {
                                        showDetails = true
                                        true
                                    } else {
                                        false
                                    }
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
                ArtworkTransitionHost(
                    artwork = shown,
                    isActive = ambientActive,
                    quality = GenartQuality.High,
                    silentFailure = shouldRotate,
                    onDisplayReady = { displayReady = true },
                    onStillFailed = {
                        if (shouldRotate) {
                            val failedId = shown.id
                            scope.launch { advanceLatest(+1, failedId) }
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
                        colors = listOf(
                            if (slideQuote != null) Color.Black.copy(alpha = 0.4f) else Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )
        val quote = slideQuote
        if (quote != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
        }
        val phoneBottomControls = !isTelevision && ambientAudioSettings != null
        val reserveBottomControls =
            showBottomChrome || phoneBottomControls || (shouldRotate && !isTelevision)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(28.dp)
                .padding(bottom = if (reserveBottomControls) 56.dp else 0.dp),
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
                                        scope.launch { advanceLatest(+1, null) }
                                    } else {
                                        scope.launch { advanceLatest(-1, null) }
                                    }
                                }
                                // Half-screen tap kept as fallback; icons are primary.
                                startX < size.width / 2f -> scope.launch { advanceLatest(-1, null) }
                                else -> scope.launch { advanceLatest(+1, null) }
                            }
                        }
                    },
            )
        }
        val showTransportChrome = shouldRotate || showBottomChrome || phoneBottomControls
        if (showTransportChrome) {
            val currentIndex = remember(shown?.id, rotatePool) {
                val idx = rotatePool.indexOfFirst { it.id == shown?.id }
                if (idx < 0) 1 else idx + 1
            }
            val totalCount = rotatePool.size
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
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
                if (bottomAudioSettings != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (shouldRotate && !isTelevision) {
                            AmbientPlayPauseButton(
                                isPlaying = isPlaying,
                                onToggle = { isPlaying = !isPlaying },
                            )
                        }
                        AmbientSoundToggleButton(
                            enabled = true,
                            onToggle = { bottomAudioSettings.setEnabled(false) },
                            focusRequester = if (isTelevision) soundFocusRequester else null,
                            onTvKeyEvent = if (isTelevision) {
                                { key ->
                                    when (key) {
                                        Key.DirectionRight, Key.MediaSkipForward -> {
                                            if (shouldRotate) {
                                                scope.launch { advanceLatest(+1, null) }
                                            }
                                            true
                                        }
                                        Key.DirectionLeft, Key.MediaSkipBackward -> {
                                            if (shouldRotate) {
                                                scope.launch { advanceLatest(-1, null) }
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            if (canOpenDetails) {
                                                showDetails = true
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                                            isPlaying = !isPlaying
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            } else {
                                null
                            },
                        )
                        if (canOpenDetails) {
                            AmbientDetailsButton(
                                onClick = { showDetails = true },
                                focusable = !isTelevision,
                            )
                        }
                    }
                } else if (phoneBottomControls && ambientAudioSettings != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (shouldRotate) {
                            AmbientPlayPauseButton(
                                isPlaying = isPlaying,
                                onToggle = { isPlaying = !isPlaying },
                            )
                        }
                        AmbientSoundToggleButton(
                            enabled = false,
                            onToggle = { ambientAudioSettings.setEnabled(true) },
                        )
                        if (canOpenDetails) {
                            AmbientDetailsButton(
                                onClick = { showDetails = true },
                                focusable = true,
                            )
                        }
                    }
                } else if (!isTelevision && shouldRotate) {
                    AmbientPlayPauseButton(
                        isPlaying = isPlaying,
                        onToggle = { isPlaying = !isPlaying },
                    )
                }
            }
        }
        if (canOpenDetails && !showBottomChrome && !phoneBottomControls) {
            AmbientDetailsButton(
                onClick = { showDetails = true },
                focusable = !isTelevision,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )
        }
    }
}


@Composable
private fun AmbientPlayPauseButton(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (isPlaying) R.string.ambient_pause else R.string.ambient_play,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .testTag("ambient_media_play_pause"),
        contentAlignment = Alignment.Center,
    ) {
        AmbientFrostedCircle()
        IconButton(
            onClick = onToggle,
            interactionSource = interactionSource,
            modifier = Modifier.border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = CircleShape,
            ),
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                ),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
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
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun AmbientSoundToggleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onTvKeyEvent: ((Key) -> Boolean)? = null,
) {
    val description = stringResource(
        if (enabled) R.string.settings_ambient_sound_enable else R.string.cd_media_player,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .testTag("ambient_sound_toggle"),
        contentAlignment = Alignment.Center,
    ) {
        AmbientFrostedCircle()
        IconButton(
            onClick = onToggle,
            interactionSource = interactionSource,
            modifier = Modifier
                .then(
                    if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
                )
                .then(
                    if (onTvKeyEvent != null) {
                        Modifier.onKeyEvent { event ->
                            val handled = when (event.key) {
                                Key.DirectionLeft,
                                Key.DirectionRight,
                                Key.DirectionUp,
                                Key.MediaSkipBackward,
                                Key.MediaSkipForward,
                                Key.MediaPlayPause,
                                Key.MediaPlay,
                                Key.MediaPause,
                                -> true
                                else -> false
                            }
                            if (!handled) return@onKeyEvent false
                            // Consume KeyDown so D-pad does not move focus away from mute.
                            if (event.type != KeyEventType.KeyUp) return@onKeyEvent true
                            onTvKeyEvent(event.key)
                        }
                    } else {
                        Modifier
                    },
                )
                .border(
                    width = if (focused) 3.dp else 0.dp,
                    color = if (focused) Color.White else Color.Transparent,
                    shape = CircleShape,
                ),
        ) {
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
