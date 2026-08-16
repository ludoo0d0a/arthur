package fr.geoking.arthur.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.dreams.DreamService
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import fr.geoking.arthur.R
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
        val title = runBlocking {
            contentEngine.catalog(
                PreparedRotation(
                    sourceIds = listOf(BundledPackSource.ID),
                    artworkIds = emptyList(),
                ),
            ).firstOrNull()?.title ?: getString(R.string.app_name)
        }
        val tv = TextView(this).apply {
            text = title
            textSize = 28f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setBackgroundColor(0xFF1A1A2E.toInt())
            setPadding(48, 48, 48, 48)
            tag = "ambient_title"
        }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(0xFF1A1A2E.toInt())
                addView(tv)
            },
        )
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
