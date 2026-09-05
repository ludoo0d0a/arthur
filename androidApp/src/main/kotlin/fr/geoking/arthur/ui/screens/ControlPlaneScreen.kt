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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.source.StockPhotoSettings
import fr.geoking.arthur.ui.components.ArtworkCard
import fr.geoking.arthur.ui.components.CategoryFilter
import fr.geoking.arthur.ui.components.CategoryFilterRow
import fr.geoking.arthur.ui.components.ContextualSubFilterRow
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.GalleryHero
import fr.geoking.arthur.ui.components.StartAmbientFab
import fr.geoking.arthur.ui.components.filterByCategoryAndSources
import fr.geoking.arthur.ui.components.toggleSource

@Composable
fun ControlPlaneScreen(
    contentEngine: ContentEngine,
    onStartAmbient: (Artwork?) -> Unit,
    modifier: Modifier = Modifier,
    initialCatalog: List<Artwork>? = null,
    initialSelected: Artwork? = null,
    showFractalPreview: Boolean = true,
    onCreateCustomFractal: (() -> Unit)? = null,
    stockPhotoSettings: StockPhotoSettings? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    var catalog by remember { mutableStateOf(initialCatalog.orEmpty()) }
    var selected by remember { mutableStateOf(initialSelected) }
    var stockCategory by remember {
        mutableStateOf(stockPhotoSettings?.category ?: StockPhotoCategory.Nature)
    }

    LaunchedEffect(contentEngine, initialCatalog, stockCategory) {
        if (stockPhotoSettings != null) {
            stockPhotoSettings.category = stockCategory
        }
        if (initialCatalog == null) {
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
        stockCategory = stockCategory,
        onStockCategoryChange = { stockCategory = it },
        onOpenSettings = onOpenSettings,
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
    onCreateCustomFractal: (() -> Unit)? = null,
    stockCategory: StockPhotoCategory = StockPhotoCategory.Nature,
    onStockCategoryChange: (StockPhotoCategory) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val isTelevision = remember(configuration) {
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    }
    if (isTelevision) {
        TvControlPlaneContent(
            catalog = catalog,
            selected = selected,
            onSelect = onSelect,
            onStartAmbient = onStartAmbient,
            modifier = modifier,
            showFractalPreview = showFractalPreview,
            onOpenSettings = onOpenSettings,
        )
        return
    }
    PhoneControlPlaneContent(
        catalog = catalog,
        selected = selected,
        onSelect = onSelect,
        onStartAmbient = onStartAmbient,
        modifier = modifier,
        showFractalPreview = showFractalPreview,
        onCreateCustomFractal = onCreateCustomFractal,
        stockCategory = stockCategory,
        onStockCategoryChange = onStockCategoryChange,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun PhoneControlPlaneContent(
    catalog: List<Artwork>,
    selected: Artwork?,
    onSelect: (Artwork) -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    showFractalPreview: Boolean = true,
    onCreateCustomFractal: (() -> Unit)? = null,
    stockCategory: StockPhotoCategory = StockPhotoCategory.Nature,
    onStockCategoryChange: (StockPhotoCategory) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf(CategoryFilter.ALL) }
    var selectedSourceIds by remember { mutableStateOf(emptySet<String>()) }
    val filteredCatalog = remember(catalog, selectedCategory, selectedSourceIds) {
        catalog.filterByCategoryAndSources(selectedCategory, selectedSourceIds)
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
                onOpenSettings = onOpenSettings,
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
                    .height(168.dp)
                    .testTag("fractal_preview"),
            )
            Text(
                text = stringResource(R.string.rotation_section),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { next ->
                    selectedCategory = next
                    if (!next.showsMuseumSources()) {
                        selectedSourceIds = emptySet()
                    }
                },
                modifier = Modifier.padding(bottom = 4.dp),
            )
            ContextualSubFilterRow(
                selectedCategory = selectedCategory,
                selectedSourceIds = selectedSourceIds,
                onToggleSource = { selectedSourceIds = selectedSourceIds.toggleSource(it) },
                stockCategory = stockCategory,
                onStockCategoryChange = onStockCategoryChange,
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("artwork_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(filteredCatalog, key = { _, art -> art.id }) { _, art ->
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

@Preview(
    showBackground = true,
    name = "TV control plane",
    device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
)
@Composable
private fun TvControlPlanePreview() {
    val catalog = BundledPackSource.defaultPack()
    ArthurTheme {
        TvControlPlaneContent(
            catalog = catalog,
            selected = catalog.firstOrNull(),
            onSelect = {},
            onStartAmbient = {},
            showFractalPreview = false,
        )
    }
}
