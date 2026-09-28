package fr.geoking.arthur.auto

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.media.MediaPlaybackManager
import androidx.car.app.media.model.MediaPlaybackTemplate
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.constraints.ConstraintManager
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.isGenerative
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext

/** Pane ambient (silent) vs MediaPlayback ambient (sound / Spotify-like host chrome). */
internal fun createAmbientScreen(
    carContext: CarContext,
    packSelection: PackSelection = PackSelection(PackFamily.Museum),
    initialArtworkId: String? = null,
): Screen {
    val soundOn = runCatching {
        GlobalContext.get().get<AmbientAudioSettings>().enabled.value
    }.getOrDefault(false)
    return if (soundOn) {
        MediaAmbientPlaybackScreen(carContext, packSelection, initialArtworkId)
    } else {
        ArtworkPaneScreen(carContext, packSelection, initialArtworkId)
    }
}

internal fun Screen.switchAmbientSoundMode(
    enableSound: Boolean,
    packSelection: PackSelection,
    artworkId: String?,
) {
    runCatching {
        GlobalContext.get().get<AmbientAudioSettings>().setEnabled(enableSound)
    }
    screenManager.push(
        if (enableSound) {
            MediaAmbientPlaybackScreen(carContext, packSelection, artworkId)
        } else {
            ArtworkPaneScreen(carContext, packSelection, artworkId)
        },
    )
    finish()
}

internal fun ambientSoundToggleAction(
    carContext: CarContext,
    soundOn: Boolean,
    onToggle: () -> Unit,
): Action {
    val iconRes = if (soundOn) R.drawable.ic_car_sound_on else R.drawable.ic_car_sound_off
    return Action.Builder()
        .setIcon(
            CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build(),
        )
        .setOnClickListener { onToggle() }
        .build()
}

internal fun ambientNextAction(carContext: CarContext, onNext: () -> Unit): Action {
    return Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(carContext, R.drawable.ic_car_next),
            ).build(),
        )
        .setOnClickListener { onNext() }
        .build()
}

