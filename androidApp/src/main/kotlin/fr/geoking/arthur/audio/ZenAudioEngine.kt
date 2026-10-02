package fr.geoking.arthur.audio

import android.content.Context
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.error.ErrorLogger
import fr.geoking.arthur.source.AmbientAudioSettings
import org.koin.core.context.GlobalContext

/**
 * Thin compatibility wrapper around [ProceduralMusicEngine].
 * Existing call sites may keep using this type.
 */
class ZenAudioEngine(
    context: Context,
    audioSettings: AmbientAudioSettings,
    errorLogger: ErrorLogger? = runCatching { GlobalContext.get().get<ErrorLogger>() }.getOrNull(),
) {
    private val engine = ProceduralMusicEngine(context, audioSettings, errorLogger)

    var onAudioFocusChanged: ((AudioFocusEvent) -> Unit)?
        get() = engine.onAudioFocusChanged
        set(value) {
            engine.onAudioFocusChanged = value
        }

    fun start() = engine.start()
    fun stop(abandonFocus: Boolean = true) = engine.stop(abandonFocus)
    fun destroy() = engine.destroy()
    fun triggerChime() = engine.triggerChime()
    fun triggerTransition() = engine.triggerTransition()
    fun setArtwork(artwork: Artwork) = engine.setArtwork(artwork)
}
