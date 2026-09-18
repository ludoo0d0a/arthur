package fr.geoking.arthur.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.dreams.DreamService
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.ScreensaverSettings
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.allowsGenerativeAmbientFallback
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.sourceIdsForAmbientLoad
import fr.geoking.arthur.ui.screens.AmbientScreenContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Minimal leanback launcher — starts ambient fullscreen (no browse). */
class AmbientActivity : ComponentActivity() {
    private val contentEngine: ContentEngine by inject()
    private val rotationSettings: RotationSettings by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val requested = intentArtwork()
        val rotate = intent.getBooleanExtra(EXTRA_ROTATE, false) ||
            intent.data?.getQueryParameter("rotate")?.toBooleanStrictOrNull() == true
        val stashedPool = if (rotate) AmbientRotationLaunch.pool else emptyList()
        val fallbackTitle = getString(R.string.app_name)
        setContent {
            ArthurTheme {
                var artwork by remember { mutableStateOf(requested) }
                var rotationPool by remember { mutableStateOf(stashedPool) }
                var isLoading by remember { mutableStateOf(false) }
                val intervalMs by rotationSettings.intervalMs.collectAsState()
                val scope = rememberCoroutineScope()
                LaunchedEffect(requested?.id, rotate) {
                    if (!rotate) {
                        val pinned = withContext(Dispatchers.IO) {
                            loadPinnedAmbient(contentEngine, requested)
                        }
                        artwork = pinned
                        return@LaunchedEffect
                    }
                    val initialLoaded = withContext(Dispatchers.IO) {
                        loadRotatingAmbient(contentEngine, requested, stashedPool)
                    }
                    if (initialLoaded.first.isNotEmpty()) {
                        rotationPool = initialLoaded.first
                    }
                    if (artwork == null) {
                        artwork = initialLoaded.second
                    }
                    val renewIds = AmbientRotationLaunch.renewSourceIds
                    if (renewIds != null) {
                        isLoading = true
                        try {
                            contentEngine.catalogFlow(
                                PreparedRotation(sourceIds = renewIds, artworkIds = emptyList()),
                            ).collect { emitted ->
                                if (emitted.isNotEmpty()) {
                                    rotationPool = emitted
                                    if (artwork == null) {
                                        artwork = emitted.randomOrNull()
                                    }
                                    AmbientRotationLaunch.prepare(emitted, renewIds)
                                }
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                }
                AmbientScreenContent(
                    title = artwork?.title ?: fallbackTitle,
                    artwork = artwork,
                    rotationPool = if (rotate) rotationPool else emptyList(),
                    isActive = true,
                    isLoading = isLoading,
                    intervalMs = intervalMs,
                    onNeedRenewPool = if (rotate) {
                        {
                            val ids = AmbientRotationLaunch.renewSourceIds
                            if (ids != null) {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        contentEngine.catalogFlow(
                                            PreparedRotation(sourceIds = ids, artworkIds = emptyList()),
                                        ).collect { emitted ->
                                            if (emitted.size >= 2) {
                                                rotationPool = emitted
                                                AmbientRotationLaunch.prepare(emitted, ids)
                                            }
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun intentArtwork(): Artwork? {
        val uri = intent.data
        val id = intent.getStringExtra(EXTRA_ARTWORK_ID)
            ?: uri?.getQueryParameter("artwork_id")
            ?: uri?.getQueryParameter("id")
            ?: return null
        val kindStr = intent.getStringExtra(EXTRA_ARTWORK_KIND)
            ?: uri?.getQueryParameter("artwork_kind")
            ?: uri?.getQueryParameter("kind")
        val kind = kindStr?.let { runCatching { ArtworkKind.valueOf(it) }.getOrNull() }
            ?: ArtworkKind.Genart
        return Artwork(
            id = id,
            title = intent.getStringExtra(EXTRA_ARTWORK_TITLE)
                ?: uri?.getQueryParameter("artwork_title")
                ?: uri?.getQueryParameter("title")
                ?: id,
            attribution = intent.getStringExtra(EXTRA_ATTRIBUTION)
                ?: uri?.getQueryParameter("attribution").orEmpty(),
            sourceId = intent.getStringExtra(EXTRA_SOURCE_ID)
                ?: uri?.getQueryParameter("source_id")
                ?: uri?.getQueryParameter("source").orEmpty(),
            kind = kind,
            remoteUrl = intent.getStringExtra(EXTRA_REMOTE_URL)
                ?: uri?.getQueryParameter("remote_url"),
            localPath = intent.getStringExtra(EXTRA_LOCAL_PATH)
                ?: uri?.getQueryParameter("local_path"),
        )
    }

    companion object {
        const val EXTRA_ARTWORK_ID = "artwork_id"
        const val EXTRA_ARTWORK_TITLE = "artwork_title"
        const val EXTRA_ARTWORK_KIND = "artwork_kind"
        const val EXTRA_SOURCE_ID = "source_id"
        const val EXTRA_ATTRIBUTION = "attribution"
        const val EXTRA_REMOTE_URL = "remote_url"
        const val EXTRA_LOCAL_PATH = "local_path"
        const val EXTRA_ROTATE = "rotate"

        fun intent(
            context: Context,
            artwork: Artwork? = null,
            rotate: Boolean = false,
        ): Intent =
            Intent(context, AmbientActivity::class.java).apply {
                putExtra(EXTRA_ROTATE, rotate)
                if (artwork != null) {
                    putExtra(EXTRA_ARTWORK_ID, artwork.id)
                    putExtra(EXTRA_ARTWORK_TITLE, artwork.title)
                    putExtra(EXTRA_ARTWORK_KIND, artwork.kind.name)
                    putExtra(EXTRA_SOURCE_ID, artwork.sourceId)
                    putExtra(EXTRA_ATTRIBUTION, artwork.attribution)
                    putExtra(EXTRA_REMOTE_URL, artwork.remoteUrl)
                    putExtra(EXTRA_LOCAL_PATH, artwork.localPath)
                }
            }
    }
}

/** TV Canvas screensaver / Dream — live generative Ambient via Compose. */
class ArthurDreamService : DreamService() {
    private val contentEngine: ContentEngine by inject()
    private val rotationSettings: RotationSettings by inject()
    private val screensaverSettings: ScreensaverSettings by inject()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true
        val fallbackTitle = getString(R.string.app_name)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    ArthurTheme {
                        var catalog by remember { mutableStateOf<List<Artwork>>(emptyList()) }
                        var artwork by remember { mutableStateOf<Artwork?>(null) }
                        val intervalMs by rotationSettings.intervalMs.collectAsState()
                        val defaultPack by screensaverSettings.defaultPack.collectAsState()
                        LaunchedEffect(defaultPack) {
                            val loaded = withContext(Dispatchers.IO) {
                                loadDreamAmbient(contentEngine, defaultPack)
                            }
                            catalog = loaded.first
                            artwork = loaded.second
                        }
                        AmbientScreenContent(
                            title = artwork?.title ?: fallbackTitle,
                            artwork = artwork,
                            rotationPool = catalog,
                            intervalMs = intervalMs,
                        )
                    }
                }
            },
        )
    }
}

/**
 * Prefer the Control Plane selection. Refresh from catalog when the id still exists
 * (fresher remote URL); otherwise keep the intent-stashed Artwork so we never swap to
 * an unrelated genart fallback. A null request stays null (placeholder) — do not invent
 * a generative piece when the Control Plane asked for museum/photo stills.
 */
internal suspend fun loadPinnedAmbient(
    contentEngine: ContentEngine,
    requested: Artwork?,
): Artwork? {
    if (requested == null) return null
    val catalog = contentEngine.catalog(
        PreparedRotation(
            sourceIds = emptyList(),
            artworkIds = emptyList(),
        ),
    )
    return resolveAmbientArtwork(catalog, requested.id) ?: requested
}

/**
 * Start ambient with random rotation: prefer the Control Plane pool (live URLs),
 * else fall back to a fresh engine catalog. Seed with the requested piece when present.
 * Never invent a generative fallback when the requested seed is absent — an empty
 * museum/photo pool should show the placeholder, not Drifting Particles.
 */
internal suspend fun loadRotatingAmbient(
    contentEngine: ContentEngine,
    requested: Artwork?,
    stashedPool: List<Artwork>,
): Pair<List<Artwork>, Artwork?> {
    val pool = stashedPool.ifEmpty {
        contentEngine.catalog(
            PreparedRotation(
                sourceIds = emptyList(),
                artworkIds = emptyList(),
            ),
        )
    }
    val artwork = when {
        requested == null -> pool.randomOrNull()
        pool.any { it.id == requested.id } ->
            pool.first { it.id == requested.id }
        else -> requested
    }
    return pool to artwork
}

/** Dream: rotate the selected screensaver pack catalog, or default ambient pack when none selected. */
internal suspend fun loadDreamAmbient(
    contentEngine: ContentEngine,
    selection: PackSelection? = null,
): Pair<List<Artwork>, Artwork?> {
    val activeSelection = selection ?: ScreensaverSettings.DEFAULT_PACK_SELECTION
    val renewIds = activeSelection.sourceIdsForAmbientLoad()
    val fullCatalog = contentEngine.catalog(
        PreparedRotation(
            sourceIds = renewIds ?: emptyList(),
            artworkIds = emptyList(),
        ),
    )
    val pool = resolvePackPool(fullCatalog, activeSelection)
    val chosen = pool.randomOrNull()
        ?: if (activeSelection.allowsGenerativeAmbientFallback()) {
            resolveAmbientArtwork(fullCatalog, artworkId = null)
        } else {
            null
        }
    val rotationPool = if (pool.isNotEmpty()) pool else fullCatalog
    return rotationPool to chosen
}