class ArtworkPaneScreen(
    carContext: CarContext,
    val packSelection: PackSelection = PackSelection(PackFamily.Museum),
    val initialArtworkId: String? = null,
) : Screen(carContext), KoinComponent {
    private val ambientAudioSettings: AmbientAudioSettings by inject()
    private val rotation = AmbientRotationController(
        appContext = carContext,
        lifecycle = lifecycle,
        packSelection = packSelection,
        initialArtworkId = initialArtworkId,
        onInvalidate = { invalidate() },
    )

    fun advance(delta: Int, isAuto: Boolean = false) = rotation.advance(delta, isAuto)

    fun togglePlay() = rotation.togglePlay()

    fun isPlaying(): Boolean = rotation.isPlaying

    fun currentArtwork(): fr.geoking.arthur.shared.domain.Artwork? = rotation.current

    fun debugSnapshot(): AmbientRotationDebug = rotation.debugSnapshot()

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

        if (!rotation.loaded) {
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

        val art = rotation.current
        val paneBuilder = Pane.Builder()
        val rowLimit = paneRowLimit()
        val isDevMode = rotation.isDevMode()
        var rowsUsed = 0

        if (art != null && rowLimit > 0) {
            val hasFreshRender =
                art.id == rotation.renderedArtId &&
                    rotation.generation == rotation.renderedGen &&
                    rotation.renderedBitmap != null
            if (!hasFreshRender) {
                // Controller schedules its own renders; invalidate will refresh.
            }
            val rowBuilder = Row.Builder()
                .setTitle(art.title.ifBlank { carContext.getString(R.string.app_name) })

            val quote = if (rotation.showQuotes()) rotation.currentQuote else null

            val index = rotation.catalog.indexOfFirst { it.id == art.id }.let { if (it < 0) 0 else it }
            val slidePos = if (isDevMode && rotation.catalog.isNotEmpty()) {
                "[${index + 1}/${rotation.catalog.size}] cache:${rotation.cacheCount} live:${rotation.liveCount}"
            } else {
                null
            }

            val metaParts = buildList {
                if (art.attribution.isNotBlank()) add(art.attribution)
                if (slidePos != null) add(slidePos)
            }
            if (metaParts.isNotEmpty()) {
                rowBuilder.addText(metaParts.joinToString(" "))
            }
            val bigPicture = runCatching {
                val bitmap = if (hasFreshRender) {
                    rotation.renderedBitmap!!
                } else {
                    AmbientStillRenderer.renderPlaceholder(art, rotation.generation)
                }
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
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.app_name))
                    .addText(carContext.getString(R.string.car_gallery_empty))
                    .build(),
            )
        }

        paneBuilder.addAction(
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            if (rotation.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_circle,
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

        val soundOn = ambientAudioSettings.enabled.value
        val header = Header.Builder()
            .setTitle(title)
            .setStartHeaderAction(Action.BACK)
            .addEndHeaderAction(
                ambientSoundToggleAction(carContext, soundOn) {
                    switchAmbientSoundMode(
                        enableSound = !soundOn,
                        packSelection = packSelection,
                        artworkId = rotation.current?.id,
                    )
                },
            )
            .addEndHeaderAction(
                ambientNextAction(carContext) { advance(+1, isAuto = false) },
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

/**
 * Spotify-like host media player ambient: [MediaPlaybackTemplate] driven by a local
 * [MediaSessionCompat] (album art / transport / ambient backdrop from the host).
 */
class MediaAmbientPlaybackScreen(
    carContext: CarContext,
    val packSelection: PackSelection = PackSelection(PackFamily.Museum),
    val initialArtworkId: String? = null,
) : Screen(carContext), KoinComponent {
    private val ambientAudioSettings: AmbientAudioSettings by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session = MediaSessionCompat(carContext, "ArthurCarAmbient")

    private val rotation = AmbientRotationController(
        appContext = carContext,
        lifecycle = lifecycle,
        packSelection = packSelection,
        initialArtworkId = initialArtworkId,
        limitAutoRotation = false,
        onInvalidate = { invalidate() },
        onArtworkChanged = { art, gen, playing -> publishSession(art, gen, playing) },
    )

    init {
        // Media playback ambient always implies ambient sound on.
        if (!ambientAudioSettings.enabled.value) {
            ambientAudioSettings.setEnabled(true)
        }
        session.setCallback(
            object : MediaSessionCompat.Callback() {
                override fun onPlay() = rotation.setPlaying(true)
                override fun onPause() = rotation.setPlaying(false)
                override fun onSkipToNext() = rotation.advance(+1, isAuto = false)
                override fun onSkipToPrevious() = rotation.advance(-1, isAuto = false)
            },
        )
        session.isActive = true
        runCatching {
            carContext.getCarService(MediaPlaybackManager::class.java)
                .registerMediaPlaybackToken(session.sessionToken)
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    session.isActive = false
                    session.release()
                    scope.cancel()
                }
            },
        )
    }

    fun advance(delta: Int, isAuto: Boolean = false) = rotation.advance(delta, isAuto)

    fun currentArtwork(): fr.geoking.arthur.shared.domain.Artwork? = rotation.current

    fun isPlaying(): Boolean = rotation.isPlaying

    override fun onGetTemplate(): Template {
        return try {
            buildPlaybackTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildPlaybackTemplate(): Template {
        val title = carContext.getString(packSelection.family.titleRes)
        val soundOn = ambientAudioSettings.enabled.value
        val header = Header.Builder()
            .setTitle(title)
            .setStartHeaderAction(Action.BACK)
            .addEndHeaderAction(
                ambientSoundToggleAction(carContext, soundOn) {
                    switchAmbientSoundMode(
                        enableSound = !soundOn,
                        packSelection = packSelection,
                        artworkId = rotation.current?.id,
                    )
                },
            )
            .build()
        return MediaPlaybackTemplate.Builder()
            .setHeader(header)
            .build()
    }

    private fun publishSession(art: fr.geoking.arthur.shared.domain.Artwork, gen: Long, playing: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val file = AmbientAlbumArt.cacheFile(carContext, art.id, gen)
                if (!file.exists()) {
                    runCatching {
                        AmbientStillRenderer.renderToFile(
                            art,
                            gen,
                            file,
                            rotation.imageCache(),
                            rotation.invalidStore(),
                        )
                    }
                }
            }
            val uri = AmbientAlbumArt.contentUri(carContext.packageName, art.id, gen).toString()
            val subtitle = art.attribution
            runCatching {
                session.setMetadata(
                    MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, art.id)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, art.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, art.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_GENRE,
                            if (art.isGenerative) "generative" else art.kind.name,
                        )
                        .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, uri)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, uri)
                        .build(),
                )
                session.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setActions(
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                        )
                        .setState(
                            if (playing) {
                                PlaybackStateCompat.STATE_PLAYING
                            } else {
                                PlaybackStateCompat.STATE_PAUSED
                            },
                            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                            1f,
                        )
                        .build(),
                )
            }
        }
    }
}
