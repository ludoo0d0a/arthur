package fr.geoking.arthur.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import fr.geoking.arthur.ui.components.homeTile
import fr.geoking.arthur.ui.components.subPackTiles

/**
 * TV Control Plane: pack grid (no hero preview).
 * D-pad moves selection; OK starts Ambient; Back returns to the previous screen.
 */
@Composable
fun TvControlPlaneContent(
    openedFamily: PackFamily?,
    selection: PackSelection,
    onOpenFamily: (PackFamily) -> Unit,
    onSelectSubPack: (PackSelection) -> Unit,
    onBackToHome: () -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    catalog: List<Artwork> = emptyList(),
    onOpenSettings: (() -> Unit)? = null,
    defaultScreensaverSelection: PackSelection? = null,
    onSetDefaultScreensaver: ((PackSelection) -> Unit)? = null,
    packOwnership: PackOwnership = PackOwnership.NONE,
    onOpenMarketplace: ((highlightPackId: String?) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val firstTileFocus = remember { FocusRequester() }
    val selectedTileFocus = remember { FocusRequester() }

    BackHandler(enabled = openedFamily != null) {
        onBackToHome()
    }

    LaunchedEffect(openedFamily) {
        val targetSelection = if (openedFamily == null) {
            PackSelection(selection.family)
        } else {
            if (selection.family == openedFamily) selection else null
        }

        if (targetSelection != null) {
            selectedTileFocus.requestFocus()
        } else {
            firstTileFocus.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("control_plane"),
    ) {
        ControlPlaneHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            onOpenSettings = onOpenSettings,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            if (openedFamily == null) {
                Text(
                    text = stringResource(R.string.packs_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PackGrid(
                    tiles = PackFamily.entries.map { it.homeTile(catalog) },
                    selected = null,
                    onTileClick = { onOpenFamily(it.selection.family) },
                    isLocked = { it.isLocked(packOwnership) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    firstTileFocusRequester = firstTileFocus,
                    selectedTileFocusRequester = selectedTileFocus,
                )
            } else {
                Text(
                    text = stringResource(openedFamily.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PackGrid(
                    tiles = openedFamily.subPackTiles(catalog),
                    selected = selection,
                    onTileClick = { tile ->
                        if (tile.isLocked(packOwnership)) {
                            onOpenMarketplace?.invoke(tile.sellablePackId)
                        } else {
                            onStartAmbient()
                        }
                    },
                    onTileLongClick = { tile ->
                        if (tile.isLocked(packOwnership)) {
                            onOpenMarketplace?.invoke(tile.sellablePackId)
                        } else {
                            onSetDefaultScreensaver?.invoke(tile.selection)
                            onStartAmbient()
                        }
                    },
                    isLocked = { it.isLocked(packOwnership) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    firstTileFocusRequester = firstTileFocus,
                    selectedTileFocusRequester = selectedTileFocus,
                    selectOnFocus = true,
                    onTileFocused = { tile ->
                        if (!tile.isLocked(packOwnership)) {
                            onSelectSubPack(tile.selection)
                        }
                    },
                )
            }
        }
    }
}
