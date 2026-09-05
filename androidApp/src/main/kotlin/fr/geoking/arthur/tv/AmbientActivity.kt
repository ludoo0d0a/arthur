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
        val artworkId = intent.getStringExtra(EXTRA_ARTWORK_ID)
        val fallbackTitle = getString(R.string.app_name)
        setContent {
            ArthurTheme {
                var catalog by remember { mutableStateOf<List<Artwork>>(emptyList()) }
                var artwork by remember { mutableStateOf<Artwork?>(null) }
                LaunchedEffect(artworkId) {
                    val loaded = withContext(Dispatchers.IO) {
                        loadAmbient(contentEngine, artworkId)
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
    }

    companion object {
        const val EXTRA_ARTWORK_ID = "artwork_id"

        fun intent(context: Context, artworkId: String? = null): Intent =
            Intent(context, AmbientActivity::class.java).apply {
                if (artworkId != null) putExtra(EXTRA_ARTWORK_ID, artworkId)
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
                                loadAmbient(contentEngine, artworkId = null)
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
 * Loads the ambient catalog off the main thread.
 * Does **not** bake genart stills — phone/TV Ambient uses live engines; Auto bakes on demand.
 */
internal suspend fun loadAmbient(
    contentEngine: ContentEngine,
    artworkId: String?,
): Pair<List<Artwork>, Artwork?> {
    val catalog = contentEngine.catalog(
        PreparedRotation(
            sourceIds = emptyList(),
            artworkIds = emptyList(),
        ),
    )
    val artwork = resolveAmbientArtwork(catalog, artworkId)
    return catalog to artwork
}
