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
import fr.geoking.arthur.shared.source.MuseumSearchKind
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.source.MuseumSearchSettings
import fr.geoking.arthur.source.StockPhotoSettings
import fr.geoking.arthur.ui.components.ArtworkCard
import fr.geoking.arthur.ui.components.CatalogPageSize
import fr.geoking.arthur.ui.components.CategoryFilter
import fr.geoking.arthur.ui.components.CategoryFilterRow
import fr.geoking.arthur.ui.components.ContextualSubFilterRow
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.GalleryHero
import fr.geoking.arthur.ui.components.GenartTopic
import fr.geoking.arthur.ui.components.MuseumTopic
import fr.geoking.arthur.ui.components.StartAmbientFab
import fr.geoking.arthur.ui.components.canLoadMoreCatalog
import fr.geoking.arthur.ui.components.resolveCategoryCatalog
import fr.geoking.arthur.ui.components.takeCatalogPage

private val StockSourceIds = setOf(
    BundledPackSource.ID,
    PexelsSource.ID,
    UnsplashSource.ID,
)

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
    museumSearchSettings: MuseumSearchSettings? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    var catalog by remember { mutableStateOf(initialCatalog.orEmpty()) }
    var selected by remember { mutableStateOf(initialSelected) }
    var stockCategory by remember {
        mutableStateOf(stockPhotoSettings?.category ?: StockPhotoCategory.Suggestions)
    }
    var selectedCategory by remember { mutableStateOf(CategoryFilter.ALL) }
    var museumTopic by remember { mutableStateOf(MuseumTopic.Suggestions) }
    var genartTopic by remember { mutableStateOf(GenartTopic.Fractal) }

    val museumKind = when (selectedCategory) {
        CategoryFilter.PAINTING -> MuseumSearchKind.Painting
        CategoryFilter.SCULPTURE -> MuseumSearchKind.Sculpture
        else -> MuseumSearchKind.All
    }

    LaunchedEffect(contentEngine, initialCatalog, stockCategory, museumKind) {
        if (stockPhotoSettings != null) {
            stockPhotoSettings.category = stockCategory
        }
        if (museumSearchSettings != null) {
            museumSearchSettings.kind = museumKind
        }
        if (initialCatalog != null) return@LaunchedEffect

        // Fast path: refresh stock Sources first so Photo → Nature/City/… fill immediately
        // without waiting on slow museum N+1 fetches.
        val stockOnly = contentEngine.catalog(
            PreparedRotation(
                sourceIds = StockSourceIds.toList(),
                artworkIds = emptyList(),
            ),
        )
        catalog = catalog.filterNot { it.sourceId in StockSourceIds } + stockOnly

        catalog = contentEngine.catalog(
            PreparedRotation(
                sourceIds = emptyList(),
                artworkIds = emptyList(),
            ),
        )
    }

    ControlPlaneContent(
        catalog = catalog,
        selected = selected,
        onSelect = { selected = it },
        onStartAmbient = {
            // Prefer the live selection (keeps remote URL / kind) over a catalog re-resolve
            // that can miss ephemeral stock ids and silently swap to another engine.
            val chosen = selected?.let { sel ->
                catalog.firstOrNull { it.id == sel.id } ?: sel
            }
            onStartAmbient(chosen ?: resolveAmbientArtwork(catalog, artworkId = null))
        },
        modifier = modifier,
        showFractalPreview = showFractalPreview,
        onCreateCustomFractal = onCreateCustomFractal,
        selectedCategory = selectedCategory,
        onCategorySelected = { next ->
            selectedCategory = next
            if (!next.showsMuseumTopics()) {
                museumTopic = MuseumTopic.Suggestions
            }
            if (!next.showsGenartTopics()) {
                genartTopic = GenartTopic.Fractal
            }
        },
        museumTopic = museumTopic,
        onMuseumTopicChange = { museumTopic = it },
        stockCategory = stockCategory,
        onStockCategoryChange = { stockCategory = it },
        genartTopic = genartTopic,
        onGenartTopicChange = { genartTopic = it },
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
    selectedCategory: CategoryFilter = CategoryFilter.ALL,
    onCategorySelected: (CategoryFilter) -> Unit = {},
    museumTopic: MuseumTopic = MuseumTopic.Suggestions,
    onMuseumTopicChange: (MuseumTopic) -> Unit = {},
    stockCategory: StockPhotoCategory = StockPhotoCategory.Suggestions,
    onStockCategoryChange: (StockPhotoCategory) -> Unit = {},
    genartTopic: GenartTopic = GenartTopic.Fractal,
    onGenartTopicChange: (GenartTopic) -> Unit = {},
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
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            museumTopic = museumTopic,
            onMuseumTopicChange = onMuseumTopicChange,
            genartTopic = genartTopic,
            onGenartTopicChange = onGenartTopicChange,
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
        selectedCategory = selectedCategory,
        onCategorySelected = onCategorySelected,
        museumTopic = museumTopic,
        onMuseumTopicChange = onMuseumTopicChange,
        stockCategory = stockCategory,
        onStockCategoryChange = onStockCategoryChange,
        genartTopic = genartTopic,
        onGenartTopicChange = onGenartTopicChange,
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
    selectedCategory: CategoryFilter = CategoryFilter.ALL,
    onCategorySelected: (CategoryFilter) -> Unit = {},
    museumTopic: MuseumTopic = MuseumTopic.Suggestions,
    onMuseumTopicChange: (MuseumTopic) -> Unit = {},
    stockCategory: StockPhotoCategory = StockPhotoCategory.Suggestions,
    onStockCategoryChange: (StockPhotoCategory) -> Unit = {},
    genartTopic: GenartTopic = GenartTopic.Fractal,
    onGenartTopicChange: (GenartTopic) -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val resolved = remember(catalog, selectedCategory, museumTopic, stockCategory, genartTopic) {
        resolveCategoryCatalog(
            catalog = catalog,
            category = selectedCategory,
            museumTopic = museumTopic,
            stockCategory = stockCategory,
            genartTopic = genartTopic,
        )
    }
    val filteredCatalog = resolved.items
    var visibleCount by remember { mutableStateOf(CatalogPageSize) }
    LaunchedEffect(filteredCatalog.map { it.id }) {
        visibleCount = CatalogPageSize
    }
    val visibleCatalog = remember(filteredCatalog, visibleCount) {
        filteredCatalog.takeCatalogPage(visibleCount)
    }
    // Keep preview in sync with the visible list; each click still updates via onSelect.
    LaunchedEffect(visibleCatalog.map { it.id }) {
        val stillVisible = selected != null && visibleCatalog.any { it.id == selected.id }
        if (!stillVisible) {
            visibleCatalog.firstOrNull()?.let(onSelect)
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
                onOpenSettings = onOpenSettings,
            )
            if (
                onCreateCustomFractal != null &&
                selectedCategory == CategoryFilter.GENART &&
                genartTopic == GenartTopic.Custom &&
                resolved.showSubfilters
            ) {
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
                onCategorySelected = onCategorySelected,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (resolved.showSubfilters) {
                ContextualSubFilterRow(
                    selectedCategory = selectedCategory,
                    museumTopic = museumTopic,
                    onMuseumTopicChange = onMuseumTopicChange,
                    stockCategory = stockCategory,
                    onStockCategoryChange = onStockCategoryChange,
                    genartTopic = genartTopic,
                    onGenartTopicChange = onGenartTopicChange,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("artwork_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(visibleCatalog, key = { _, art -> art.id }) { _, art ->
                    ArtworkCard(
                        artwork = art,
                        selected = art.id == selected?.id,
                        onClick = { onSelect(art) },
                    )
                }
                if (filteredCatalog.canLoadMoreCatalog(visibleCount)) {
                    item(key = "load_more") {
                        TextButton(
                            onClick = { visibleCount += CatalogPageSize },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_more"),
                        ) {
                            Text(stringResource(R.string.load_more))
                        }
                    }
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
