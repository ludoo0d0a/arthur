package fr.geoking.arthur.audio.engines

import fr.geoking.arthur.audio.engines.llm.LocalNeuralInference
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import java.io.File

/**
 * SoundGen engine (Pack 1: Atmospheres & Nature).
 * Prompt-conditioned local neural decode → nature / atmosphere PCM.
 */
class SoundGenEngine(
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager,
    private val inference: LocalNeuralInference = LocalNeuralInference(cacheManager),
) : NeuralMusicEngine {
    override val engineType: NeuralEngineType = NeuralEngineType.SOUNDGEN
    override val pack: NeuralAudioPack = NeuralAudioPack.ATMOSPHERES_NATURE

    override fun isModelReady(): Boolean = downloader.isModelAvailable(engineType)

    override suspend fun generateTrack(
        prompt: NeuralPromptDef,
        randomSeed: Long,
        durationSeconds: Int,
    ): File? {
        if (!isModelReady()) return null
        return inference.generate(
            engineType = engineType,
            modelFile = downloader.getModelFile(engineType),
            prompt = prompt,
            seed = randomSeed,
            durationSeconds = durationSeconds.coerceIn(10, 120),
        )
    }
}
