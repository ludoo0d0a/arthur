package fr.geoking.arthur.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.ui.components.ArtworkCard
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.GalleryHero
import fr.geoking.arthur.ui.components.StartAmbientBar

@Composable
fun ControlPlaneScreen(
    contentEngine: ContentEngine,
    onStartAmbient: (Artwork?) -> Unit,
    modifier: Modifier = Modifier,
    initialCatalog: List<Artwork>? = null,
    initialSelected: Artwork? = null,
    showFractalPreview: Boolean = true,
) {
    var catalog by remember { mutableStateOf(initialCatalog.orEmpty()) }
    var selected by remember { mutableStateOf(initialSelected) }

    LaunchedEffect(contentEngine, initialCatalog) {
        if (initialCatalog == null) {
            // Empty sourceIds → all registered Sources (bundled, genart, fractal)
            catalog = contentEngine.catalog(
                PreparedRotation(
                    sourceIds = emptyList(),
                    artworkIds = emptyList(),
                ),
            )
        }
    }

    ControlPlaneContent(
        catalog = catalog,
        selected = selected,
        onSelect = { selected = it },
        onStartAmbient = { onStartAmbient(selected) },
        modifier = modifier,
        showFractalPreview = showFractalPreview,
    )
}

@Composable
fun ControlPlaneContent(
    catalog: List<Artwork>,
    selected: Artwork?,
    onSelect: (Artwork) -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    showFractalPreview: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("control_plane"),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            StartAmbientBar(
                selectedTitle = selected?.title,
                onStartAmbient = onStartAmbient,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            ControlPlaneHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            GalleryHero(
                livePreview = showFractalPreview,
                artwork = selected,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(168.dp)
                    .testTag("fractal_preview"),
            )
            Text(
                text = stringResource(R.string.rotation_section),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("artwork_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(catalog, key = { it.id }) { art ->
                    ArtworkCard(
                        artwork = art,
                        selected = art.id == selected?.id,
                        onClick = { onSelect(art) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Control plane")
@Composable
private fun ControlPlanePreview() {
    val catalog = BundledPackSource.defaultPack()
    ArthurTheme {
        ControlPlaneContent(
            catalog = catalog,
            selected = catalog.firstOrNull(),
            onSelect = {},
            onStartAmbient = {},
            showFractalPreview = false,
        )
    }
}
