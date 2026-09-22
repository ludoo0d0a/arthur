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
import fr.geoking.arthur.source.AmbientStillPicker
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.DeveloperSettings
import fr.geoking.arthur.source.InvalidArtworkStore
import fr.geoking.arthur.source.Quote
import fr.geoking.arthur.source.QuoteRepository
import fr.geoking.arthur.source.QuoteSettings
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.source.StillImagePrefetcher
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.allowsGenerativeAmbientFallback
import fr.geoking.arthur.ui.components.resolvePackPool
import fr.geoking.arthur.ui.components.sourceIdsForAmbientLoad
import fr.geoking.arthur.ui.components.subPackTiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * - Pane actions ≤ 2 (primary play/pause icon-only here)
 * - Header end actions ≤ 2, icon-only for prev/next (Car API 7+ hosts ignore deprecated ActionStrip)
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
        val uri = intent.data
        val artworkId = intent.getStringExtra("artwork_id")
            ?: uri?.getQueryParameter("artwork_id")
            ?: uri?.getQueryParameter("id")
        if (artworkId != null) {
            val familyStr = intent.getStringExtra("pack_family")
                ?: uri?.getQueryParameter("family")
            val family = familyStr?.let { runCatching { PackFamily.valueOf(it) }.getOrNull() }
                ?: PackFamily.Museum
            return ArtworkPaneScreen(
                carContext,
                packSelection = PackSelection(family),
                initialArtworkId = artworkId,
            )
        }
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
            .filter { it != PackFamily.Personal } // Marketplace packs: no Auto commerce / Personal browse
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
    private val quoteSettings: QuoteSettings by inject()

    override fun onGetTemplate(): Template {
        return try {
            buildTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildTemplate(): Template {
        val currentInterval = rotationSettings.autoIntervalMs.value
        val listBuilder = ItemList.Builder()

        val showQuotes = quoteSettings.showQuotes.value
        val quoteRowBuilder = Row.Builder()
            .setTitle(carContext.getString(R.string.settings_show_quotes))
            .addText(carContext.getString(R.string.settings_show_quotes_subtitle))
        if (showQuotes) {
            quoteRowBuilder.addText("✓")
        }
        quoteRowBuilder.setOnClickListener {
            quoteSettings.setShowQuotes(!showQuotes)
            invalidate()
        }
        listBuilder.addItem(quoteRowBuilder.build())

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
                rotationSettings.setAutoIntervalMs(ms)
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
        val tiles = family.subPackTiles()
            .filter { it.sellablePackId == null } // no Marketplace commerce on Auto
            .take(gridLimit)

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
    val initialArtworkId: String? = null,
) : Screen(carContext), KoinComponent {
    private val contentEngine: ContentEngine by inject()
    private val rotationSettings: RotationSettings by inject()
    private val imageCache: ArtworkImageCache by inject()
    private val invalidStore: InvalidArtworkStore by inject()
    private val quoteSettings: QuoteSettings by inject()
    private val quoteRepository: QuoteRepository by inject()
    private val developerSettings: DeveloperSettings by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val renewSourceIds: List<String> = packSelection.sourceIdsForAmbientLoad().orEmpty()

    private var catalog: List<Artwork> = emptyList()
    private var current: Artwork? = null
    private var currentQuote: Quote? = null
    private var generation: Long = 0L
    private var isPlaying: Boolean = true
    private var consecutiveAutoRotations: Int = 0
    private var seenIds: Set<String> = emptySet()
    private var cacheCount: Int = 0
    private var liveCount: Int = 0
    private var queryLaunched: Boolean = false
    private var rotationJob: Job? = null
    private var renderJob: Job? = null
    private var loaded: Boolean = false
    private val advanceMutex = Mutex()

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
            bootstrapRotation()
        }
    }

    private suspend fun bootstrapRotation() {
        val cachedRaw = withContext(Dispatchers.IO) {
            imageCache.loadCachedArtworks(renewSourceIds.takeIf { it.isNotEmpty() })
        }
        val cachedPool = resolvePackPool(cachedRaw, packSelection)
            .distinctBy { it.id }
            .ifEmpty {
                if (packSelection.allowsGenerativeAmbientFallback()) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            contentEngine.catalog(
                                PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()),
                            )
                        }.getOrDefault(emptyList())
                    }.let { resolvePackPool(it, packSelection).distinctBy { art -> art.id } }
                } else {
                    emptyList()
                }
            }
        cacheCount = cachedPool.size
        if (cachedPool.isNotEmpty()) {
            applyPool(cachedPool, preferInitial = true)
            loaded = true
            invalidate()
        }

        queryLaunched = true
        val liveRaw = withContext(Dispatchers.IO) {
            runCatching {
                contentEngine.catalog(
                    PreparedRotation(sourceIds = renewSourceIds, artworkIds = emptyList()),
                )
            }.getOrDefault(emptyList())
        }
        val livePool = resolvePackPool(liveRaw, packSelection).distinctBy { it.id }
        liveCount = livePool.size
        if (livePool.isNotEmpty()) {
            applyPool(livePool, preferInitial = catalog.isEmpty())
        } else if (catalog.isEmpty() && packSelection.allowsGenerativeAmbientFallback()) {
            applyPool(cachedPool, preferInitial = true)
        }

        loaded = true
        prefetchNeighbors()
        if (isPlaying) startRotation()
        invalidate()
    }

    private suspend fun applyPool(pool: List<Artwork>, preferInitial: Boolean) {
        val previousId = current?.id
        catalog = pool
        current = when {
            preferInitial && initialArtworkId != null ->
                pool.firstOrNull { it.id == initialArtworkId } ?: resolveAmbientArtwork(pool, null)
            previousId != null ->
                pool.firstOrNull { it.id == previousId } ?: resolveAmbientArtwork(pool, null)
            else -> resolveAmbientArtwork(pool, null)
        }
        seenIds = current?.id?.let { setOf(it) }.orEmpty()
        updateQuoteForCurrent()
        scheduleAsyncRender()
    }

    private suspend fun renewCatalog() {
        queryLaunched = true
        val liveRaw = withContext(Dispatchers.IO) {
            runCatching {
                contentEngine.catalog(
                    PreparedRotation(sourceIds = renewSourceIds, artworkIds = emptyList()),
                )
            }.getOrDefault(emptyList())
        }
        val livePool = resolvePackPool(liveRaw, packSelection).distinctBy { it.id }
        liveCount = livePool.size
        if (livePool.isNotEmpty()) {
            val keepId = current?.id
            catalog = livePool
            if (keepId == null || livePool.none { it.id == keepId }) {
                current = resolveAmbientArtwork(livePool, null)
            }
        }
    }

    private suspend fun prefetchNeighbors() {
        val pool = catalog
        if (pool.isEmpty()) return
        val index = pool.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val currentArt = pool.getOrNull(index)
        val nextArt = pool.getOrNull(AmbientAlbumArt.advanceIndex(index, pool.size))
        val prevArt = pool.getOrNull(Math.floorMod(index - 1, pool.size))
        withContext(Dispatchers.IO) {
            currentArt?.let { StillImagePrefetcher.ensureCached(imageCache, it, allowNetwork = true) }
            nextArt?.let { StillImagePrefetcher.ensureCached(imageCache, it, allowNetwork = true) }
            prevArt?.let { StillImagePrefetcher.ensureCached(imageCache, it, allowNetwork = true) }
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
                    AmbientStillRenderer.render(art, gen, imageCache, invalidStore)
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
                delay(rotationSettings.autoIntervalMs.value)
                if (isPlaying) {
                    advance(+1, isAuto = true)
                }
            }
        }
    }

    private suspend fun updateQuoteForCurrent() {
        if (quoteSettings.showQuotes.value && current != null) {
            currentQuote = withContext(Dispatchers.IO) {
                runCatching { quoteRepository.nextQuote() }.getOrNull()
            }
        } else {
            currentQuote = null
        }
    }

    fun advance(delta: Int, isAuto: Boolean = false) {
        if (!isAuto) {
            consecutiveAutoRotations = 0
            if (!isPlaying) {
                isPlaying = true
            }
        } else {
            consecutiveAutoRotations += 1
            if (consecutiveAutoRotations >= 3) {
                isPlaying = false
                consecutiveAutoRotations = 0
                rotationJob?.cancel()
            }
        }
        scope.launch {
            advanceMutex.withLock {
                if (isAuto) {
                    applyAutoAdvance()
                } else {
                    applyManualAdvance(delta)
                }
            }
        }
    }

    private suspend fun applyManualAdvance(delta: Int) {
        if (catalog.isEmpty()) return
        val index = catalog.indexOfFirst { it.id == current?.id }.let { if (it < 0) 0 else it }
        val nextIndex = AmbientAlbumArt.nextValidIndex(
            poolSize = catalog.size,
            currentIndex = index,
            delta = delta,
            isInvalidAt = { catalog[it].id.let(invalidStore::isInvalid) },
        )
        showArtwork(catalog[nextIndex])
        if (isPlaying) startRotation()
    }

    private suspend fun applyAutoAdvance() {
        if (catalog.isEmpty()) return
        val eligible = catalog.mapNotNull { art ->
            if (invalidStore.isInvalid(art.id)) null else art.id
        }.toSet()
        if (eligible.isEmpty()) return

        var pickedId = AmbientStillPicker.pickNextRandom(
            poolIds = catalog.map { it.id },
            currentId = current?.id,
            seenIds = seenIds,
            recentIds = rotationSettings.recentStillIds(),
            eligibleIds = eligible,
        )

        val noUnseen = eligible.all { it in seenIds } || pickedId == null
        if (noUnseen) {
            renewCatalog()
            val renewedEligible = catalog.mapNotNull { art ->
                if (invalidStore.isInvalid(art.id)) null else art.id
            }.toSet()
            val stillNoUnseen = renewedEligible.all { it in seenIds }
            if (stillNoUnseen) {
                seenIds = current?.id?.let { setOf(it) }.orEmpty()
            }
            pickedId = AmbientStillPicker.pickNextRandom(
                poolIds = catalog.map { it.id },
                currentId = current?.id,
                seenIds = seenIds,
                recentIds = rotationSettings.recentStillIds(),
                eligibleIds = renewedEligible,
            )
        }

        val next = catalog.firstOrNull { it.id == pickedId } ?: return
        showArtwork(next)
        if (isPlaying) startRotation()
    }

    private suspend fun showArtwork(art: Artwork) {
        current = art
        generation += 1
        seenIds = seenIds + art.id
        if (!art.remoteUrl.isNullOrBlank()) {
            rotationSettings.recordRecentStillId(art.id)
        }
        updateQuoteForCurrent()
        scheduleAsyncRender()
        prefetchNeighbors()
        invalidate()
    }

    fun togglePlay() {
        isPlaying = !isPlaying
        consecutiveAutoRotations = 0
        if (isPlaying) startRotation() else rotationJob?.cancel()
        invalidate()
    }

    fun isPlaying(): Boolean = isPlaying

    fun currentArtwork(): Artwork? = current

    fun debugSnapshot(): AmbientRotationDebug {
        val unseen = catalog.count { art ->
            !invalidStore.isInvalid(art.id) && art.id !in seenIds
        }
        return AmbientRotationDebug(
            querySourceIds = renewSourceIds,
            cacheCount = cacheCount,
            liveCount = liveCount,
            poolSize = catalog.size,
            seenCount = seenIds.size,
            unseenCount = unseen,
            consecutiveAutoRotations = consecutiveAutoRotations,
            isPlaying = isPlaying,
            currentArtworkId = current?.id,
            generation = generation,
            queryLaunched = queryLaunched,
        )
    }

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
        val title = carContext.getString(packSelection.family.titleRes)

        if (!loaded) {
            return PaneTemplate.Builder(
                Pane.Builder().setLoading(true).build(),
            )
                .setHeader(
                    Header.Builder()
                        .setTitle(title)
                        .setStartHeaderAction(Action.BACK)
                        .build(),
                )
                .build()
        }

        val art = current
        val paneBuilder = Pane.Builder()
        val rowLimit = paneRowLimit()
        val isDevMode = developerSettings.verbose.value
        var rowsUsed = 0

        if (art != null && rowLimit > 0) {
            val hasFreshRender = art.id == renderedArtId && generation == renderedGen && renderedBitmap != null
            if (!hasFreshRender) {
                scheduleAsyncRender()
            }
            val rowBuilder = Row.Builder()
                .setTitle(art.title.ifBlank { carContext.getString(R.string.app_name) })

            val quote = if (quoteSettings.showQuotes.value) currentQuote else null

            val metaParts = buildList {
                if (art.attribution.isNotBlank()) add(art.attribution)
            }
            if (metaParts.isNotEmpty()) {
                rowBuilder.addText(metaParts.joinToString(" "))
            }
            val bigPicture = runCatching {
                val bitmap = if (hasFreshRender) renderedBitmap!! else AmbientStillRenderer.renderPlaceholder(art, generation)
                CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
            }.getOrNull()
            if (bigPicture != null) {
                rowBuilder.setImage(bigPicture, Row.IMAGE_TYPE_LARGE)
                paneBuilder.setImage(bigPicture)
            }
            paneBuilder.addRow(rowBuilder.build())
            rowsUsed++

            if (quote != null && rowsUsed < rowLimit) {
                val quoteRow = Row.Builder()
                    .setTitle("\u201C${quote.text}\u201D")
                if (quote.author.isNotBlank()) {
                    quoteRow.addText("\u2014 ${quote.author}")
                }
                paneBuilder.addRow(quoteRow.build())
                rowsUsed++
            }

            if (isDevMode && catalog.isNotEmpty() && rowsUsed < rowLimit) {
                val index = catalog.indexOfFirst { it.id == art.id }.let { if (it < 0) 0 else it }
                val unseen = catalog.count { item ->
                    !invalidStore.isInvalid(item.id) && item.id !in seenIds
                }
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("[${index + 1}/${catalog.size}]")
                        .addText("cache:$cacheCount live:$liveCount seen:${seenIds.size} unseen:$unseen")
                        .build(),
                )
                rowsUsed++
            }
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.app_name))
                    .addText(carContext.getString(R.string.car_gallery_empty))
                    .build(),
            )
        }

        // Pane actions ≤ 2: primary play/pause; optional Debug title action in verbose mode
        // (Pane rows with IMAGE_TYPE_LARGE cannot host click listeners).
        paneBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
                        ),
                    ).build(),
                )
                .setFlags(Action.FLAG_PRIMARY)
                .setOnClickListener { togglePlay() }
                .build(),
        )
        if (isDevMode) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_ambient_debug))
                    .setOnClickListener {
                        screenManager.push(AmbientDebugScreen(carContext, debugSnapshot()))
                    }
                    .build(),
            )
        }

        val header = Header.Builder()
            .setTitle(title)
            .setStartHeaderAction(Action.BACK)
            .addEndHeaderAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_car_previous),
                        ).build(),
                    )
                    .setOnClickListener { advance(-1, isAuto = false) }
                    .build(),
            )
            .addEndHeaderAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_car_next),
                        ).build(),
                    )
                    .setOnClickListener { advance(+1, isAuto = false) }
                    .build(),
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
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

