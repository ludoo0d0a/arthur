package fr.geoking.arthur.auto

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.content.ContextCompat
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
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.subPackTiles
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

/** Hard caps so host content limits cannot densify the Spotify-style dashboard. */
internal const val MAX_HOME_GRID_ITEMS = 5
internal const val MAX_SUB_GRID_ITEMS = 30

private const val COVER_ICON_SIZE_PX = 512

internal fun gridContentLimit(carContext: CarContext, maxItems: Int): Int {
    val hostLimit = try {
        carContext.getCarService(ConstraintManager::class.java)
            ?.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID) ?: maxItems
    } catch (_: Exception) {
        maxItems
    }
    return minOf(hostLimit, maxItems)
}

internal fun coverCarIcon(carContext: CarContext, @DrawableRes coverRes: Int): CarIcon {
    val bitmap = decodeCoverBitmap(carContext, coverRes)
    val icon = if (bitmap != null) {
        IconCompat.createWithBitmap(bitmap)
    } else {
        IconCompat.createWithResource(carContext, coverRes)
    }
    return CarIcon.Builder(icon).build()
}

private fun decodeCoverBitmap(carContext: CarContext, @DrawableRes coverRes: Int): Bitmap? {
    return runCatching {
        val drawable = ContextCompat.getDrawable(carContext, coverRes) ?: return null
        val bitmap = Bitmap.createBitmap(COVER_ICON_SIZE_PX, COVER_ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, COVER_ICON_SIZE_PX, COVER_ICON_SIZE_PX)
        drawable.draw(canvas)
        bitmap
    }.getOrNull()
}

private fun carErrorTemplate(carContext: CarContext, e: Throwable): Template {
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

/**
 * Car App Library service for Android Auto displaying large artwork images via PaneTemplate.
 *
 * Host constraints applied:
 * - Pane actions ≤ 2 (play/pause primary only here)
 * - ActionStrip ≤ 2, icon-only for prev/next (no label buttons in strip)
 * - Pane rows capped via [ConstraintManager.CONTENT_LIMIT_TYPE_PANE]
 * - Pack grids use [SectionedItemTemplate] + [GridSection.ITEM_SIZE_EXTRA_LARGE], hard-capped
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
        return PackSelectionScreen(carContext)
    }
}

/**
 * Screen displaying the available pack families on the Android Auto dashboard.
 */
@OptIn(ExperimentalCarApi::class)
class PackSelectionScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return try {
            buildTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildTemplate(): Template {
        val gridLimit = gridContentLimit(carContext, MAX_HOME_GRID_ITEMS)
        val families = PackFamily.entries
            .filter { it != PackFamily.Video }
            .take(gridLimit)

        val sectionBuilder = GridSection.Builder()
            .setItemSize(GridSection.ITEM_SIZE_EXTRA_LARGE)
            .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_UNSET)

        families.forEach { family ->
            val item = GridItem.Builder()
                .setTitle(carContext.getString(family.titleRes))
                .setImage(coverCarIcon(carContext, family.coverRes), GridItem.IMAGE_TYPE_LARGE)
                .setOnClickListener {
                    screenManager.push(SubPackSelectionScreen(carContext, family))
                }
                .build()
            sectionBuilder.addItem(item)
        }

        val settingsAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_settings),
                ).build(),
            )
            .setOnClickListener {
                screenManager.push(CarSettingsScreen(carContext))
            }
            .build()

        val header = Header.Builder()
            .setTitle(carContext.getString(R.string.packs_section))
            .setStartHeaderAction(Action.APP_ICON)
            .addEndHeaderAction(settingsAction)
            .build()

        return SectionedItemTemplate.Builder()
            .setHeader(header)
            .addSection(sectionBuilder.build())
            .build()
    }
}

/**
 * Screen displaying the sub-packs / topics for a given [PackFamily].
 */
@OptIn(ExperimentalCarApi::class)
/**
 * Screen displaying application settings (such as rotation interval) in Android Auto.
 */
class CarSettingsScreen(carContext: CarContext) : Screen(carContext), KoinComponent {
    private val rotationSettings: RotationSettings by inject()

