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
import fr.geoking.arthur.shared.domain.ArtUniverse
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import fr.geoking.arthur.ui.components.homeTile
import fr.geoking.arthur.ui.components.GenartPackItemsSection
import fr.geoking.arthur.ui.components.genartTopicOrNull
import fr.geoking.arthur.ui.components.isPackLocked
import fr.geoking.arthur.ui.components.opensSoundPlayer
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.sellablePackIdOrNull
import fr.geoking.arthur.ui.components.subPackTiles
import fr.geoking.arthur.ui.components.visibleHomePackFamilies

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
    onStartAmbientArtwork: (Artwork) -> Unit = {},
    modifier: Modifier = Modifier,
    catalog: List<Artwork> = emptyList(),
    onOpenSettings: (() -> Unit)? = null,
    onOpenMediaPlayer: (() -> Unit)? = null,
    defaultScreensaverSelection: PackSelection? = null,
    onSetDefaultScreensaver: ((PackSelection) -> Unit)? = null,
    packOwnership: PackOwnership = PackOwnership.NONE,
    onOpenMarketplace: ((highlightPackId: String?) -> Unit)? = null,
    startingSelection: PackSelection? = null,
    activeUniverse: ArtUniverse = ArtUniverse.Gallery,
    onUniverseChange: ((ArtUniverse) -> Unit)? = null,
    showUniverseSwitchHint: Boolean = false,
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
            onOpenMediaPlayer = onOpenMediaPlayer,
            activeUniverse = activeUniverse,
            onUniverseChange = onUniverseChange,
            showUniverseSwitchHint = showUniverseSwitchHint,
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
                    tiles = visibleHomePackFamilies(activeUniverse).map { it.homeTile(catalog) },
                    selected = null,
                    onTileClick = { onOpenFamily(it.selection.family) },
                    isLocked = { it.isLocked(packOwnership) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    firstTileFocusRequester = firstTileFocus,
                    selectedTileFocusRequester = selectedTileFocus,
                )
            } else if (openedFamily == PackFamily.Genart) {
                val genartDetailTopic = selection.genartTopicOrNull()
                Text(
                    text = stringResource(
                        genartDetailTopic?.labelRes ?: openedFamily.titleRes,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (genartDetailTopic == null) {
                    PackGrid(
                        tiles = openedFamily.subPackTiles(catalog),
                        selected = null,
                        onTileClick = { tile: PackTile ->
                            onSelectSubPack(tile.selection)
                        },
                        isLocked = { it.isLocked(packOwnership) },
                        startingSelection = startingSelection,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        firstTileFocusRequester = firstTileFocus,
                        selectedTileFocusRequester = selectedTileFocus,
                    )
                } else {
                    val genarts = remember(catalog, selection) { resolvePackPool(catalog, selection) }
                    val packLocked = selection.isPackLocked(packOwnership)
                    GenartPackItemsSection(
                        artworks = genarts,
                        packOwnership = packOwnership,
                        packLocked = packLocked,
                        sellablePackId = selection.sellablePackIdOrNull(),
                        onBuyPack = onOpenMarketplace,
                        onPlayArtwork = onStartAmbientArtwork,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("genart_all_list"),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        firstTileFocusRequester = firstTileFocus,
                    )
                }
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
                        } else if (tile.selection.opensSoundPlayer()) {
                            // onSelectSubPack opens SoundPlayer via ControlPlaneScreen.
                            onSelectSubPack(tile.selection)
                        } else {
                            onSelectSubPack(tile.selection)
                            onStartAmbient()
                        }
                    },
                    onTileLongClick = { tile ->
                        if (tile.isLocked(packOwnership)) {
                            onOpenMarketplace?.invoke(tile.sellablePackId)
                        } else if (tile.selection.opensSoundPlayer()) {
                            onSelectSubPack(tile.selection)
                        } else {
                            onSelectSubPack(tile.selection)
                            onSetDefaultScreensaver?.invoke(tile.selection)
                            onStartAmbient()
                        }
                    },
                    isLocked = { it.isLocked(packOwnership) },
                    startingSelection = startingSelection,
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
