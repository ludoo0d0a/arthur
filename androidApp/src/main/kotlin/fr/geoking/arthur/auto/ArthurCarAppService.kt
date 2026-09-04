package fr.geoking.arthur.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.graphics.drawable.IconCompat
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Car App Library service for Android Auto displaying large artwork images via PaneTemplate.
 */
class ArthurCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return ArthurCarSession()
    }
}

class ArthurCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return ArtworkPaneScreen(carContext)
    }
}

class ArtworkPaneScreen(carContext: CarContext) : Screen(carContext), KoinComponent {
    private val contentEngine: ContentEngine by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var catalog: List<Artwork> = emptyList()
    private var current: Artwork? = null
    private var generation: Long = 0L
    private var isPlaying: Boolean = true
    private var rotationJob: Job? = null

    init {
        scope.launch {
            catalog = withContext(Dispatchers.IO) {
                runCatching {
                    contentEngine.catalog(PreparedRotation(emptyList(), emptyList()))
                }.getOrDefault(emptyList())
            }
            current = resolveAmbientArtwork(catalog, null)
            invalidate()
            if (isPlaying) startRotation()
        }
    }

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                delay(AmbientAlbumArt.ROTATION_INTERVAL_MS)
                if (isPlaying) advance(+1)
            }
        }
    }

    fun advance(delta: Int) {
        if (catalog.isEmpty()) return
        val index = catalog.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val nextIndex = if (delta >= 0) {
            AmbientAlbumArt.advanceIndex(index, catalog.size)
        } else {
            Math.floorMod(index - 1, catalog.size)
        }
        current = catalog[nextIndex]
        generation += 1
        invalidate()
    }

    fun togglePlay() {
        isPlaying = !isPlaying
        if (isPlaying) startRotation() else rotationJob?.cancel()
        invalidate()
    }

    fun isPlaying(): Boolean = isPlaying

    fun currentArtwork(): Artwork? = current

    override fun onGetTemplate(): Template {
        val art = current
        val paneBuilder = Pane.Builder()

        if (art != null) {
            val bitmap = AmbientStillRenderer.render(art, generation)
            val carIcon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()

            val row = Row.Builder()
                .setTitle(art.title)
                .addText(art.attribution)
                .setImage(carIcon, Row.IMAGE_TYPE_LARGE)
                .build()

            paneBuilder.addRow(row)
        } else {
            val row = Row.Builder()
                .setTitle("Arthur - Ambient Art")
                .addText("Chargement de la galerie...")
                .build()
            paneBuilder.addRow(row)
        }

        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Précédent")
                .setOnClickListener { advance(-1) }
                .build(),
        )
        paneBuilder.addAction(
            Action.Builder()
                .setTitle(if (isPlaying) "Pause" else "Lecture")
                .setOnClickListener { togglePlay() }
                .build(),
        )
        paneBuilder.addAction(
            Action.Builder()
                .setTitle("Suivant")
                .setOnClickListener { advance(+1) }
                .build(),
        )

        val header = Header.Builder()
            .setTitle("Arthur")
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
            .build()
    }
}
