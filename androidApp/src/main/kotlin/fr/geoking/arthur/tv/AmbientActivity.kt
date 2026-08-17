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
import fr.geoking.arthur.phone.AmbientScreenContent
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.source.BundledPackSource
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
        val title = runBlocking {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = listOf(BundledPackSource.ID),
                    artworkIds = emptyList(),
                ),
            ).firstOrNull()?.title ?: getString(R.string.app_name)
        }
        setContent {
            ArthurTheme {
                AmbientScreenContent(title = title)
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, AmbientActivity::class.java)
    }
}

/** TV Canvas screensaver / Dream. */
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
