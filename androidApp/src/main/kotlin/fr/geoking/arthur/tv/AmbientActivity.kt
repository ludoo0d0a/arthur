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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.ui.screens.AmbientScreenContent
import kotlinx.coroutines.runBlocking
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
        val artwork = runBlocking {
            val catalog = contentEngine.catalog(
                PreparedRotation(
                    sourceIds = emptyList(),
                    artworkIds = emptyList(),
                ),
            )
            resolveAmbientArtwork(catalog, artworkId)
        }
        val title = artwork?.title ?: getString(R.string.app_name)
        setContent {
            ArthurTheme {
                AmbientScreenContent(title = title, artwork = artwork)
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
        val artwork = runBlocking {
            val catalog = contentEngine.catalog(
                PreparedRotation(
                    sourceIds = emptyList(),
                    artworkIds = emptyList(),
                ),
            )
            resolveAmbientArtwork(catalog, null)
        }
        val title = artwork?.title ?: getString(R.string.app_name)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    ArthurTheme {
                        AmbientScreenContent(title = title, artwork = artwork)
                    }
                }
            },
        )
    }
}
