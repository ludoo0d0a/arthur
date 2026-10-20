package fr.geoking.arthur.audio.engines

import android.content.Context
import fr.geoking.arthur.shared.audio.ExecuTorchMidiPresets
import fr.geoking.arthur.shared.audio.NeuralEngineType
import fr.geoking.arthur.shared.error.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Downloading(val engineType: NeuralEngineType, val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    data class Ready(val engineType: NeuralEngineType, val modelFile: File) : ModelDownloadState()
    data class Error(val engineType: NeuralEngineType, val message: String) : ModelDownloadState()
}

/** Downloads and manages model weights for local neural music generation. */
class NeuralModelDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .followRedirects(true)
        .build(),
    private val errorLogger: ErrorLogger? = null,
) {
    private val modelsDir = File(context.filesDir, "neural_models").apply { mkdirs() }
    private val soundFontsDir = File(context.filesDir, "soundfonts").apply { mkdirs() }

    private val _downloadStates = MutableStateFlow(
        NeuralEngineType.entries.associateWith { engine -> initialState(engine) },
    )
    val downloadStates: StateFlow<Map<NeuralEngineType, ModelDownloadState>> = _downloadStates.asStateFlow()

    private fun initialState(engineType: NeuralEngineType): ModelDownloadState {
        val file = artifactFile(engineType)
        return if (file.exists() && file.length() > 0) {
            ModelDownloadState.Ready(engineType, file)
        } else {
            ModelDownloadState.Idle
        }
    }

    /**
     * On-disk artifact for [engineType].
     * ExecuTorch MIDI uses a public SoundFont (no private .pte host); other engines use weight files.
     */
    fun getModelFile(engineType: NeuralEngineType): File = when (engineType) {
        NeuralEngineType.SOUNDGEN -> File(modelsDir, "soundgen_small.bin")
        NeuralEngineType.MUSICGEN_SMALL -> File(modelsDir, "musicgen_small.bin")
        NeuralEngineType.STABLE_AUDIO_3_SMALL -> File(modelsDir, "stable_audio_3_small.bin")
        // Optional future .pte path; readiness is driven by [artifactFile] (SoundFont).
        NeuralEngineType.EXECUTORCH_MIDI -> File(modelsDir, "executorch_midi_gen.pte")
    }

    fun getSoundFontFile(presetFileName: String): File = File(soundFontsDir, presetFileName)

    fun isSoundFontAvailable(presetFileName: String): Boolean {
        val sf = getSoundFontFile(presetFileName)
        return sf.exists() && sf.length() > 0
    }

    /** File that must exist for [isModelAvailable] / Ready UI state. */
    fun artifactFile(engineType: NeuralEngineType): File = when (engineType) {
        NeuralEngineType.EXECUTORCH_MIDI ->
            getSoundFontFile(ExecuTorchMidiPresets.DEFAULT_SOUNDFONT.soundFontFileName)
        else -> getModelFile(engineType)
    }

    fun getDefaultDownloadUrl(engineType: NeuralEngineType): String = when (engineType) {
        NeuralEngineType.SOUNDGEN ->
            "https://huggingface.co/facebook/audiogen-medium/resolve/main/state_dict.bin?download=true"
        NeuralEngineType.MUSICGEN_SMALL ->
            "https://huggingface.co/facebook/musicgen-small/resolve/main/pytorch_model.bin?download=true"
        NeuralEngineType.STABLE_AUDIO_3_SMALL ->
            "https://huggingface.co/Comfy-Org/stable-audio-open-1.0_repackaged/resolve/main/stable-audio-open-1.0.safetensors?download=true"
        // Public FluidR3 GM — previous geoking/arthur-executorch-midi .pte URL returned HTTP 401.
        NeuralEngineType.EXECUTORCH_MIDI -> ExecuTorchMidiPresets.DEFAULT_SOUNDFONT.downloadUrl
    }

    fun isModelAvailable(engineType: NeuralEngineType): Boolean {
        val file = artifactFile(engineType)
        return file.exists() && file.length() > 0
    }

    fun getModelSizeBytes(engineType: NeuralEngineType): Long {
        val file = artifactFile(engineType)
        return if (file.exists()) file.length() else 0L
    }

    fun deleteModel(engineType: NeuralEngineType): Boolean {
        val artifact = artifactFile(engineType)
        val deletedArtifact = if (artifact.exists()) artifact.delete() else true
        val deletedModel = if (engineType == NeuralEngineType.EXECUTORCH_MIDI) {
            val pte = getModelFile(engineType)
            if (pte.exists()) pte.delete() else true
        } else {
            true
        }
        updateState(engineType, ModelDownloadState.Idle)
        return deletedArtifact && deletedModel
    }

    suspend fun downloadModel(
        engineType: NeuralEngineType,
        downloadUrl: String = getDefaultDownloadUrl(engineType),
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = artifactFile(engineType)
        if (isModelAvailable(engineType)) {
            updateState(engineType, ModelDownloadState.Ready(engineType, targetFile))
            return@withContext true
        }

        updateState(engineType, ModelDownloadState.Downloading(engineType, 0, 0, 0))

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "ArthurAndroid/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed with HTTP ${response.code}"
                    errorLogger?.log(
                        sourceId = "neural_model_downloader",
                        category = fr.geoking.arthur.shared.error.ErrorCategory.Network,
                        message = msg,
                        url = downloadUrl,
                        statusCode = response.code,
                    )
                    updateState(engineType, ModelDownloadState.Error(engineType, msg))
                    return@withContext false
                }

                val body = response.body
                val totalBytes = body.contentLength()
                var bytesDownloaded = 0L
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var lastReportTime = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 200 || bytesDownloaded == totalBytes) {
                                lastReportTime = now
                                val progress = if (totalBytes > 0) {
                                    ((bytesDownloaded * 100) / totalBytes).toInt()
                                } else {
                                    0
                                }
                                updateState(
                                    engineType,
                                    ModelDownloadState.Downloading(
                                        engineType,
                                        progress,
                                        bytesDownloaded,
                                        totalBytes,
                                    ),
                                )
                            }
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) targetFile.delete()
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }
                    updateState(engineType, ModelDownloadState.Ready(engineType, targetFile))
                    true
                } else {
                    updateState(engineType, ModelDownloadState.Error(engineType, "Failed to save model file"))
                    false
                }
            }
        } catch (e: Exception) {
            errorLogger?.log(
                sourceId = "neural_model_downloader",
                category = fr.geoking.arthur.shared.error.ErrorCategory.Network,
                message = e.message ?: "Unknown download error",
                url = downloadUrl,
                throwable = e,
            )
            updateState(
                engineType,
                ModelDownloadState.Error(engineType, e.message ?: "Unknown download error"),
            )
            false
        }
    }

    private fun updateState(engineType: NeuralEngineType, state: ModelDownloadState) {
        val currentMap = _downloadStates.value.toMutableMap()
        currentMap[engineType] = state
        _downloadStates.value = currentMap
    }
}
