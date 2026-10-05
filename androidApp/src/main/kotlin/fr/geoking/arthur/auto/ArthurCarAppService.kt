package fr.geoking.arthur.auto

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.car.app.CarAppService
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.car.app.CarContext
import androidx.car.app.CarToast
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
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import fr.geoking.arthur.R
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.source.QuoteProvider
import fr.geoking.arthur.source.QuoteSettings
import fr.geoking.arthur.source.RotationSettings
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.subPackTiles
import fr.geoking.arthur.shared.marketplace.PackOwnership
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Hard caps so host content limits cannot densify the Spotify-style dashboard. */
internal const val MAX_HOME_GRID_ITEMS = 6
internal const val MAX_SUB_GRID_ITEMS = 30

private const val COVER_ICON_SIZE_PX = 512

internal fun gridContentLimit(carContext: CarContext, maxItems: Int): Int {
    val hostLimit = try {
        carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID)
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

internal fun carErrorTemplate(carContext: CarContext, e: Throwable): Template {
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
        .setHeader(
            Header.Builder()
                .setTitle(carContext.getString(R.string.app_name))
                .setStartHeaderAction(Action.APP_ICON)
                .build(),
        )
        .setIcon(logo)
        .setDebugMessage(e)
        .build()
}

/**
 * Car App Library service for Android Auto displaying large artwork images via PaneTemplate
 * (silent ambient) or MediaPlaybackTemplate (sound on / host media player).
 *
 * Host constraints applied:
 * - Pane actions ≤ 2 (primary play/pause icon-only here)
 * - Header end actions ≤ 2, icon-only for sound toggle + next
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
        if (androidx.car.app.media.MediaConstants.ACTION_SHOW_MEDIA_PLAYBACK == intent.action) {
            return createAmbientScreen(carContext)
        }
        val uri = intent.data
        val artworkId = intent.getStringExtra("artwork_id")
            ?: uri?.getQueryParameter("artwork_id")
            ?: uri?.getQueryParameter("id")
        if (artworkId != null) {
            val familyStr = intent.getStringExtra("pack_family")
                ?: uri?.getQueryParameter("family")
            val family = familyStr?.let { runCatching { PackFamily.valueOf(it) }.getOrNull() }
                ?: PackFamily.Museum
            return createAmbientScreen(
                carContext,
                packSelection = PackSelection(family),
                initialArtworkId = artworkId,
            )
        }
        return PackSelectionScreen(carContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (androidx.car.app.media.MediaConstants.ACTION_SHOW_MEDIA_PLAYBACK != intent.action) return
        val screenManager = carContext.getCarService(androidx.car.app.ScreenManager::class.java)
        if (screenManager.top is MediaAmbientPlaybackScreen) return
        if (screenManager.top is SoundPlayerCarScreen) return
        runCatching {
            org.koin.core.context.GlobalContext.get()
                .get<fr.geoking.arthur.source.AmbientAudioSettings>()
                .setEnabled(true)
        }
        screenManager.push(createAmbientScreen(carContext))
    }
}

/**
 * Screen displaying the available pack families on the Android Auto dashboard.
 */
@OptIn(ExperimentalCarApi::class)
class PackSelectionScreen(carContext: CarContext) : Screen(carContext) {
    init {
        runCatching {
            AppUpdateManagerFactory.create(carContext).appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    CarToast.makeText(
                        carContext,
                        carContext.getString(R.string.update_available_car_subtitle),
                        CarToast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

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
                .setImage(coverCarIcon(carContext, family.coverRes))
                .setOnClickListener {
                    screenManager.push(SubPackSelectionScreen(carContext, family))
                }
                .build()
            sectionBuilder.addItem(item)
        }

        val mediaPlayerAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_play_circle),
                ).build(),
            )
            .setOnClickListener {
                screenManager.push(createAmbientScreen(carContext))
            }
            .build()

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
            .addEndHeaderAction(mediaPlayerAction)
            .addEndHeaderAction(settingsAction)
            .build()

        return SectionedItemTemplate.Builder()
            .setHeader(header)
            .addSection(sectionBuilder.build())
            .build()
    }
}

/** Application settings on Android Auto (updates, quotes, ambient sound, rotation). */
class CarSettingsScreen(carContext: CarContext) : Screen(carContext), KoinComponent {
    private val rotationSettings: RotationSettings by inject()
    private val quoteSettings: QuoteSettings by inject()
    private val ambientAudioSettings: AmbientAudioSettings by inject()
    private var updateStatusText: String? = null

