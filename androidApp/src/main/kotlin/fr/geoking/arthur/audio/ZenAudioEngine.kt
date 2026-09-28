package fr.geoking.arthur.audio

import android.content.Context
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.source.AmbientAudioSettings

/**
 * Thin compatibility wrapper around [ProceduralMusicEngine].
 * Existing call sites may keep using this type.
 */
class ZenAudioEngine(
    context: Context,
    audioSettings: AmbientAudioSettings,
) {
    private val engine = ProceduralMusicEngine(context, audioSettings)

    fun start() = engine.start()
    fun stop() = engine.stop()
    fun destroy() = engine.destroy()
    fun triggerChime() = engine.triggerChime()
    fun triggerTransition() = engine.triggerTransition()
    fun setArtwork(artwork: Artwork) = engine.setArtwork(artwork)
}
