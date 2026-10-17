package fr.geoking.arthur.audio.engines

import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import java.io.File

/**
 * Interface for on-device AI local music generation engines (SoundGen, MusicGen Small, Stable Audio 3 Small).
 */
interface NeuralMusicEngine {
    val engineType: NeuralEngineType
    val pack: NeuralAudioPack

    /** Checks whether local model weights are present and ready for generation. */
    fun isModelReady(): Boolean

    /**
     * Renders or generates a 2-minute max instrumental track (no lyrics) into a cached audio file.
     * @param prompt Predefined prompt definition
     * @param randomSeed Seed for audio variation
     * @param durationSeconds Max duration in seconds (capped at 120s)
     * @return Generated/cached WAV or AAC file, or null if generation unavailable
     */
    suspend fun generateTrack(
        prompt: NeuralPromptDef,
        randomSeed: Long = System.currentTimeMillis(),
        durationSeconds: Int = 120,
    ): File?
}
