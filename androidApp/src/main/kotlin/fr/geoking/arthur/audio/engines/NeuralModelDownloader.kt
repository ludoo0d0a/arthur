package fr.geoking.arthur.audio.engines

import android.content.Context
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

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Downloading(val engineType: NeuralEngineType, val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    data class Ready(val engineType: NeuralEngineType, val modelFile: File) : ModelDownloadState()
    data class Error(val engineType: NeuralEngineType, val message: String) : ModelDownloadState()
}

/** Downloads and manages model weights for local neural music generation. */
class NeuralModelDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
    private val errorLogger: ErrorLogger? = null,
) {
    private val modelsDir = File(context.filesDir, "neural_models").apply { mkdirs() }

    private val _downloadStates = MutableStateFlow<Map<NeuralEngineType, ModelDownloadState>>(
        NeuralEngineType.entries.associateWith { engine ->
            val file = getModelFile(engine)
            if (file.exists() && file.length() > 0) ModelDownloadState.Ready(engine, file)
            else ModelDownloadState.Idle
        }
    )
    val downloadStates: StateFlow<Map<NeuralEngineType, ModelDownloadState>> = _downloadStates.asStateFlow()

    fun getModelFile(engineType: NeuralEngineType): File = when (engineType) {
        NeuralEngineType.SOUNDGEN -> File(modelsDir, "soundgen_small.bin")
        NeuralEngineType.MUSICGEN_SMALL -> File(modelsDir, "musicgen_small.bin")
        NeuralEngineType.STABLE_AUDIO_3_SMALL -> File(modelsDir, "stable_audio_3_small.bin")
    }

    fun getDefaultDownloadUrl(engineType: NeuralEngineType): String = when (engineType) {
        NeuralEngineType.SOUNDGEN -> "https://huggingface.co/facebook/audiogen-medium/resolve/main/state_dict.bin"
        NeuralEngineType.MUSICGEN_SMALL -> "https://huggingface.co/facebook/musicgen-small/resolve/main/pytorch_model.bin"
        NeuralEngineType.STABLE_AUDIO_3_SMALL -> "https://huggingface.co/Comfy-Org/stable-audio-open-1.0_repackaged/resolve/main/stable-audio-open-1.0.safetensors"
    }

    fun isModelAvailable(engineType: NeuralEngineType): Boolean {
        val file = getModelFile(engineType)
        return file.exists() && file.length() > 0
    }

    fun getModelSizeBytes(engineType: NeuralEngineType): Long {
        val file = getModelFile(engineType)
        return if (file.exists()) file.length() else 0L
    }

    fun deleteModel(engineType: NeuralEngineType): Boolean {
        val file = getModelFile(engineType)
        val deleted = if (file.exists()) file.delete() else true
        updateState(engineType, ModelDownloadState.Idle)
        return deleted
    }

    suspend fun downloadModel(
        engineType: NeuralEngineType,
        downloadUrl: String = getDefaultDownloadUrl(engineType),
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getModelFile(engineType)
        if (isModelAvailable(engineType)) {
            updateState(engineType, ModelDownloadState.Ready(engineType, targetFile))
            return@withContext true
        }

        updateState(engineType, ModelDownloadState.Downloading(engineType, 0, 0, 0))

        try {
            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed with HTTP ${response.code}"
                    updateState(engineType, ModelDownloadState.Error(engineType, msg))
                    return@withContext false
                }

                val body = response.body
                val totalBytes = body.contentLength()
                var bytesDownloaded = 0L
                val tempFile = File(modelsDir, "${targetFile.name}.tmp")

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
                                val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
                                updateState(engineType, ModelDownloadState.Downloading(engineType, progress, bytesDownloaded, totalBytes))
                            }
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    updateState(engineType, ModelDownloadState.Ready(engineType, targetFile))
                    true
                } else {
                    updateState(engineType, ModelDownloadState.Error(engineType, "Failed to save model file"))
                    false
                }
            }
        } catch (e: Exception) {
            updateState(engineType, ModelDownloadState.Error(engineType, e.message ?: "Unknown download error"))
            false
        }
    }

    private fun updateState(engineType: NeuralEngineType, state: ModelDownloadState) {
        val currentMap = _downloadStates.value.toMutableMap()
        currentMap[engineType] = state
        _downloadStates.value = currentMap
    }
}
