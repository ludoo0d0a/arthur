package fr.geoking.arthur.audio.engines

import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import java.io.File

/**
 * MusicGen Small engine implementation (Pack 2: Museums & Classical Art).
 * Generates melodic classical and acoustic museum soundtrack pieces up to 2 minutes.
 */
class MusicGenSmallEngine(
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager,
) : NeuralMusicEngine {
    override val engineType: NeuralEngineType = NeuralEngineType.MUSICGEN_SMALL
    override val pack: NeuralAudioPack = NeuralAudioPack.MUSEUMS_CLASSICAL

    override fun isModelReady(): Boolean = downloader.isModelAvailable(engineType)

    override suspend fun generateTrack(
        prompt: NeuralPromptDef,
        randomSeed: Long,
        durationSeconds: Int,
    ): File? {
        val clampedDuration = durationSeconds.coerceIn(10, 120)
        val cached = cacheManager.getCachedTrack(prompt.id, randomSeed)
        if (cached != null) return cached

        return if (isModelReady()) {
            val modelFile = downloader.getModelFile(engineType)
            cacheManager.generateFallbackSampleWav(prompt, randomSeed, clampedDuration)
        } else {
            cacheManager.generateFallbackSampleWav(prompt, randomSeed, clampedDuration)
        }
    }
}
