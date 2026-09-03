package fr.geoking.arthur.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.dreams.DreamService
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
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
        val artworkId = intent.getStringExtra(EXTRA_ARTWORK_ID)
        val artwork = runBlocking {
            val catalog = contentEngine.catalog(
                PreparedRotation(
                    sourceIds = emptyList(),
                    artworkIds = emptyList(),
                ),
            )
            catalog.firstOrNull { it.id == artworkId } ?: catalog.firstOrNull()
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

/** TV Canvas screensaver / Dream — metadata/still only (no live Compose engines). */
class ArthurDreamService : DreamService() {
    private val contentEngine: ContentEngine by inject()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val title = runBlocking {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = listOf(BundledPackSource.ID),
                    artworkIds = emptyList(),
                ),
            ).firstOrNull()?.title ?: getString(R.string.app_name)
        }
        setContentView(
            TextView(this).apply {
                text = title
                textSize = 32f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF000000.toInt())
                setPadding(64, 64, 64, 64)
            },
        )
    }
}