    private fun checkUpdateStatus(showToast: Boolean) {
        runCatching {
            AppUpdateManagerFactory.create(carContext).appUpdateInfo
                .addOnSuccessListener { info ->
                    val status = if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        carContext.getString(R.string.update_available_car_subtitle)
                    } else {
                        carContext.getString(R.string.update_check_up_to_date)
                    }
                    updateStatusText = status
                    if (showToast) {
                        CarToast.makeText(carContext, status, CarToast.LENGTH_LONG).show()
                    }
                    invalidate()
                }
                .addOnFailureListener {
                    val status = carContext.getString(R.string.update_check_up_to_date)
                    updateStatusText = status
                    if (showToast) {
                        CarToast.makeText(carContext, status, CarToast.LENGTH_SHORT).show()
                    }
                    invalidate()
                }
        }
    }

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

        // Keep "Check for updates" first so it stays visible under host list limits.
        val checkUpdateRowBuilder = Row.Builder()
            .setTitle(carContext.getString(R.string.settings_check_update))
        val status = updateStatusText
        if (status != null) {
            checkUpdateRowBuilder.addText(status)
        }
        checkUpdateRowBuilder.setOnClickListener {
            checkUpdateStatus(showToast = true)
        }
        listBuilder.addItem(checkUpdateRowBuilder.build())

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

        val provider = quoteSettings.provider.value
        val providerLabel = when (provider) {
            QuoteProvider.ZenQuotes ->
                carContext.getString(R.string.settings_quote_provider_zenquotes)
            QuoteProvider.CitationLecog ->
                carContext.getString(R.string.settings_quote_provider_lecog)
        }
        val providerRowBuilder = Row.Builder()
            .setTitle(carContext.getString(R.string.settings_quote_provider))
            .addText(providerLabel)
        providerRowBuilder.setOnClickListener {
            val next = when (provider) {
                QuoteProvider.ZenQuotes -> QuoteProvider.CitationLecog
                QuoteProvider.CitationLecog -> QuoteProvider.ZenQuotes
            }
            quoteSettings.setProvider(next)
            invalidate()
        }
        listBuilder.addItem(providerRowBuilder.build())

        val soundEnabled = ambientAudioSettings.enabled.value
        val soundRowBuilder = Row.Builder()
            .setTitle(carContext.getString(R.string.settings_ambient_sound))
            .addText(carContext.getString(R.string.settings_ambient_sound_subtitle))
        if (soundEnabled) {
            soundRowBuilder.addText("✓")
        }
        soundRowBuilder.setOnClickListener {
            ambientAudioSettings.setEnabled(!soundEnabled)
            invalidate()
        }
        listBuilder.addItem(soundRowBuilder.build())

        val durationRow = Row.Builder()
            .setTitle(carContext.getString(R.string.screen_rotation_interval))
            .addText(rotationIntervalLabel(carContext, currentInterval))
            .setBrowsable(true)
            .setOnClickListener {
                screenManager.push(CarRotationIntervalScreen(carContext))
            }
            .build()
        listBuilder.addItem(durationRow)

        val header = Header.Builder()
            .setTitle(carContext.getString(R.string.screen_settings))
            .setStartHeaderAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setHeader(header)
            .setSingleList(listBuilder.build())
            .build()
    }
}

/** Choose Ambient rotation duration for Android Auto. */
class CarRotationIntervalScreen(carContext: CarContext) : Screen(carContext), KoinComponent {
    private val rotationSettings: RotationSettings by inject()

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
        RotationSettings.OPTIONS_MS.forEach { ms ->
            val rowBuilder = Row.Builder()
                .setTitle(rotationIntervalLabel(carContext, ms))
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

internal fun rotationIntervalLabel(carContext: CarContext, ms: Long): String =
    if (ms < 60_000L) {
        carContext.getString(R.string.rotation_interval_seconds, (ms / 1_000L).toInt())
    } else {
        carContext.getString(R.string.rotation_interval_minutes, (ms / 60_000L).toInt())
    }

/** Sub-packs / topics for a given [PackFamily]. */
@OptIn(ExperimentalCarApi::class)
class SubPackSelectionScreen(
    carContext: CarContext,
    val family: PackFamily,
) : Screen(carContext), KoinComponent {
    private val packOwnership: PackOwnership
        get() = runCatching { getKoin().get<PackOwnership>() }.getOrDefault(PackOwnership.NONE)

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
            .filter { tile ->
                when (family) {
                    // Sound: free + owned only (no Marketplace unlock on Auto).
                    PackFamily.Sound -> !tile.isLocked(packOwnership)
                    // Other families: no sellable/commerce tiles on Auto.
                    else -> tile.sellablePackId == null
                }
            }
            .take(gridLimit)

        val sectionBuilder = GridSection.Builder()
            .setItemSize(GridSection.ITEM_SIZE_EXTRA_LARGE)
            .setItemImageShape(GridSection.ITEM_IMAGE_SHAPE_UNSET)

        tiles.forEach { tile ->
            val item = GridItem.Builder()
                .setTitle(carContext.getString(tile.titleRes))
                .setImage(coverCarIcon(carContext, tile.coverRes))
                .setOnClickListener {
                    if (tile.selection.family == PackFamily.Sound) {
                        screenManager.push(SoundPlayerCarScreen(carContext, tile.selection))
                    } else {
                        screenManager.push(createAmbientScreen(carContext, tile.selection))
                    }
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
