package fr.geoking.arthur.audio.engines

import fr.geoking.arthur.audio.engines.llm.LocalNeuralInference
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import java.io.File

/**
 * Stable Audio 3 Small engine (Pack 3: Genart & Digital Art).
 * Prompt-conditioned local neural decode → electronic / synth PCM.
 */
class StableAudio3Engine(
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager,
    private val inference: LocalNeuralInference = LocalNeuralInference(cacheManager),
) : NeuralMusicEngine {
    override val engineType: NeuralEngineType = NeuralEngineType.STABLE_AUDIO_3_SMALL
    override val pack: NeuralAudioPack = NeuralAudioPack.GENART_DIGITAL

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
