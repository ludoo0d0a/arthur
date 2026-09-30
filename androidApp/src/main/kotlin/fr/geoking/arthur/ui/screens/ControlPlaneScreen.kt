package fr.geoking.arthur.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MuseumSearchKind
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.debug.DebugLogger
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.source.MuseumSearchSettings
import fr.geoking.arthur.source.ScreensaverSettings
import fr.geoking.arthur.source.StockPhotoSettings
import fr.geoking.arthur.source.rememberArtworkImageCache
import fr.geoking.arthur.ui.components.ArtworkCard
import fr.geoking.arthur.ui.components.ControlPlaneHeader
import org.koin.core.context.GlobalContext
import fr.geoking.arthur.ui.components.GenartTopic
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackGrid
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PackTile
import fr.geoking.arthur.ui.components.allowsGenerativeAmbientFallback
import fr.geoking.arthur.ui.components.defaultSubId
import fr.geoking.arthur.ui.components.genartTopicOrNull
import fr.geoking.arthur.ui.components.homeTile
import fr.geoking.arthur.ui.components.isGenartCustom
import fr.geoking.arthur.ui.components.museumTopicOrNull
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.sourceIdsForAmbientLoad
import fr.geoking.arthur.ui.components.stockCategoryOrNull
import fr.geoking.arthur.ui.components.subPackTiles
import fr.geoking.arthur.ui.components.videoSourceOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ControlPlaneScreen(
    contentEngine: ContentEngine,
    onStartAmbient: (artwork: Artwork?, pool: List<Artwork>, renewSourceIds: List<String>?) -> Unit,
    modifier: Modifier = Modifier,
    initialCatalog: List<Artwork>? = null,
    onCreateCustomFractal: (() -> Unit)? = null,
    stockPhotoSettings: StockPhotoSettings? = null,
    museumSearchSettings: MuseumSearchSettings? = null,
    screensaverSettings: ScreensaverSettings? = null,
    onOpenSettings: (() -> Unit)? = null,
    packOwnership: PackOwnership = PackOwnership.NONE,
    onOpenMarketplace: ((highlightPackId: String?) -> Unit)? = null,
) {
    var catalog by remember { mutableStateOf(initialCatalog.orEmpty()) }
    var openedFamily by remember { mutableStateOf<PackFamily?>(null) }
    var selection by remember { mutableStateOf(PackSelection(PackFamily.Museum)) }
    val packCatalogCache = remember { mutableMapOf<String, List<Artwork>>() }
    val imageCache = rememberArtworkImageCache()
    val scope = rememberCoroutineScope()

    val debugLogger = remember {
        runCatching { GlobalContext.get().get<DebugLogger>() }.getOrNull()
    }

    val defaultScreensaver by screensaverSettings?.defaultPack?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val ambientAudioSettings = remember {
        runCatching { GlobalContext.get().get<AmbientAudioSettings>() }.getOrNull()
    }

    val stockCategory = selection.stockCategoryOrNull()
        ?: if (selection.videoSourceOrNull() != null || selection.museumTopicOrNull() != null) {
            StockPhotoCategory.Random
        } else {
            stockPhotoSettings?.category ?: StockPhotoCategory.Random
        }

    val museumKind = when (selection.family) {
        PackFamily.Painting -> MuseumSearchKind.Painting
        PackFamily.Sculpture -> MuseumSearchKind.Sculpture
        PackFamily.Museum -> MuseumSearchKind.All
        PackFamily.Photo -> MuseumSearchKind.Photo
        PackFamily.Personal -> MuseumSearchKind.Photo
        else -> MuseumSearchKind.All
    }

    fun syncSourceSettings(forSelection: PackSelection = selection) {
        val cat = forSelection.stockCategoryOrNull()
            ?: if (forSelection.videoSourceOrNull() != null || forSelection.museumTopicOrNull() != null) {
                StockPhotoCategory.Random
            } else {
                stockPhotoSettings?.category ?: StockPhotoCategory.Random
            }
        val kind = when (forSelection.family) {
            PackFamily.Painting -> MuseumSearchKind.Painting
            PackFamily.Sculpture -> MuseumSearchKind.Sculpture
            PackFamily.Museum -> MuseumSearchKind.All
            PackFamily.Photo -> MuseumSearchKind.Photo
            PackFamily.Personal -> MuseumSearchKind.Photo
            else -> MuseumSearchKind.All
        }
        when {
            forSelection.stockCategoryOrNull() != null -> {
                if (stockPhotoSettings != null) {
                    stockPhotoSettings.category = cat
                }
            }
            forSelection.videoSourceOrNull() != null -> {
                if (stockPhotoSettings != null) {
                    stockPhotoSettings.category = StockPhotoCategory.Random
                }
            }
        }
        if (stockPhotoSettings != null) {
            stockPhotoSettings.contentKind =
                if (forSelection.family == PackFamily.Video) ArtworkKind.Video else ArtworkKind.Photo
        }
        if (museumSearchSettings != null) {
            museumSearchSettings.kind = kind
        }
    }

    fun launchAmbient(forSelection: PackSelection) {
        syncSourceSettings(forSelection)
        val renewIds = forSelection.sourceIdsForAmbientLoad()
        val cat = forSelection.stockCategoryOrNull()
            ?: if (forSelection.videoSourceOrNull() != null || forSelection.museumTopicOrNull() != null) {
                StockPhotoCategory.Random
            } else {
                stockPhotoSettings?.category ?: StockPhotoCategory.Random
            }
        val kind = when (forSelection.family) {
            PackFamily.Painting -> MuseumSearchKind.Painting
            PackFamily.Sculpture -> MuseumSearchKind.Sculpture
            PackFamily.Museum -> MuseumSearchKind.All
            PackFamily.Photo -> MuseumSearchKind.Photo
            PackFamily.Personal -> MuseumSearchKind.Photo
            else -> MuseumSearchKind.All
        }
        val prefetchKey = forSelection.prefetchKey(kind, cat)
        scope.launch {
            val preferred: (Artwork) -> Boolean = { art ->
                art.isGenerative ||
                    !art.localPath.isNullOrBlank() ||
                    imageCache.hasImage(art.id)
            }
            val livePool = packCatalogCache[prefetchKey] ?: withContext(Dispatchers.IO) {
                runCatching {
                    contentEngine.catalog(
                        PreparedRotation(
                            sourceIds = renewIds.orEmpty(),
                            artworkIds = emptyList(),
                        ),
                    )
                }.getOrDefault(emptyList())
            }.also { loaded ->
                if (loaded.isNotEmpty()) packCatalogCache[prefetchKey] = loaded
            }
            val cachedPool = livePool.ifEmpty {
                withContext(Dispatchers.IO) {
                    imageCache.loadCachedArtworks(renewIds)
                }.ifEmpty {
                    if (forSelection.allowsGenerativeAmbientFallback()) catalog else emptyList()
                }
            }
            val pool = resolvePackPool(cachedPool, forSelection)
            val rotationPool = AmbientAlbumArt.sampleRotationPool(
                pool = pool,
                isPreferred = preferred,
            )
            val chosen = rotationPool.firstOrNull()
                ?: if (forSelection.allowsGenerativeAmbientFallback()) {
                    resolveAmbientArtwork(catalog, artworkId = null)
                } else {
                    null
                }
            onStartAmbient(chosen, rotationPool, renewIds)
        }
    }

    // Genart/Fractal/CustomFractal are procedural (no network) — safe to load eagerly so
    // the Genart pack and the generative Ambient fallback have something to read from
    // catalog without ever waiting on an HTTP call at screen entry.
    LaunchedEffect(contentEngine, initialCatalog) {
        syncSourceSettings()
        if (initialCatalog != null) return@LaunchedEffect

        val startTime = System.currentTimeMillis()
        catalog = contentEngine.catalog(
            PreparedRotation(
                sourceIds = listOf(GenartSource.ID, FractalSource.ID, CustomFractalSource.ID),
                artworkIds = emptyList(),
            ),
        )
        val duration = System.currentTimeMillis() - startTime
        debugLogger?.recordLoadDuration(duration)
    }

    // Prefetch the selected pack's rotation catalog so Start Ambient can skip the network wait.
    LaunchedEffect(selection, museumKind, stockCategory, contentEngine) {
        syncSourceSettings()
        val renewIds = selection.sourceIdsForAmbientLoad() ?: return@LaunchedEffect
        val key = selection.prefetchKey(museumKind, stockCategory)
        val startTime = System.currentTimeMillis()
        val loaded = withContext(Dispatchers.IO) {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = renewIds,
                    artworkIds = emptyList(),
                ),
            )
        }
        val duration = System.currentTimeMillis() - startTime
        debugLogger?.recordLoadDuration(duration)
        packCatalogCache[key] = loaded
    }

    ControlPlaneContent(
        openedFamily = openedFamily,
        selection = selection,
        catalog = catalog,
        onOpenFamily = { family ->
            val home = family.homeTile()
            if (home.isLocked(packOwnership)) {
                onOpenMarketplace?.invoke(home.sellablePackId)
            } else if (family == PackFamily.Personal) {
                selection = PackSelection(PackFamily.Personal)
                openedFamily = family
            } else {
                openedFamily = family
                selection = PackSelection(family, family.defaultSubId())
            }
        },
        onSelectSubPack = { selection = it },
        onBackToHome = { openedFamily = null },
        onStartAmbient = { launchAmbient(selection) },
        onStartAmbientArtwork = { artwork ->
            syncSourceSettings()
            val renewIds = selection.sourceIdsForAmbientLoad()
            val pool = resolvePackPool(catalog, selection)
            val rotationPool = AmbientAlbumArt.sampleRotationPool(
                pool = pool,
                seed = artwork,
                isPreferred = { art ->
                    art.isGenerative ||
                        !art.localPath.isNullOrBlank() ||
                        imageCache.hasImage(art.id)
                },
            )
            onStartAmbient(artwork, rotationPool, renewIds)
        },
        onStartMediaPlayer = {
            ambientAudioSettings?.setEnabled(true)
            val mediaSelection = when {
                openedFamily != null -> selection
                defaultScreensaver != null -> defaultScreensaver!!
                else -> PackSelection(PackFamily.Genart, PackFamily.Genart.defaultSubId())
            }
            val home = mediaSelection.family.homeTile()
            if (home.isLocked(packOwnership)) {
                onOpenMarketplace?.invoke(home.sellablePackId)
            } else {
                launchAmbient(mediaSelection)
            }
        },
        modifier = modifier,
        onCreateCustomFractal = onCreateCustomFractal,
        onOpenSettings = onOpenSettings,
        defaultScreensaverSelection = defaultScreensaver,
        onSetDefaultScreensaver = screensaverSettings?.let { settings -> { settings.setDefaultPack(it) } },
        packOwnership = packOwnership,
        onOpenMarketplace = onOpenMarketplace,
        onOpenMediaPlayer = {
            syncSourceSettings()
            val renewIds = selection.sourceIdsForAmbientLoad()
            val prefetchKey = selection.prefetchKey(museumKind, stockCategory)
            scope.launch {
                val preferred: (Artwork) -> Boolean = { art ->
                    art.isGenerative ||
                        !art.localPath.isNullOrBlank() ||
                        imageCache.hasImage(art.id)
                }
                val livePool = packCatalogCache[prefetchKey] ?: withContext(Dispatchers.IO) {
                    runCatching {
                        contentEngine.catalog(
                            PreparedRotation(
                                sourceIds = renewIds.orEmpty(),
                                artworkIds = emptyList(),
                            ),
                        )
                    }.getOrDefault(emptyList())
                }.also { loaded ->
                    if (loaded.isNotEmpty()) packCatalogCache[prefetchKey] = loaded
                }
                val cachedPool = livePool.ifEmpty {
                    withContext(Dispatchers.IO) {
                        imageCache.loadCachedArtworks(renewIds)
                    }.ifEmpty {
                        if (selection.allowsGenerativeAmbientFallback()) catalog else emptyList()
                    }
                }
                val pool = resolvePackPool(cachedPool, selection)
                val rotationPool = AmbientAlbumArt.sampleRotationPool(
                    pool = pool,
                    isPreferred = preferred,
                )
                val chosen = rotationPool.firstOrNull()
                    ?: if (selection.allowsGenerativeAmbientFallback()) {
                        resolveAmbientArtwork(catalog, artworkId = null)
                    } else {
                        null
                    }
                onStartAmbient(chosen, rotationPool, renewIds)
            }
        },
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
    onStartAmbientArtwork: (Artwork) -> Unit = {},
    onStartMediaPlayer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    catalog: List<Artwork> = emptyList(),
    startingAmbient: Boolean = false,
    onCreateCustomFractal: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    defaultScreensaverSelection: PackSelection? = null,
    onSetDefaultScreensaver: ((PackSelection) -> Unit)? = null,
    packOwnership: PackOwnership = PackOwnership.NONE,
    onOpenMarketplace: ((highlightPackId: String?) -> Unit)? = null,
    onOpenMediaPlayer: (() -> Unit)? = null,
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
                catalog = catalog,
                onOpenSettings = onOpenSettings,
                onOpenMediaPlayer = onOpenMediaPlayer,
                defaultScreensaverSelection = defaultScreensaverSelection,
                onSetDefaultScreensaver = onSetDefaultScreensaver,
                packOwnership = packOwnership,
                onOpenMarketplace = onOpenMarketplace,
            )
        } else {
            PhoneControlPlaneContent(
                openedFamily = openedFamily,
                selection = selection,
                onOpenFamily = onOpenFamily,
                onSelectSubPack = onSelectSubPack,
                onBackToHome = onBackToHome,
                onStartAmbient = onStartAmbient,
                onStartAmbientArtwork = onStartAmbientArtwork,
                onStartMediaPlayer = onStartMediaPlayer,
                modifier = Modifier.fillMaxSize(),
                catalog = catalog,
                onCreateCustomFractal = onCreateCustomFractal,
                onOpenSettings = onOpenSettings,
                onOpenMediaPlayer = onOpenMediaPlayer,
                packOwnership = packOwnership,
                onOpenMarketplace = onOpenMarketplace,
            )
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
    onStartAmbientArtwork: (Artwork) -> Unit,
    onStartMediaPlayer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    catalog: List<Artwork> = emptyList(),
    onCreateCustomFractal: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenMediaPlayer: (() -> Unit)? = null,
    packOwnership: PackOwnership = PackOwnership.NONE,
    onOpenMarketplace: ((highlightPackId: String?) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var searchQuery by remember(openedFamily, selection) { mutableStateOf("") }
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
                    actions = {
                        if (onStartMediaPlayer != null) {
                            IconButton(
                                onClick = onStartMediaPlayer,
                                modifier = Modifier.testTag("media_player_button"),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_circle),
                                    contentDescription = stringResource(R.string.cd_media_player),
                                    tint = scheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        titleContentColor = scheme.onSurface,
                        navigationIconContentColor = scheme.onSurface,
                        actionIconContentColor = scheme.onSurface,
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
                    onOpenMediaPlayer = onOpenMediaPlayer,
                )
                Text(
                    text = stringResource(R.string.packs_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                PackGrid(
                    tiles = PackFamily.entries.map { it.homeTile(catalog) },
                    selected = null,
                    onTileClick = { onOpenFamily(it.selection.family) },
                    isLocked = { it.isLocked(packOwnership) },
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
                val isGenartFamily = openedFamily == PackFamily.Genart

                PackGrid(
                    tiles = openedFamily.subPackTiles(catalog),
                    selected = selection,
                    onTileClick = { tile: PackTile ->
                        if (tile.isLocked(packOwnership)) {
                            onOpenMarketplace?.invoke(tile.sellablePackId)
                        } else if (tile.selection == selection) {
                            onStartAmbient()
                        } else {
                            onSelectSubPack(tile.selection)
                        }
                    },
                    isLocked = { it.isLocked(packOwnership) },
                    modifier = Modifier.weight(if (isGenartFamily) 0.38f else 1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = if (isGenartFamily) 8.dp else 24.dp,
                    ),
                )

                if (isGenartFamily) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .testTag("genart_search_input"),
                        placeholder = { Text(stringResource(R.string.search_genart_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("genart_search_clear"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.custom_fractal_clear),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )

                    val allGenarts = remember(catalog, selection) { resolvePackPool(catalog, selection) }
                    val filteredGenarts = remember(allGenarts, searchQuery) {
                        if (searchQuery.isBlank()) {
                            allGenarts
                        } else {
                            allGenarts.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxWidth()
                            .testTag("genart_all_list"),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredGenarts, key = { it.id }) { artwork ->
                            ArtworkCard(
                                artwork = artwork,
                                selected = false,
                                onClick = { onStartAmbientArtwork(artwork) },
                            )
                        }
                    }
                }
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
    name = "Phone landscape packs",
    device = "spec:width=800dp,height=360dp,dpi=420",
)
@Composable
private fun ControlPlaneLandscapePreview() {
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
