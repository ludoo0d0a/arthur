package fr.geoking.arthur.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.resolveAmbientArtwork
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.RotationSettings
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
 *
 * Host constraints applied:
 * - Pane actions ≤ 2 (play/pause primary only here)
 * - ActionStrip ≤ 2, icon-only for prev/next (no label buttons in strip)
 * - Pane rows capped via [ConstraintManager.CONTENT_LIMIT_TYPE_PANE]
 * - Loading vs rows mutually exclusive
 * - [onGetTemplate] never throws — errors surface as [MessageTemplate]
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
    private val rotationSettings: RotationSettings by inject()
    private val imageCache: ArtworkImageCache by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var catalog: List<Artwork> = emptyList()
    private var current: Artwork? = null
    private var generation: Long = 0L
    private var isPlaying: Boolean = true
    private var rotationJob: Job? = null
    private var renderJob: Job? = null
    private var loaded: Boolean = false

    @Volatile
    private var renderedBitmap: android.graphics.Bitmap? = null
    @Volatile
    private var renderedArtId: String? = null
    @Volatile
    private var renderedGen: Long = -1L

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    rotationJob?.cancel()
                    renderJob?.cancel()
                    scope.cancel()
                }
            },
        )
        scope.launch {
            catalog = withContext(Dispatchers.IO) {
                runCatching {
                    contentEngine.catalog(PreparedRotation(emptyList(), emptyList()))
                }.getOrDefault(emptyList())
            }
            runCatching {
                current = resolveAmbientArtwork(catalog, null)
                loaded = true
                scheduleAsyncRender()
                invalidate()
                if (isPlaying) startRotation()
            }
        }
    }

    private fun scheduleAsyncRender() {
        val art = current ?: return
        val gen = generation
        if (art.id == renderedArtId && gen == renderedGen && renderedBitmap != null) {
            return
        }
        renderJob?.cancel()
        renderJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    AmbientStillRenderer.render(art, gen, imageCache)
                }.getOrNull()
            }
            if (isActive && bitmap != null) {
                renderedBitmap = bitmap
                renderedArtId = art.id
                renderedGen = gen
                invalidate()
            }
        }
    }

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                delay(rotationSettings.intervalMs.value)
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
        scheduleAsyncRender()
        // Restart the interval so a manual skip doesn't get auto-advanced immediately.
        if (isPlaying) startRotation()
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
        return try {
            buildPaneTemplate()
        } catch (e: Throwable) {
            buildErrorTemplate(e)
        }
    }

    private fun buildErrorTemplate(e: Throwable): Template {
        val detail = e.message?.take(300)?.takeIf { it.isNotBlank() }
        val message = buildString {
            append(e::class.simpleName ?: carContext.getString(R.string.car_error_generic))
            if (detail != null) {
                append(": ")
                append(detail)
            }
        }.take(500)
        val logo = CarIcon.Builder(IconCompat.createWithResource(carContext, R.mipmap.ic_launcher)).build()
        return MessageTemplate.Builder(message)
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .setIcon(logo)
            .setDebugMessage(e)
            .build()
    }

    private fun buildPaneTemplate(): Template {
        val header = Header.Builder()
            .setTitle(carContext.getString(R.string.app_name))
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        if (!loaded) {
            return PaneTemplate.Builder(
                Pane.Builder().setLoading(true).build(),
            )
                .setHeader(header)
                .build()
        }

        val art = current
        val paneBuilder = Pane.Builder()
        val rowLimit = paneRowLimit()

        if (art != null && rowLimit > 0) {
            val bitmap = if (art.id == renderedArtId && generation == renderedGen && renderedBitmap != null) {
                renderedBitmap!!
            } else {
                scheduleAsyncRender()
                AmbientStillRenderer.renderPlaceholder(art, generation)
            }
            val carIcon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
            val rowBuilder = Row.Builder()
                .setTitle(art.title.ifBlank { carContext.getString(R.string.app_name) })
            if (art.attribution.isNotBlank()) {
                rowBuilder.addText(art.attribution)
            }
            rowBuilder.setImage(carIcon, Row.IMAGE_TYPE_LARGE)
            paneBuilder.addRow(rowBuilder.build())
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.app_name))
                    .addText(carContext.getString(R.string.car_gallery_empty))
                    .build(),
            )
        }

        // Pane actions: max 2. Primary play/pause only; prev/next live in ActionStrip.
        paneBuilder.addAction(
            Action.Builder()
                .setTitle(
                    carContext.getString(
                        if (isPlaying) R.string.car_pause else R.string.car_play,
                    ),
                )
                .setFlags(Action.FLAG_PRIMARY)
                .setOnClickListener { togglePlay() }
                .build(),
        )

        val strip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_car_previous),
                        ).build(),
                    )
                    .setOnClickListener { advance(-1) }
                    .build(),
            )
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_car_next),
                        ).build(),
                    )
                    .setOnClickListener { advance(+1) }
                    .build(),
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
            .setActionStrip(strip)
            .build()
    }

    private fun paneRowLimit(): Int {
        return try {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PANE)
        } catch (_: Exception) {
            4
        }
    }
}