/** Developer-only ListTemplate with Ambient rotation diagnostics. */
class AmbientDebugScreen(
    carContext: CarContext,
    private val snapshot: AmbientRotationDebug,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return try {
            buildTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildTemplate(): Template {
        val listBuilder = ItemList.Builder()
        fun addRow(title: String, detail: String) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(detail)
                    .build(),
            )
        }
        val queryLabel = if (snapshot.querySourceIds.isEmpty()) {
            carContext.getString(R.string.car_ambient_debug_all_sources)
        } else {
            snapshot.querySourceIds.joinToString(", ")
        }
        addRow(
            carContext.getString(R.string.car_ambient_debug_query),
            if (snapshot.queryLaunched) queryLabel else carContext.getString(R.string.car_ambient_debug_query_pending),
        )
        addRow(carContext.getString(R.string.car_ambient_debug_live_count), snapshot.liveCount.toString())
        addRow(carContext.getString(R.string.car_ambient_debug_pool_size), snapshot.poolSize.toString())
        addRow(carContext.getString(R.string.car_ambient_debug_cache_count), snapshot.cacheCount.toString())
        addRow(
            carContext.getString(R.string.car_ambient_debug_seen),
            "${snapshot.seenCount} / unseen ${snapshot.unseenCount}",
        )
        addRow(
            carContext.getString(R.string.car_ambient_debug_auto),
            "${snapshot.consecutiveAutoRotations} · playing=${snapshot.isPlaying}",
        )
        addRow(
            carContext.getString(R.string.car_ambient_debug_current),
            "${snapshot.currentArtworkId.orEmpty()} · gen=${snapshot.generation}",
        )

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.car_ambient_debug))
                    .setStartHeaderAction(Action.BACK)
                    .build(),
            )
            .setSingleList(listBuilder.build())
            .build()
    }
}
