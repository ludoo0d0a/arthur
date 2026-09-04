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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.ui.components.ArtworkCard
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.GalleryHero

/**
 * TV Control Plane: single flat z-level (no FAB), D-pad left/right + up/down,
 * no touch-only custom-fractal authoring.
 *
 * Layout: catalog column (filters ↓ list) | preview column (hero ↓ start).
 */
@Composable
fun TvControlPlaneContent(
    catalog: List<Artwork>,
    selected: Artwork?,
    onSelect: (Artwork) -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    showFractalPreview: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf(CategoryFilter.ALL) }
    val filteredCatalog = remember(catalog, selectedCategory) {
        catalog.filter { selectedCategory.matches(it.kind) }
    }

    val startFocus = remember { FocusRequester() }
    val firstCardFocus = remember { FocusRequester() }
    val firstChipFocus = remember { FocusRequester() }

    LaunchedEffect(filteredCatalog.firstOrNull()?.id) {
        if (filteredCatalog.isNotEmpty()) {
            firstCardFocus.requestFocus()
        } else {
            startFocus.requestFocus()
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
                    .weight(0.55f)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.rotation_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("category_filter_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(CategoryFilter.entries) { index, category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(stringResource(category.labelRes)) },
                            modifier = Modifier
                                .then(
                                    if (index == 0) {
                                        Modifier
                                            .focusRequester(firstChipFocus)
                                            .focusProperties { down = firstCardFocus }
                                    } else {
                                        Modifier
                                    },
                                )
                                .testTag("filter_chip_${category.name.lowercase()}"),
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("artwork_list"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(filteredCatalog, key = { _, art -> art.id }) { index, art ->
                        ArtworkCard(
                            artwork = art,
                            selected = art.id == selected?.id,
                            onClick = { onSelect(art) },
                            focusRequester = if (index == 0) firstCardFocus else null,
                            modifier = if (index == 0) {
                                Modifier.focusProperties {
                                    up = firstChipFocus
                                    right = startFocus
                                }
                            } else {
                                Modifier.focusProperties { right = startFocus }
                            },
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GalleryHero(
                    livePreview = showFractalPreview,
                    artwork = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("fractal_preview"),
                )
                Button(
                    onClick = onStartAmbient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(startFocus)
                        .focusProperties { left = firstCardFocus }
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
