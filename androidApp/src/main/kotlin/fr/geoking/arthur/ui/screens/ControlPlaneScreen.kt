package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.ui.components.ArtworkCard
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.GalleryHero
import fr.geoking.arthur.ui.components.StartAmbientFab

@Composable
fun ControlPlaneScreen(
    contentEngine: ContentEngine,
    onStartAmbient: (Artwork?) -> Unit,
    modifier: Modifier = Modifier,
    initialCatalog: List<Artwork>? = null,
    initialSelected: Artwork? = null,
    showFractalPreview: Boolean = true,
    onCreateCustomFractal: (() -> Unit)? = null,
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
        onStartAmbient = {
            onStartAmbient(resolveAmbientArtwork(catalog, selected?.id))
        },
        modifier = modifier,
        showFractalPreview = showFractalPreview,
        onCreateCustomFractal = onCreateCustomFractal,
    )
}

enum class CategoryFilter(@get:StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    PHOTO(R.string.kind_photo),
    SCULPTURE(R.string.kind_sculpture),
    GENART(R.string.kind_genart),
    FRACTAL(R.string.kind_fractal),
    PAINTING(R.string.kind_painting),
    PERSONAL(R.string.kind_personal);

    fun matches(kind: ArtworkKind): Boolean = when (this) {
        ALL -> true
        PHOTO -> kind == ArtworkKind.Photo
        SCULPTURE -> kind == ArtworkKind.Sculpture
        GENART -> kind == ArtworkKind.Genart
        FRACTAL -> kind == ArtworkKind.FractalPreset || kind == ArtworkKind.CustomFractal
        PAINTING -> kind == ArtworkKind.Painting
        PERSONAL -> kind == ArtworkKind.PersonalPhoto
    }
}

@Composable
fun ControlPlaneContent(
    catalog: List<Artwork>,
    selected: Artwork?,
    onSelect: (Artwork) -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    showFractalPreview: Boolean = true,
    onCreateCustomFractal: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val isTelevision = remember(configuration) {
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    }
    var selectedCategory by remember { mutableStateOf(CategoryFilter.ALL) }
    val filteredCatalog = remember(catalog, selectedCategory) {
        catalog.filter { selectedCategory.matches(it.kind) }
    }

    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(isTelevision, filteredCatalog.firstOrNull()?.id) {
        if (isTelevision && filteredCatalog.isNotEmpty()) {
            firstItemFocus.requestFocus()
        }
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("control_plane"),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            StartAmbientFab(
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
            if (onCreateCustomFractal != null) {
                TextButton(
                    onClick = onCreateCustomFractal,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("create_custom_fractal"),
                ) {
                    Text(stringResource(R.string.custom_fractal_create))
                }
            }
            GalleryHero(
                livePreview = showFractalPreview,
                artwork = selected,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(if (isTelevision) 200.dp else 168.dp)
                    .testTag("fractal_preview"),
            )
            Text(
                text = stringResource(R.string.rotation_section),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("category_filter_row"),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CategoryFilter.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(stringResource(category.labelRes)) },
                        modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}"),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("artwork_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTelevision) 14.dp else 10.dp),
            ) {
                itemsIndexed(filteredCatalog, key = { _, art -> art.id }) { index, art ->
                    ArtworkCard(
                        artwork = art,
                        selected = art.id == selected?.id,
                        onClick = { onSelect(art) },
                        focusRequester = if (index == 0) firstItemFocus else null,
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
