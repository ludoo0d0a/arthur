package fr.geoking.arthur.source

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AmbientAudioMode {
    PAD_AND_CHIME,
    CHIME_ONLY,
    PAD_ONLY,
}

/** Persists ambient audio preferences: enabled state, volume, and audio mode. */
class AmbientAudioSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _volume = MutableStateFlow(prefs.getFloat(KEY_VOLUME, 0.5f).coerceIn(0f, 1f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _mode = MutableStateFlow(
        runCatching {
            AmbientAudioMode.valueOf(prefs.getString(KEY_MODE, AmbientAudioMode.PAD_AND_CHIME.name)!!)
        }.getOrDefault(AmbientAudioMode.PAD_AND_CHIME)
    )
    val mode: StateFlow<AmbientAudioMode> = _mode.asStateFlow()

    fun setEnabled(value: Boolean) {
        if (_enabled.value == value) return
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    fun setVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_volume.value == clamped) return
        prefs.edit().putFloat(KEY_VOLUME, clamped).apply()
        _volume.value = clamped
    }

    fun setMode(value: AmbientAudioMode) {
        if (_mode.value == value) return
        prefs.edit().putString(KEY_MODE, value.name).apply()
        _mode.value = value
    }

    companion object {
        private const val PREFS = "arthur_ambient_audio"
        private const val KEY_ENABLED = "audio_enabled"
        private const val KEY_VOLUME = "audio_volume"
        private const val KEY_MODE = "audio_mode"
    }
}
