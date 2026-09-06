package fr.geoking.arthur.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.source.ScreensaverSettings
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
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
    onOpenSettings: (() -> Unit)? = null,
    defaultScreensaverSelection: PackSelection? = null,
    onSetDefaultScreensaver: ((PackSelection) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val firstTileFocus = remember { FocusRequester() }

    BackHandler(enabled = openedFamily != null) {
        onBackToHome()
    }

    LaunchedEffect(openedFamily) {
        firstTileFocus.requestFocus()
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
                    tiles = PackFamily.entries.map { it.homeTile() },
                    selected = null,
                    onTileClick = { onOpenFamily(it.selection.family) },
                    columns = 4,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    firstTileFocusRequester = firstTileFocus,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(openedFamily.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    if (onSetDefaultScreensaver != null) {
                        val activeDefault = defaultScreensaverSelection ?: ScreensaverSettings.DEFAULT_PACK_SELECTION
                        val isDefault = activeDefault == selection
                        Button(
                            onClick = { onSetDefaultScreensaver(selection) },
                            modifier = Modifier.testTag("save_default_screensaver_button"),
                        ) {
                            if (isDefault) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text(stringResource(R.string.is_default_screensaver))
                            } else {
                                Text(stringResource(R.string.save_default_screensaver))
                            }
                        }
                    }
                }
                PackGrid(
                    tiles = openedFamily.subPackTiles(),
                    selected = selection,
                    onTileClick = { onStartAmbient() },
                    columns = 4,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    firstTileFocusRequester = firstTileFocus,
                    selectOnFocus = true,
                    onTileFocused = { onSelectSubPack(it.selection) },
                )
            }
        }
    }
}
