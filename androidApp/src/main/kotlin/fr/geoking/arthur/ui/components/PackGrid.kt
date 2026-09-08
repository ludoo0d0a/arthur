package fr.geoking.arthur.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import fr.geoking.arthur.R
import kotlin.math.max

private val PackCoverShape = RoundedCornerShape(8.dp)
private val PackGridHSpacing = 12.dp
private val PackGridVSpacing = 16.dp
/** Space reserved under each cover for the 1–2 line title. */
private val PackTileLabelReserve = 40.dp
private val PackCoverMinSize = 72.dp
/** Target first viewport: at least two rows of tiles are visible. */
private const val PackGridVisibleRows = 2
private const val PackGridMinColumns = 3

/**
 * Column count and cover cap so phone landscape / TV keep roughly a 2×3 (or denser)
 * pack grid on screen, using [WindowSizeClass] breakpoints + available container size.
 */
internal data class PackGridLayout(
    val columns: Int,
    val maxCoverSize: Dp,
)

internal fun packGridLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    windowSizeClass: WindowSizeClass,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    columnsOverride: Int? = null,
): PackGridLayout {
    val availableWidth = (maxWidth - horizontalPadding).coerceAtLeast(PackCoverMinSize)
    val availableHeight = (maxHeight - verticalPadding).coerceAtLeast(PackCoverMinSize)

    // Prefer width breakpoints (Material adaptive guidance) over device type checks.
    val widthClassColumns = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 4
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 3
        else -> PackGridMinColumns
    }

    // Cap cover height so [PackGridVisibleRows] fit in the first viewport (landscape / TV chrome).
    val heightBudgetCover = (
        availableHeight -
            PackGridVSpacing * (PackGridVisibleRows - 1) -
            PackTileLabelReserve * PackGridVisibleRows
        ) / PackGridVisibleRows
    val maxCoverFromHeight = heightBudgetCover.coerceAtLeast(PackCoverMinSize)

    val columnsForHeight = max(
        1,
        ((availableWidth + PackGridHSpacing) / (maxCoverFromHeight + PackGridHSpacing)).toInt(),
    )
    val columns = (columnsOverride ?: max(widthClassColumns, columnsForHeight))
        .coerceAtLeast(PackGridMinColumns)

    val cellWidth = (availableWidth - PackGridHSpacing * (columns - 1)) / columns
    val maxCoverSize = minOf(cellWidth, maxCoverFromHeight).coerceAtLeast(PackCoverMinSize)

    return PackGridLayout(
        columns = columns,
        maxCoverSize = maxCoverSize,
    )
}

@Composable
private fun PackSelectedPlayOverlay(
    visible: Boolean,
    testTagSuffix: String,
) {
    // File-level caller so AnimatedVisibility is not ColumnScope-bound.
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_circle),
                contentDescription = stringResource(R.string.start_ambient),
                tint = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("pack_tile_play_$testTagSuffix"),
            )
        }
    }
}

@Composable
fun PackCoverTile(
    tile: PackTile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selectOnFocus: Boolean = false,
    onFocusSelect: (() -> Unit)? = null,
    maxCoverSize: Dp = Dp.Unspecified,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val currentOnFocusSelect by rememberUpdatedState(onFocusSelect)

    LaunchedEffect(focused, tile.id) {
        if (selectOnFocus && focused) {
            currentOnFocusSelect?.invoke()
        }
    }

    // Phone: chrome follows tap selection. TV: border follows D-pad focus.
    val highlight = focused || selected
    val showPlay = if (selectOnFocus) focused else selected

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.selected = selected || (selectOnFocus && focused) }
            .testTag("pack_tile_${tile.testTagSuffix}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            shape = PackCoverShape,
            border = if (highlight) {
                BorderStroke(3.dp, scheme.primary)
            } else {
                null
            },
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
                    .clip(PackCoverShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(tile.coverRes),
                    contentDescription = stringResource(tile.titleRes),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                PackSelectedPlayOverlay(
                    visible = showPlay,
                    testTagSuffix = tile.testTagSuffix,
                )
            }
        }
        Text(
            text = stringResource(tile.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) scheme.primary else scheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
fun PackGrid(
    tiles: List<PackTile>,
    selected: PackSelection?,
    onTileClick: (PackTile) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    firstTileFocusRequester: FocusRequester? = null,
    selectOnFocus: Boolean = false,
    onTileFocused: ((PackTile) -> Unit)? = null,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = packGridLayout(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            windowSizeClass = windowSizeClass,
            horizontalPadding = contentPadding.calculateStartPadding(layoutDirection) +
                contentPadding.calculateEndPadding(layoutDirection),
            verticalPadding = contentPadding.calculateTopPadding() +
                contentPadding.calculateBottomPadding(),
            columnsOverride = columns,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag("pack_grid"),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(PackGridHSpacing),
            verticalArrangement = Arrangement.spacedBy(PackGridVSpacing),
        ) {
            items(tiles, key = { it.id }) { tile ->
                PackCoverTile(
                    tile = tile,
                    selected = selected == tile.selection,
                    onClick = { onTileClick(tile) },
                    focusRequester = if (tile == tiles.firstOrNull()) firstTileFocusRequester else null,
                    selectOnFocus = selectOnFocus,
                    onFocusSelect = onTileFocused?.let { focused -> { focused(tile) } },
                    maxCoverSize = layout.maxCoverSize,
                )
            }
        }
    }
}
