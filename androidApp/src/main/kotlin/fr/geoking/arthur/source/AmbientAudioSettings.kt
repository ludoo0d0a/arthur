package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.audio.MusicStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Legacy pad/chime modes — kept for persistence compatibility; prefer [AmbientAudioCharacter]. */
enum class AmbientAudioMode {
    PAD_AND_CHIME,
    CHIME_ONLY,
    PAD_ONLY,
}

/** User-facing soundtrack character. */
enum class AmbientAudioCharacter {
    /** Melody-forward; pads off/quiet, no noise textures. */
    Melody,
    /** Light pad bed under melody. */
    Balanced,
    /** Allows drone beds and style textures (ocean/wind/cosmic). */
    Atmosphere,
}

/** Persists ambient audio preferences. */
class AmbientAudioSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _volume = MutableStateFlow(prefs.getFloat(KEY_VOLUME, 0.5f).coerceIn(0f, 1f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _mode = MutableStateFlow(
        runCatching {
            AmbientAudioMode.valueOf(prefs.getString(KEY_MODE, AmbientAudioMode.CHIME_ONLY.name)!!)
        }.getOrDefault(AmbientAudioMode.CHIME_ONLY),
    )
    val mode: StateFlow<AmbientAudioMode> = _mode.asStateFlow()

    private val _character = MutableStateFlow(loadCharacter())
    val character: StateFlow<AmbientAudioCharacter> = _character.asStateFlow()

    private val _stylePreference = MutableStateFlow(loadStylePreference())
    /** null = Auto (artwork-derived). */
    val stylePreference: StateFlow<MusicStyle?> = _stylePreference.asStateFlow()

    private val _complexity = MutableStateFlow(prefs.getFloat(KEY_COMPLEXITY, 0.55f).coerceIn(0f, 1f))
    val complexity: StateFlow<Float> = _complexity.asStateFlow()

    private fun loadCharacter(): AmbientAudioCharacter {
        val stored = prefs.getString(KEY_CHARACTER, null)
        if (stored != null) {
            return runCatching { AmbientAudioCharacter.valueOf(stored) }
                .getOrDefault(AmbientAudioCharacter.Melody)
        }
        // Migrate from legacy mode when character was never set.
        return when (_mode.value) {
            AmbientAudioMode.CHIME_ONLY -> AmbientAudioCharacter.Melody
            AmbientAudioMode.PAD_ONLY -> AmbientAudioCharacter.Atmosphere
            AmbientAudioMode.PAD_AND_CHIME -> AmbientAudioCharacter.Balanced
        }
    }

    private fun loadStylePreference(): MusicStyle? {
        val raw = prefs.getString(KEY_STYLE_PREF, STYLE_AUTO) ?: STYLE_AUTO
        if (raw == STYLE_AUTO) return null
        return runCatching { MusicStyle.valueOf(raw) }.getOrNull()
    }

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

    fun setCharacter(value: AmbientAudioCharacter) {
        if (_character.value == value) return
        prefs.edit().putString(KEY_CHARACTER, value.name).apply()
        _character.value = value
        // Keep legacy mode in sync for older call sites / tests.
        setMode(
            when (value) {
                AmbientAudioCharacter.Melody -> AmbientAudioMode.CHIME_ONLY
                AmbientAudioCharacter.Balanced -> AmbientAudioMode.PAD_AND_CHIME
                AmbientAudioCharacter.Atmosphere -> AmbientAudioMode.PAD_AND_CHIME
            },
        )
    }

    fun setStylePreference(value: MusicStyle?) {
        if (_stylePreference.value == value) return
        val raw = value?.name ?: STYLE_AUTO
        prefs.edit().putString(KEY_STYLE_PREF, raw).apply()
        _stylePreference.value = value
    }

    fun setComplexity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (_complexity.value == clamped) return
        prefs.edit().putFloat(KEY_COMPLEXITY, clamped).apply()
        _complexity.value = clamped
    }

    companion object {
        private const val PREFS = "arthur_ambient_audio"
        private const val KEY_ENABLED = "audio_enabled"
        private const val KEY_VOLUME = "audio_volume"
        private const val KEY_MODE = "audio_mode"
        private const val KEY_CHARACTER = "audio_character"
        private const val KEY_STYLE_PREF = "audio_style_pref"
        private const val KEY_COMPLEXITY = "audio_complexity"
        private const val STYLE_AUTO = "AUTO"
    }
}
