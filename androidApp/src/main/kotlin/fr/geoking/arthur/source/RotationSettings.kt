package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.auto.AmbientAlbumArt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists how long each artwork stays on screen before ambient rotation advances. */
class RotationSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _intervalMs = MutableStateFlow(loadIntervalMs())
    val intervalMs: StateFlow<Long> = _intervalMs.asStateFlow()

    fun setIntervalMs(ms: Long) {
        val value = if (ms in OPTIONS_MS) ms else AmbientAlbumArt.ROTATION_INTERVAL_MS
        if (_intervalMs.value == value) return
        prefs.edit().putLong(KEY_INTERVAL_MS, value).apply()
        _intervalMs.value = value
    }

    private fun loadIntervalMs(): Long {
        val stored = prefs.getLong(KEY_INTERVAL_MS, AmbientAlbumArt.ROTATION_INTERVAL_MS)
        return if (stored in OPTIONS_MS) stored else AmbientAlbumArt.ROTATION_INTERVAL_MS
    }

    companion object {
        val OPTIONS_MS: List<Long> = listOf(
            10_000L,
            20_000L,
            60_000L,
            120_000L,
            180_000L,
            240_000L,
            300_000L,
            600_000L,
            900_000L,
            1_200_000L,
            1_800_000L,
        )

        private const val PREFS = "arthur_rotation"
        private const val KEY_INTERVAL_MS = "interval_ms"
    }
}
