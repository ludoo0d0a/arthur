package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.auto.AmbientAlbumArt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists ambient rotation interval, Wi‑Fi-only downloads, and recent still ring. */
class RotationSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _intervalMs = MutableStateFlow(loadIntervalMs())
    val intervalMs: StateFlow<Long> = _intervalMs.asStateFlow()

    private val _wifiOnlyRemoteStills = MutableStateFlow(
        prefs.getBoolean(KEY_WIFI_ONLY, false),
    )
    val wifiOnlyRemoteStills: StateFlow<Boolean> = _wifiOnlyRemoteStills.asStateFlow()

    fun setIntervalMs(ms: Long) {
        val value = if (ms in OPTIONS_MS) ms else AmbientAlbumArt.ROTATION_INTERVAL_MS
        if (_intervalMs.value == value) return
        prefs.edit().putLong(KEY_INTERVAL_MS, value).apply()
        _intervalMs.value = value
    }

    fun setWifiOnlyRemoteStills(enabled: Boolean) {
        if (_wifiOnlyRemoteStills.value == enabled) return
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        _wifiOnlyRemoteStills.value = enabled
    }

    /** Most-recent-first ring of remote still ids (museum/stock), max [RECENT_RING_SIZE]. */
    fun recentStillIds(): List<String> =
        prefs.getString(KEY_RECENT_STILL_IDS, "").orEmpty()
            .split(RECENT_SEP)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(RECENT_RING_SIZE)

    fun recordRecentStillId(artworkId: String) {
        if (artworkId.isBlank()) return
        val next = (listOf(artworkId) + recentStillIds().filter { it != artworkId })
            .take(RECENT_RING_SIZE)
        prefs.edit().putString(KEY_RECENT_STILL_IDS, next.joinToString(RECENT_SEP)).apply()
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

        const val RECENT_RING_SIZE = 12

        private const val PREFS = "arthur_rotation"
        private const val KEY_INTERVAL_MS = "interval_ms"
        private const val KEY_WIFI_ONLY = "wifi_only_remote_stills"
        private const val KEY_RECENT_STILL_IDS = "recent_still_ids"
        private const val RECENT_SEP = "\n"
    }
}
