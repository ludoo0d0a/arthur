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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R

private val PackCoverShape = RoundedCornerShape(8.dp)

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
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    firstTileFocusRequester: FocusRequester? = null,
    selectOnFocus: Boolean = false,
    onTileFocused: ((PackTile) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pack_grid"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(tiles, key = { it.id }) { tile ->
            PackCoverTile(
                tile = tile,
                selected = selected == tile.selection,
                onClick = { onTileClick(tile) },
                focusRequester = if (tile == tiles.firstOrNull()) firstTileFocusRequester else null,
                selectOnFocus = selectOnFocus,
                onFocusSelect = onTileFocused?.let { focused -> { focused(tile) } },
            )
        }
    }
}
