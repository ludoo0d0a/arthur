package fr.geoking.arthur.auto

import android.net.Uri
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.media.MediaPlaybackManager
import androidx.car.app.media.model.MediaPlaybackTemplate
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import fr.geoking.arthur.R
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.audio.ZenAudioEngine
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.source.AmbientAudioCharacter
import fr.geoking.arthur.source.AmbientAudioSettings
import fr.geoking.arthur.ui.components.AudioPackTopic
import fr.geoking.arthur.ui.components.PackCovers
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.audioPackTopicOrNull
import fr.geoking.arthur.ui.components.labelRes
import fr.geoking.arthur.ui.components.primaryStyle
import fr.geoking.arthur.ui.components.stylesInPack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android Auto listen surface for a Sound pack — [MediaPlaybackTemplate] + [ZenAudioEngine].
 * Skip next/prev cycles styles inside the pack. Prefs restore on dismiss (preview, not Ambient).
 */
@OptIn(UnstableApi::class)
class SoundPlayerCarScreen(
    carContext: CarContext,
    val selection: PackSelection,
) : Screen(carContext), KoinComponent {
    private val ambientAudioSettings: AmbientAudioSettings by inject()

    private val topic: AudioPackTopic =
        selection.audioPackTopicOrNull() ?: AudioPackTopic.Essentials
    private val styles: List<MusicStyle> = topic.stylesInPack().ifEmpty {
        listOf(topic.primaryStyle())
    }

    private var styleIndex: Int = 0
    private var isPlaying: Boolean = true

    private val previousEnabled = ambientAudioSettings.enabled.value
    private val previousStyle = ambientAudioSettings.stylePreference.value
    private val previousCharacter = ambientAudioSettings.character.value

    private val zenAudio = ZenAudioEngine(carContext, ambientAudioSettings)

    private val player = AmbientMediaPlayer(
        Looper.getMainLooper(),
        object : AmbientMediaPlayer.Callbacks {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean) {
                isPlaying = playWhenReady
                syncEngine()
            }

            override fun onSkip(delta: Int) {
                if (styles.size <= 1) return
                styleIndex = (styleIndex + delta).mod(styles.size)
                syncEngine()
                publishSession()
                invalidate()
            }

            override fun onPlayMediaId(mediaId: String) {
                val idx = styles.indexOfFirst { styleMediaId(it) == mediaId }
                if (idx >= 0 && idx != styleIndex) {
                    styleIndex = idx
                    syncEngine()
                    publishSession()
                    invalidate()
                }
            }
        },
    )

    private val mediaSession: MediaSession =
        MediaSession.Builder(carContext, player)
            .setId("ArthurCarSound-${System.identityHashCode(this)}")
            .build()

    init {
        syncEngine()
        publishSession()
        runCatching {
            @Suppress("DEPRECATION")
            val token = MediaSessionCompat.Token.fromToken(mediaSession.platformToken)
            carContext.getCarService(MediaPlaybackManager::class.java)
                .registerMediaPlaybackToken(token)
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    zenAudio.stop()
                    zenAudio.destroy()
                    mediaSession.release()
                    player.release()
                    ambientAudioSettings.setEnabled(previousEnabled)
                    ambientAudioSettings.setStylePreference(previousStyle)
                    ambientAudioSettings.setCharacter(previousCharacter)
                }
            },
        )
    }

    private fun selectedStyle(): MusicStyle = styles[styleIndex.coerceIn(0, styles.lastIndex)]

    private fun styleMediaId(style: MusicStyle): String {
        val suffix = topic.packSuffix ?: "essentials"
        return "sound.$suffix.${style.name}"
    }

    private fun syntheticArtwork(style: MusicStyle): Artwork {
        val suffix = topic.packSuffix ?: "essentials"
        return Artwork(
            id = styleMediaId(style),
            title = carContext.getString(topic.labelRes),
            sourceId = "sound",
            kind = ArtworkKind.Photo,
            attribution = carContext.getString(style.labelRes()),
        )
    }

    private fun applyAtmosphereIfNeeded() {
        when (topic) {
            AudioPackTopic.HearthWeather, AudioPackTopic.DawnChorus,
            AudioPackTopic.TempleResonance, AudioPackTopic.WindGarden,
            AudioPackTopic.CosmicDrift,
            -> ambientAudioSettings.setCharacter(AmbientAudioCharacter.Atmosphere)
            else -> Unit
        }
    }

    private fun syncEngine() {
        val style = selectedStyle()
        ambientAudioSettings.setEnabled(true)
        ambientAudioSettings.setStylePreference(style)
        applyAtmosphereIfNeeded()
        zenAudio.setArtwork(syntheticArtwork(style))
        if (isPlaying) {
            zenAudio.start()
            zenAudio.triggerTransition()
        } else {
            zenAudio.stop(abandonFocus = false)
        }
    }

    private fun publishSession() {
        val style = selectedStyle()
        val art = syntheticArtwork(style)
        val coverRes = PackCovers.audio(topic)
        val artworkUri = Uri.parse(
            "android.resource://${carContext.packageName}/$coverRes",
        )
        val queue = styles.map { syntheticArtwork(it) }
        player.publish(
            art = art,
            artworkUri = artworkUri,
            title = carContext.getString(topic.labelRes),
            subtitle = carContext.getString(style.labelRes()),
            genre = "sound",
            queue = queue,
            queueUris = queue.associate { it.id to artworkUri },
            playing = isPlaying,
            playlistTitle = carContext.getString(R.string.pack_sound),
        )
    }

    override fun onGetTemplate(): Template {
        return try {
            buildPlaybackTemplate()
        } catch (e: Throwable) {
            carErrorTemplate(carContext, e)
        }
    }

    private fun buildPlaybackTemplate(): Template {
        // Header end action opens Sound sub-packs (playlist / pack switch).
        val header = Header.Builder()
            .setTitle(carContext.getString(topic.labelRes))
            .setStartHeaderAction(Action.BACK)
            .addEndHeaderAction(
                ambientChangePlaylistAction(carContext) {
                    screenManager.push(SubPackSelectionScreen(carContext, PackFamily.Sound))
                },
            )
            .build()
        return MediaPlaybackTemplate.Builder()
            .setHeader(header)
            .build()
    }
}
