package fr.geoking.arthur.audio.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class MusicDownloadProgress {
    data class Downloading(val progress: Float) : MusicDownloadProgress()
    data object Completed : MusicDownloadProgress()
    data class Failed(val message: String) : MusicDownloadProgress()
}

/** Process-wide download progress so UI survives Activity recreation. */
object MusicDownloadTracker {
    private val _states = MutableStateFlow<Map<String, MusicDownloadProgress>>(emptyMap())
    val states: StateFlow<Map<String, MusicDownloadProgress>> = _states.asStateFlow()

    fun setDownloading(trackId: String, progress: Float) {
        _states.update { it + (trackId to MusicDownloadProgress.Downloading(progress.coerceIn(0f, 1f))) }
    }

    fun setCompleted(trackId: String) {
        _states.update { it + (trackId to MusicDownloadProgress.Completed) }
    }

    fun setFailed(trackId: String, message: String) {
        _states.update { it + (trackId to MusicDownloadProgress.Failed(message)) }
    }

    fun clear(trackId: String) {
        _states.update { it - trackId }
    }

    fun isActive(trackId: String): Boolean =
        _states.value[trackId] is MusicDownloadProgress.Downloading
}
