package fr.geoking.arthur.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val PackCoverShape = RoundedCornerShape(8.dp)

@Composable
fun PackCoverTile(
    tile: PackTile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            .semantics { this.selected = selected }
            .testTag("pack_tile_${tile.testTagSuffix}"),
    ) {
        Surface(
            onClick = onClick,
            shape = PackCoverShape,
            border = if (selected) {
                BorderStroke(3.dp, scheme.primary)
            } else {
                null
            },
            tonalElevation = if (selected) 2.dp else 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(tile.coverRes),
                contentDescription = stringResource(tile.titleRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(PackCoverShape),
            )
        }
        Text(
            text = stringResource(tile.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) scheme.primary else scheme.onBackground,
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
            )
        }
    }
}
