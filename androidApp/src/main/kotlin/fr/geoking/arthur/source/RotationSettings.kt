package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.auto.AmbientAlbumArt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists ambient rotation interval, Wi‑Fi-only downloads, and recent still ring. */
class RotationSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _phoneIntervalMs = MutableStateFlow(loadIntervalMs(KEY_PHONE_INTERVAL_MS, KEY_INTERVAL_MS))
    val phoneIntervalMs: StateFlow<Long> = _phoneIntervalMs.asStateFlow()

    private val _tvIntervalMs = MutableStateFlow(loadIntervalMs(KEY_TV_INTERVAL_MS))
    val tvIntervalMs: StateFlow<Long> = _tvIntervalMs.asStateFlow()

    private val _autoIntervalMs = MutableStateFlow(loadIntervalMs(KEY_AUTO_INTERVAL_MS))
    val autoIntervalMs: StateFlow<Long> = _autoIntervalMs.asStateFlow()

    @Deprecated("Use phoneIntervalMs, tvIntervalMs, or autoIntervalMs based on target device", ReplaceWith("phoneIntervalMs"))
    val intervalMs: StateFlow<Long> get() = phoneIntervalMs

    private val _wifiOnlyRemoteStills = MutableStateFlow(
        prefs.getBoolean(KEY_WIFI_ONLY, false),
    )
    val wifiOnlyRemoteStills: StateFlow<Boolean> = _wifiOnlyRemoteStills.asStateFlow()

    fun setPhoneIntervalMs(ms: Long) {
        val value = if (ms in OPTIONS_MS) ms else AmbientAlbumArt.ROTATION_INTERVAL_MS
        if (_phoneIntervalMs.value == value) return
        prefs.edit().putLong(KEY_PHONE_INTERVAL_MS, value).apply()
        _phoneIntervalMs.value = value
    }

    fun setTvIntervalMs(ms: Long) {
        val value = if (ms in OPTIONS_MS) ms else AmbientAlbumArt.ROTATION_INTERVAL_MS
        if (_tvIntervalMs.value == value) return
        prefs.edit().putLong(KEY_TV_INTERVAL_MS, value).apply()
        _tvIntervalMs.value = value
    }

    fun setAutoIntervalMs(ms: Long) {
        val value = if (ms in OPTIONS_MS) ms else AmbientAlbumArt.ROTATION_INTERVAL_MS
        if (_autoIntervalMs.value == value) return
        prefs.edit().putLong(KEY_AUTO_INTERVAL_MS, value).apply()
        _autoIntervalMs.value = value
    }

    @Deprecated("Use setPhoneIntervalMs, setTvIntervalMs, or setAutoIntervalMs based on target device", ReplaceWith("setPhoneIntervalMs(ms)"))
    fun setIntervalMs(ms: Long) {
        setPhoneIntervalMs(ms)
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

    private fun loadIntervalMs(key: String, legacyKey: String? = null): Long {
        val defaultVal = AmbientAlbumArt.ROTATION_INTERVAL_MS
        val stored = if (prefs.contains(key)) {
            prefs.getLong(key, defaultVal)
        } else if (legacyKey != null && prefs.contains(legacyKey)) {
            prefs.getLong(legacyKey, defaultVal)
        } else {
            defaultVal
        }
        return if (stored in OPTIONS_MS) stored else defaultVal
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
        private const val KEY_PHONE_INTERVAL_MS = "interval_phone_ms"
        private const val KEY_TV_INTERVAL_MS = "interval_tv_ms"
        private const val KEY_AUTO_INTERVAL_MS = "interval_auto_ms"
        private const val KEY_WIFI_ONLY = "wifi_only_remote_stills"
        private const val KEY_RECENT_STILL_IDS = "recent_still_ids"
        private const val RECENT_SEP = "\n"
    }
}
