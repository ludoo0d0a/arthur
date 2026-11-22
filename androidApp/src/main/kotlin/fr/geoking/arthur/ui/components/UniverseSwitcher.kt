package fr.geoking.arthur.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.ArtUniverse

@Composable
internal fun UniverseSwitcher(
    activeUniverse: ArtUniverse,
    onUniverseChange: (ArtUniverse) -> Unit,
    modifier: Modifier = Modifier,
    showSwitchHint: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("universe_switcher"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = activeUniverse == ArtUniverse.Gallery,
                onClick = { onUniverseChange(ArtUniverse.Gallery) },
                label = { Text(stringResource(R.string.universe_gallery)) },
                modifier = Modifier.testTag("universe_chip_gallery"),
            )
            FilterChip(
                selected = activeUniverse == ArtUniverse.Generative,
                onClick = { onUniverseChange(ArtUniverse.Generative) },
                label = { Text(stringResource(R.string.universe_generative)) },
                modifier = Modifier.testTag("universe_chip_generative"),
            )
        }
        if (showSwitchHint) {
            Text(
                text = stringResource(R.string.universe_switch_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("universe_switch_hint"),
            )
        }
    }
}
