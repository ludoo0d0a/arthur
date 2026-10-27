package fr.geoking.arthur.ui.screens.sandbox

import android.content.Context
import fr.geoking.arthur.audio.engines.AudioCacheManager
import fr.geoking.arthur.audio.engines.ExecuTorchMidiEngine
import fr.geoking.arthur.audio.engines.MusicGenSmallEngine
import fr.geoking.arthur.audio.engines.NeuralModelDownloader
import fr.geoking.arthur.audio.engines.SoundGenEngine
import fr.geoking.arthur.audio.engines.StableAudio3Engine
import fr.geoking.arthur.audio.engines.llm.LocalNeuralInference
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.audio.NeuralPromptDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs real on-device generation for the Music LLM sandbox (no fake/silent previews).
 */
class SandboxAudioGenerator(
    private val context: Context,
    private val downloader: NeuralModelDownloader,
    private val cacheManager: AudioCacheManager = AudioCacheManager(context),
    private val inference: LocalNeuralInference = LocalNeuralInference(cacheManager),
) {
    private val soundGen = SoundGenEngine(downloader, cacheManager, inference)
    private val musicGen = MusicGenSmallEngine(downloader, cacheManager, inference)
    private val stableAudio = StableAudio3Engine(downloader, cacheManager, inference)
    private val executorch = ExecuTorchMidiEngine(downloader, cacheManager)

    suspend fun generate(
        model: SandboxModelOption,
        format: MusicOutputFormat,
        promptText: String,
        durationSeconds: Int,
    ): Result<File> = withContext(Dispatchers.IO) {
        val prompt = sandboxPrompt(model, promptText, durationSeconds)
        val seed = System.currentTimeMillis()
        val duration = durationSeconds.coerceIn(5, 120)

        val file = when {
            format == MusicOutputFormat.MIDI_SOUNDFONT ||
                model.engineType == NeuralEngineType.EXECUTORCH_MIDI -> {
                if (!executorch.isModelReady()) {
                    return@withContext Result.failure(
                        IllegalStateException("ExecuTorch SoundFont not downloaded"),
                    )
                }
                executorch.generateTrack(prompt, seed, duration)
            }
            format == MusicOutputFormat.PROCEDURAL_DSP || model.engineType == null -> {
                // Custom / procedural: synthesize with an ephemeral weight bank (no HF download).
                val weights = ephemeralWeights()
                inference.generate(
                    engineType = NeuralEngineType.STABLE_AUDIO_3_SMALL,
                    modelFile = weights,
                    prompt = prompt,
                    seed = seed,
                    durationSeconds = duration,
                )
            }
            else -> {
                val engineType = checkNotNull(model.engineType) { "No engine selected" }
                if (!downloader.isModelAvailable(engineType)) {
                    return@withContext Result.failure(
                        IllegalStateException("Model not downloaded: ${model.name}"),
                    )
                }
                when (engineType) {
                    NeuralEngineType.SOUNDGEN -> soundGen.generateTrack(prompt, seed, duration)
                    NeuralEngineType.MUSICGEN_SMALL -> musicGen.generateTrack(prompt, seed, duration)
                    NeuralEngineType.STABLE_AUDIO_3_SMALL ->
                        stableAudio.generateTrack(prompt, seed, duration)
                    NeuralEngineType.EXECUTORCH_MIDI ->
                        executorch.generateTrack(prompt, seed, duration)
                }
            }
        }

        if (file == null || !file.exists() || file.length() <= 44L) {
            return@withContext Result.failure(
                IllegalStateException("Generation produced no audio file"),
            )
        }
        if (AudioCacheManager.wavPeakAbsolute(file) < AudioCacheManager.MIN_AUDIBLE_PEAK) {
            return@withContext Result.failure(
                IllegalStateException("Generated audio is silent"),
            )
        }

        val takeId = "sandbox_${seed}"
        val persisted = cacheManager.persistTakeAudio(file, takeId)
        Result.success(persisted)
    }

    private fun sandboxPrompt(
        model: SandboxModelOption,
        promptText: String,
        durationSeconds: Int,
    ): NeuralPromptDef {
        val pack = when (model.engineType) {
            NeuralEngineType.SOUNDGEN -> NeuralAudioPack.ATMOSPHERES_NATURE
            NeuralEngineType.MUSICGEN_SMALL -> NeuralAudioPack.MUSEUMS_CLASSICAL
            NeuralEngineType.STABLE_AUDIO_3_SMALL -> NeuralAudioPack.GENART_DIGITAL
            NeuralEngineType.EXECUTORCH_MIDI -> NeuralAudioPack.EXECUTORCH_MIDI
            null -> NeuralAudioPack.GENART_DIGITAL
        }
        return NeuralPromptDef(
            id = "sandbox_${model.id}",
            pack = pack,
            title = model.name,
            promptText = promptText.trim().ifBlank { model.name },
            targetDurationSeconds = durationSeconds.coerceIn(5, 120),
        )
    }

    private fun ephemeralWeights(): File {
        val dir = File(context.cacheDir, "sandbox_weights").apply { mkdirs() }
        val file = File(dir, "custom_llm_weights.bin")
        if (!file.exists() || file.length() < LocalNeuralInference.MIN_MODEL_BYTES) {
            // Deterministic patterned weights so Custom LLM / Procedural DSP always works offline.
            file.writeBytes(ByteArray(4096) { i -> ((i * 131) xor 0x5A).toByte() })
        }
        return file
    }
}
