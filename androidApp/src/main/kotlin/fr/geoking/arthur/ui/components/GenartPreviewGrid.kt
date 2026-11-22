package fr.geoking.arthur.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.geoking.arthur.R
import fr.geoking.arthur.fractal.FractalStillRenderer
import fr.geoking.arthur.genart.GenartCatalog
import fr.geoking.arthur.genart.GenartStillRenderer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.SafeBitmapDecoder
import fr.geoking.arthur.source.rememberArtworkImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PreviewCoverShape = RoundedCornerShape(8.dp)
private const val PreviewThumbSide = 512
private const val PreviewLargeSide = 1280

/**
 * Grid of Genart (and related generative) still previews — same layout language as [PackGrid].
 */
@Composable
fun GenartPreviewGrid(
    artworks: List<Artwork>,
    onArtworkClick: (Artwork) -> Unit,
    isLocked: (Artwork) -> Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    firstTileFocusRequester: FocusRequester? = null,
) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val layoutDirection = LocalLayoutDirection.current
    val imageCache = rememberArtworkImageCache()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = packGridLayout(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            windowSizeClass = windowSizeClass,
            horizontalPadding = contentPadding.calculateStartPadding(layoutDirection) +
                contentPadding.calculateEndPadding(layoutDirection),
            verticalPadding = contentPadding.calculateTopPadding() +
                contentPadding.calculateBottomPadding(),
            columnsOverride = null,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag("genart_preview_grid"),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(artworks, key = { it.id }) { artwork ->
                GenartPreviewTile(
                    artwork = artwork,
                    locked = isLocked(artwork),
                    onClick = { onArtworkClick(artwork) },
                    maxCoverSize = layout.maxCoverSize,
                    imageCache = imageCache,
                    focusRequester = if (artwork == artworks.firstOrNull()) {
                        firstTileFocusRequester
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
fun GenartPreviewTile(
    artwork: Artwork,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxCoverSize: Dp = Dp.Unspecified,
    imageCache: ArtworkImageCache = rememberArtworkImageCache(),
    focusRequester: FocusRequester? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val visual = artwork.kind.visual()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val highlight = focused

    var previewBitmap by remember(artwork.id) { mutableStateOf<Bitmap?>(null) }
    var loadDone by remember(artwork.id) { mutableStateOf(false) }

    LaunchedEffect(artwork.id) {
        previewBitmap = withContext(Dispatchers.Default) {
            loadOrBakeGenartStill(artwork, imageCache, maxSide = PreviewThumbSide)
        }
        loadDone = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.selected = focused
                role = Role.Button
            }
            .testTag("genart_preview_${artwork.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            shape = PreviewCoverShape,
            border = if (highlight) BorderStroke(3.dp, scheme.primary) else null,
            tonalElevation = if (highlight) 2.dp else 0.dp,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (maxCoverSize != Dp.Unspecified) {
                        Modifier.widthIn(max = maxCoverSize)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(PreviewCoverShape),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = previewBitmap
                when {
                    bmp != null -> Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = artwork.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    !loadDone -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(visual.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = visual.onContainer,
                            strokeWidth = 2.dp,
                        )
                    }
                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(visual.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(visual.iconRes),
                            contentDescription = artwork.title,
                            tint = visual.onContainer,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                if (locked) {
                    PackLockBadge(
                        testTag = "genart_preview_locked_${artwork.id}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                }
            }
        }
        Text(
            text = artwork.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) scheme.primary else scheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
    }
}

/**
 * Full-screen static still preview (no live Ambient). Dismiss by tapping outside / back.
 */
@Composable
fun GenartStillPreviewDialog(
    artwork: Artwork,
    onDismiss: () -> Unit,
    imageCache: ArtworkImageCache = rememberArtworkImageCache(),
) {
    var bitmap by remember(artwork.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(artwork.id) {
        bitmap = withContext(Dispatchers.Default) {
            loadOrBakeGenartStill(artwork, imageCache, maxSide = PreviewLargeSide)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss)
                .testTag("genart_still_preview_dialog"),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = artwork.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable(enabled = false) {},
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
            Text(
                text = artwork.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
            )
        }
    }
}

/**
 * Browse section: optional buy CTA + still preview grid.
 * Locked items open a static large preview; unlocked items start Ambient via [onPlayArtwork].
 */
@Composable
fun GenartPackItemsSection(
    artworks: List<Artwork>,
    packOwnership: PackOwnership,
    packLocked: Boolean,
    sellablePackId: String?,
    onBuyPack: ((String?) -> Unit)?,
    onPlayArtwork: (Artwork) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    firstTileFocusRequester: FocusRequester? = null,
) {
    var previewArtwork by remember { mutableStateOf<Artwork?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val horizontalPad = contentPadding.calculateStartPadding(layoutDirection)

    Column(modifier = modifier.fillMaxSize()) {
        if (packLocked && onBuyPack != null) {
            Button(
                onClick = { onBuyPack(sellablePackId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPad)
                    .padding(bottom = 8.dp)
                    .testTag("genart_buy_pack"),
            ) {
                Text(stringResource(R.string.marketplace_buy_pack))
            }
        }
        GenartPreviewGrid(
            artworks = artworks,
            onArtworkClick = { artwork ->
                if (packOwnership.allowsArtwork(artwork)) {
                    onPlayArtwork(artwork)
                } else {
                    previewArtwork = artwork
                }
            },
            isLocked = { !packOwnership.allowsArtwork(it) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = contentPadding,
            firstTileFocusRequester = firstTileFocusRequester,
        )
    }

    previewArtwork?.let { art ->
        GenartStillPreviewDialog(
            artwork = art,
            onDismiss = { previewArtwork = null },
        )
    }
}

internal fun loadOrBakeGenartStill(
    artwork: Artwork,
    imageCache: ArtworkImageCache,
    maxSide: Int,
): Bitmap? {
    val cachedPath = imageCache.localPathOrNull(artwork.id)
    if (cachedPath != null) {
        SafeBitmapDecoder.decodeFile(cachedPath, maxSide = maxSide)?.let { return it }
    }
    return bakeGenartStill(artwork, maxSide)
}

private fun bakeGenartStill(artwork: Artwork, maxSide: Int): Bitmap? {
    val size = maxSide.coerceAtLeast(64)
    return when (artwork.kind) {
        ArtworkKind.Genart -> {
            val engine = GenartCatalog.engineForId(artwork.id) ?: return null
            runCatching {
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                GenartStillRenderer.draw(
                    canvas = Canvas(bitmap),
                    engineId = engine,
                    size = size,
                    generation = 0L,
                )
                bitmap
            }.getOrNull()
        }
        ArtworkKind.FractalPreset -> {
            val type = FractalStillRenderer.typeForArtworkId(artwork.id) ?: return null
            runCatching {
                FractalStillRenderer.render(type = type, size = size, generation = 0L)
            }.getOrNull()
        }
        else -> null
    }
}
