package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import fr.geoking.arthur.shared.source.PexelsVideoSource
import fr.geoking.arthur.shared.source.PixabayVideoSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.source.MuseumSearchSettings
import fr.geoking.arthur.source.StockPhotoSettings
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import fr.geoking.arthur.ui.components.allowsGenerativeAmbientFallback
import fr.geoking.arthur.ui.components.homeTile
import fr.geoking.arthur.ui.components.isGenartCustom
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.sourceIdsForAmbientLoad
import fr.geoking.arthur.ui.components.stockCategoryOrNull
import fr.geoking.arthur.ui.components.subPackTiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val StockSourceIds = setOf(
    BundledPackSource.ID,
    PexelsSource.ID,
    UnsplashSource.ID,
)

private val VideoSourceIds = setOf(
    PexelsVideoSource.ID,
    PixabayVideoSource.ID,
    CoverrSource.ID,
)

@Composable
fun ControlPlaneScreen(
    contentEngine: ContentEngine,
    onStartAmbient: (artwork: Artwork?, pool: List<Artwork>, renewSourceIds: List<String>?) -> Unit,
    modifier: Modifier = Modifier,
    initialCatalog: List<Artwork>? = null,
    onCreateCustomFractal: (() -> Unit)? = null,
    stockPhotoSettings: StockPhotoSettings? = null,
    museumSearchSettings: MuseumSearchSettings? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    var catalog by remember { mutableStateOf(initialCatalog.orEmpty()) }
    var openedFamily by remember { mutableStateOf<PackFamily?>(null) }
    var selection by remember { mutableStateOf(PackSelection(PackFamily.Museum)) }
    var startingAmbient by remember { mutableStateOf(false) }
    val packCatalogCache = remember { mutableMapOf<String, List<Artwork>>() }
    val scope = rememberCoroutineScope()

    val stockCategory = selection.stockCategoryOrNull()
        ?: stockPhotoSettings?.category
        ?: StockPhotoCategory.Random

    val museumKind = when (selection.family) {
        PackFamily.Painting -> MuseumSearchKind.Painting
        PackFamily.Sculpture -> MuseumSearchKind.Sculpture
        PackFamily.Museum -> MuseumSearchKind.All
        PackFamily.Photo -> MuseumSearchKind.Photo
        else -> MuseumSearchKind.All
    }

    fun syncSourceSettings() {
        selection.stockCategoryOrNull()?.let { topic ->
            if (stockPhotoSettings != null) {
                stockPhotoSettings.category = topic
            }
        }
        if (museumSearchSettings != null) {
            museumSearchSettings.kind = museumKind
        }
    }

    LaunchedEffect(contentEngine, initialCatalog, stockCategory, museumKind) {
        syncSourceSettings()
        if (initialCatalog != null) return@LaunchedEffect

        val stockOnly = contentEngine.catalog(
            PreparedRotation(
                sourceIds = StockSourceIds.toList(),
                artworkIds = emptyList(),
            ),
        )
        val videoOnly = contentEngine.catalog(
            PreparedRotation(
                sourceIds = VideoSourceIds.toList(),
                artworkIds = emptyList(),
            ),
        )
        catalog = catalog.filterNot { it.sourceId in StockSourceIds || it.sourceId in VideoSourceIds } +
            stockOnly + videoOnly

        catalog = contentEngine.catalog(
            PreparedRotation(
                sourceIds = emptyList(),
                artworkIds = emptyList(),
            ),
        )
    }

    // Prefetch the selected pack's rotation catalog so Start Ambient can skip the network wait.
    LaunchedEffect(selection, museumKind, stockCategory, contentEngine) {
        syncSourceSettings()
        val renewIds = selection.sourceIdsForAmbientLoad() ?: return@LaunchedEffect
        val key = selection.prefetchKey(museumKind, stockCategory)
        val loaded = withContext(Dispatchers.IO) {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = renewIds,
                    artworkIds = emptyList(),
                ),
            )
        }
        packCatalogCache[key] = loaded
    }

    ControlPlaneContent(
        openedFamily = openedFamily,
        selection = selection,
        startingAmbient = startingAmbient,
        onOpenFamily = { family ->
            openedFamily = family
            selection = PackSelection(family)
        },
        onSelectSubPack = { selection = it },
        onBackToHome = { openedFamily = null },
        onStartAmbient = {
            if (startingAmbient) return@ControlPlaneContent
            scope.launch {
                startingAmbient = true
                try {
                    syncSourceSettings()
                    val renewIds = selection.sourceIdsForAmbientLoad()
                    val prefetchKey = selection.prefetchKey(museumKind, stockCategory)
                    val loaded = if (renewIds != null) {
                        packCatalogCache[prefetchKey] ?: withContext(Dispatchers.IO) {
                            contentEngine.catalog(
                                PreparedRotation(
                                    sourceIds = renewIds,
                                    artworkIds = emptyList(),
                                ),
                            )
                        }.also { packCatalogCache[prefetchKey] = it }
                    } else {
                        catalog
                    }
                    val pool = resolvePackPool(loaded, selection)
                    val chosen = pool.randomOrNull()
                        ?: if (selection.allowsGenerativeAmbientFallback()) {
                            resolveAmbientArtwork(catalog, artworkId = null)
                        } else {
                            null
                        }
                    onStartAmbient(chosen, pool, renewIds)
                } finally {
                    startingAmbient = false
                }
            }
        },
        modifier = modifier,
        onCreateCustomFractal = onCreateCustomFractal,
        onOpenSettings = onOpenSettings,
    )
}

