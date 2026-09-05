package fr.geoking.arthur.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import fr.geoking.arthur.ui.components.homeTile
import fr.geoking.arthur.ui.components.subPackTiles

/**
 * TV Control Plane: pack grid (no hero preview), D-pad friendly tiles + Start button.
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
) {
    val scheme = MaterialTheme.colorScheme
    val startFocus = remember { FocusRequester() }
    val firstTileFocus = remember { FocusRequester() }

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
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight(),
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
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        firstTileFocusRequester = firstTileFocus,
                    )
                } else {
                    PackSubPackHeader(
                        family = openedFamily,
                        onBack = onBackToHome,
                    )
                    PackGrid(
                        tiles = openedFamily.subPackTiles(),
                        selected = selection,
                        onTileClick = { tile: PackTile -> onSelectSubPack(tile.selection) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        firstTileFocusRequester = firstTileFocus,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Button(
                    onClick = onStartAmbient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(startFocus)
                        .focusProperties { left = firstTileFocus }
                        .testTag("start_ambient"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_ambient),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.start_ambient))
                }
            }
        }
    }
}
