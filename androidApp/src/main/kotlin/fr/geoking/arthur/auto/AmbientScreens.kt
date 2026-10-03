package fr.geoking.arthur.auto

import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
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
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PANE)
        } catch (_: Exception) {
            4
        }
    }
}

/**
 * Spotify-like host media player ambient: [MediaPlaybackTemplate] driven by a local
 * Media3 [MediaSession] (album art / transport / ambient backdrop from the host).
 */
@OptIn(UnstableApi::class)
class MediaAmbientPlaybackScreen(
    carContext: CarContext,
    val packSelection: PackSelection = PackSelection(PackFamily.Museum),
    val initialArtworkId: String? = null,
) : Screen(carContext), KoinComponent {
    private val ambientAudioSettings: AmbientAudioSettings by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = AmbientMediaPlayer(
        Looper.getMainLooper(),
        object : AmbientMediaPlayer.Callbacks {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean) {
                rotation.setPlaying(playWhenReady)
            }

            override fun onSkip(delta: Int) {
                rotation.advance(delta, isAuto = false)
            }

            override fun onPlayMediaId(mediaId: String) {
                // Car playback screen has no browse tree; ignore media-id seeks.
            }
        },
    )
    private val mediaSession: MediaSession =
        MediaSession.Builder(carContext, player)
            .setId("ArthurCarAmbient-${System.identityHashCode(this)}")
            .build()

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
        runCatching {
            @Suppress("DEPRECATION")
            val token = MediaSessionCompat.Token.fromToken(mediaSession.platformToken)
            carContext.getCarService(MediaPlaybackManager::class.java)
                .registerMediaPlaybackToken(token)
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    mediaSession.release()
                    player.release()
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
            val uri = AmbientAlbumArt.contentUri(carContext.packageName, art.id, gen)
            val quote = if (rotation.showQuotes()) rotation.currentQuote else null
            val lines = ambientMediaDisplayLines(art, quote)
            player.publish(
                art = art,
                artworkUri = uri,
                title = lines.title,
                subtitle = lines.artist,
                genre = if (art.isGenerative) "generative" else art.kind.name,
                playing = playing,
                durationMs = rotation.slideDurationMs(),
            )
        }
    }
}