private fun PackSelection.prefetchKey(
    museumKind: MuseumSearchKind,
    stockCategory: StockPhotoCategory,
): String = "${family.name}|${subId.orEmpty()}|${museumKind.name}|${stockCategory.name}"

@Composable
fun ControlPlaneContent(
    openedFamily: PackFamily?,
    selection: PackSelection,
    onOpenFamily: (PackFamily) -> Unit,
    onSelectSubPack: (PackSelection) -> Unit,
    onBackToHome: () -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    startingAmbient: Boolean = false,
    onCreateCustomFractal: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val isTelevision = remember(configuration) {
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    }
    Box(modifier = modifier.fillMaxSize()) {
        if (isTelevision) {
            TvControlPlaneContent(
                openedFamily = openedFamily,
                selection = selection,
                onOpenFamily = onOpenFamily,
                onSelectSubPack = onSelectSubPack,
                onBackToHome = onBackToHome,
                onStartAmbient = onStartAmbient,
                modifier = Modifier.fillMaxSize(),
                onOpenSettings = onOpenSettings,
            )
        } else {
            PhoneControlPlaneContent(
                openedFamily = openedFamily,
                selection = selection,
                onOpenFamily = onOpenFamily,
                onSelectSubPack = onSelectSubPack,
                onBackToHome = onBackToHome,
                onStartAmbient = onStartAmbient,
                modifier = Modifier.fillMaxSize(),
                onCreateCustomFractal = onCreateCustomFractal,
                onOpenSettings = onOpenSettings,
            )
        }
        if (startingAmbient) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .testTag("starting_ambient_loader"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneControlPlaneContent(
    openedFamily: PackFamily?,
    selection: PackSelection,
    onOpenFamily: (PackFamily) -> Unit,
    onSelectSubPack: (PackSelection) -> Unit,
    onBackToHome: () -> Unit,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateCustomFractal: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    BackHandler(enabled = openedFamily != null) {
        onBackToHome()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("control_plane"),
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (openedFamily != null) {
                TopAppBar(
                    title = {
                        Text(stringResource(openedFamily.titleRes))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackToHome,
                            modifier = Modifier.testTag("pack_back"),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        titleContentColor = scheme.onSurface,
                        navigationIconContentColor = scheme.onSurface,
                    ),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (openedFamily == null) Modifier.statusBarsPadding() else Modifier),
        ) {
            if (openedFamily == null) {
                ControlPlaneHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    onOpenSettings = onOpenSettings,
                )
                Text(
                    text = stringResource(R.string.packs_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                PackGrid(
                    tiles = PackFamily.entries.map { it.homeTile() },
                    selected = null,
                    onTileClick = { onOpenFamily(it.selection.family) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                )
            } else {
                if (onCreateCustomFractal != null && selection.isGenartCustom()) {
                    TextButton(
                        onClick = onCreateCustomFractal,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("create_custom_fractal"),
                    ) {
                        Text(stringResource(R.string.custom_fractal_create))
                    }
                }
                PackGrid(
                    tiles = openedFamily.subPackTiles(),
                    selected = selection,
                    onTileClick = { tile: PackTile ->
                        if (tile.selection == selection) {
                            onStartAmbient()
                        } else {
                            onSelectSubPack(tile.selection)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun PackSubPackHeader(
    family: PackFamily,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .testTag("pack_back"),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Text(
            text = stringResource(family.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true, name = "Control plane packs")
@Composable
private fun ControlPlanePreview() {
    ArthurTheme {
        ControlPlaneContent(
            openedFamily = null,
            selection = PackSelection(PackFamily.Museum),
            onOpenFamily = {},
            onSelectSubPack = {},
            onBackToHome = {},
            onStartAmbient = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "TV control plane packs",
    device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
)
@Composable
private fun TvControlPlanePreview() {
    ArthurTheme {
        TvControlPlaneContent(
            openedFamily = null,
            selection = PackSelection(PackFamily.Museum),
            onOpenFamily = {},
            onSelectSubPack = {},
            onBackToHome = {},
            onStartAmbient = {},
        )
    }
}
