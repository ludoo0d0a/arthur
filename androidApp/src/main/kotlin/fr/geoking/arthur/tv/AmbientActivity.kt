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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import fr.geoking.arthur.ui.screens.AmbientScreenContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Minimal leanback launcher — starts ambient fullscreen (no browse). */
class AmbientActivity : ComponentActivity() {
    private val contentEngine: ContentEngine by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val requested = intentArtwork()
        val rotate = intent.getBooleanExtra(EXTRA_ROTATE, false)
        val stashedPool = if (rotate) AmbientRotationLaunch.pool else emptyList()
        val fallbackTitle = getString(R.string.app_name)
        setContent {
            ArthurTheme {
                var artwork by remember { mutableStateOf(requested) }
                var rotationPool by remember { mutableStateOf(stashedPool) }
                LaunchedEffect(requested?.id, rotate) {
                    val loaded = withContext(Dispatchers.IO) {
                        if (rotate) {
                            loadRotatingAmbient(contentEngine, requested, stashedPool)
                        } else {
                            val pinned = loadPinnedAmbient(contentEngine, requested)
                            emptyList<Artwork>() to pinned
                        }
                    }
                    if (rotate && loaded.first.isNotEmpty()) {
                        rotationPool = loaded.first
                    }
                    artwork = loaded.second
                }
                AmbientScreenContent(
                    title = artwork?.title ?: fallbackTitle,
                    artwork = artwork,
                    rotationPool = if (rotate) rotationPool else emptyList(),
                )
            }
        }
    }

    private fun intentArtwork(): Artwork? {
        val id = intent.getStringExtra(EXTRA_ARTWORK_ID) ?: return null
        val kind = intent.getStringExtra(EXTRA_ARTWORK_KIND)
            ?.let { runCatching { ArtworkKind.valueOf(it) }.getOrNull() }
            ?: ArtworkKind.Genart
        return Artwork(
            id = id,
            title = intent.getStringExtra(EXTRA_ARTWORK_TITLE) ?: id,
            attribution = intent.getStringExtra(EXTRA_ATTRIBUTION).orEmpty(),
            sourceId = intent.getStringExtra(EXTRA_SOURCE_ID).orEmpty(),
            kind = kind,
            remoteUrl = intent.getStringExtra(EXTRA_REMOTE_URL),
            localPath = intent.getStringExtra(EXTRA_LOCAL_PATH),
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
                        LaunchedEffect(Unit) {
                            val loaded = withContext(Dispatchers.IO) {
                                loadDreamAmbient(contentEngine)
                            }
                            catalog = loaded.first
                            artwork = loaded.second
                        }
                        AmbientScreenContent(
                            title = artwork?.title ?: fallbackTitle,
                            artwork = artwork,
                            rotationPool = catalog,
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
 * an unrelated genart fallback.
 */
internal suspend fun loadPinnedAmbient(
    contentEngine: ContentEngine,
    requested: Artwork?,
): Artwork? {
    val catalog = contentEngine.catalog(
        PreparedRotation(
            sourceIds = emptyList(),
            artworkIds = emptyList(),
        ),
    )
    if (requested == null) {
        return resolveAmbientArtwork(catalog, artworkId = null)
    }
    return resolveAmbientArtwork(catalog, requested.id) ?: requested
}

/**
 * Start ambient with random rotation: prefer the Control Plane pool (live URLs),
 * else fall back to a fresh engine catalog. Seed with the requested piece when present.
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
        requested == null -> pool.randomOrNull() ?: resolveAmbientArtwork(pool, artworkId = null)
        pool.any { it.id == requested.id } ->
            pool.first { it.id == requested.id }
        else -> requested
    }
    return pool to artwork
}

/** Dream: rotate the full displayable catalog. */
internal suspend fun loadDreamAmbient(
    contentEngine: ContentEngine,
): Pair<List<Artwork>, Artwork?> {
    val catalog = contentEngine.catalog(
        PreparedRotation(
            sourceIds = emptyList(),
            artworkIds = emptyList(),
        ),
    )
    val artwork = resolveAmbientArtwork(catalog, artworkId = null)
    return catalog to artwork
}