    override fun onGetTemplate(): Template {
        return try {
            buildTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildTemplate(): Template {
        val currentInterval = rotationSettings.intervalMs.value
        val listBuilder = ItemList.Builder()

        RotationSettings.OPTIONS_MS.forEach { ms ->
            val label = if (ms < 60_000L) {
                carContext.getString(R.string.rotation_interval_seconds, (ms / 1_000L).toInt())
            } else {
                carContext.getString(R.string.rotation_interval_minutes, (ms / 60_000L).toInt())
            }

            val rowBuilder = Row.Builder()
                .setTitle(label)

            if (ms == currentInterval) {
                rowBuilder.addText("✓")
            }

            rowBuilder.setOnClickListener {
                rotationSettings.setIntervalMs(ms)
                invalidate()
            }

            listBuilder.addItem(rowBuilder.build())
        }

        val header = Header.Builder()
            .setTitle(carContext.getString(R.string.screen_rotation_interval))
            .setStartHeaderAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setHeader(header)
            .setSingleList(listBuilder.build())
            .build()
    }
}

class SubPackSelectionScreen(
    carContext: CarContext,
    val family: PackFamily,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return try {
            buildTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildTemplate(): Template {
        val gridLimit = gridContentLimit(carContext, MAX_SUB_GRID_ITEMS)
        val tiles = family.subPackTiles().take(gridLimit)

        val sectionBuilder = GridSection.Builder()
            .setItemSize(GridSection.ITEM_SIZE_EXTRA_LARGE)
            .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_UNSET)

        tiles.forEach { tile ->
            val item = GridItem.Builder()
                .setTitle(carContext.getString(tile.titleRes))
                .setImage(coverCarIcon(carContext, tile.coverRes), GridItem.IMAGE_TYPE_LARGE)
                .setOnClickListener {
                    screenManager.push(ArtworkPaneScreen(carContext, tile.selection))
                }
                .build()
            sectionBuilder.addItem(item)
        }

        val header = Header.Builder()
            .setTitle(carContext.getString(family.titleRes))
            .setStartHeaderAction(Action.BACK)
            .build()

        return SectionedItemTemplate.Builder()
            .setHeader(header)
            .addSection(sectionBuilder.build())
            .build()
    }
}

class ArtworkPaneScreen(
    carContext: CarContext,
    val packSelection: PackSelection = PackSelection(PackFamily.Museum),
) : Screen(carContext), KoinComponent {
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
            val fullCatalog = withContext(Dispatchers.IO) {
                runCatching {
                    contentEngine.catalog(PreparedRotation(emptyList(), emptyList()))
                }.getOrDefault(emptyList())
            }
            catalog = resolvePackPool(fullCatalog, packSelection).ifEmpty { fullCatalog }
            runCatching {
                current = resolveAmbientArtwork(catalog, null)
                scheduleAsyncRender()
                if (isPlaying) startRotation()
            }
            loaded = true
            invalidate()
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
            .setTitle(carContext.getString(packSelection.family.titleRes))
            .setStartHeaderAction(Action.BACK)
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
            val hasFreshRender = art.id == renderedArtId && generation == renderedGen && renderedBitmap != null
            if (!hasFreshRender) {
                scheduleAsyncRender()
            }
            val rowBuilder = Row.Builder()
                .setTitle(art.title.ifBlank { carContext.getString(R.string.app_name) })
            if (art.attribution.isNotBlank()) {
                rowBuilder.addText(art.attribution)
            }
            // The big picture is best-effort: if rendering/encoding it fails, the row still
            // shows title/attribution instead of falling back to the whole error template.
            val bigPicture = runCatching {
                val bitmap = if (hasFreshRender) renderedBitmap!! else AmbientStillRenderer.renderPlaceholder(art, generation)
                CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
            }.getOrNull()
            if (bigPicture != null) {
                rowBuilder.setImage(bigPicture, Row.IMAGE_TYPE_LARGE)
                paneBuilder.setImage(bigPicture)
            }
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
                ?.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PANE) ?: 4
        } catch (_: Exception) {
            4
        }
    }
}
