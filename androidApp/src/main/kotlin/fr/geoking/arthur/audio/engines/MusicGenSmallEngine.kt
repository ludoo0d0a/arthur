package fr.geoking.arthur.audio.engines

import fr.geoking.arthur.audio.engines.llm.LocalNeuralInference
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import java.io.File

/**
 * MusicGen Small engine (Pack 2: Museums & Classical Art).
 * Prompt-conditioned local neural decode → melodic / classical PCM.
 */
class MusicGenSmallEngine(
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager,
    private val inference: LocalNeuralInference = LocalNeuralInference(cacheManager),
) : NeuralMusicEngine {
    override val engineType: NeuralEngineType = NeuralEngineType.MUSICGEN_SMALL
    override val pack: NeuralAudioPack = NeuralAudioPack.MUSEUMS_CLASSICAL

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
