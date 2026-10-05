package fr.geoking.arthur.pairing

import android.content.Context

/** Remembers the last TV host the phone successfully paired with. */
class LanPairingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var lastHost: String
        get() = prefs.getString(KEY_HOST, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_HOST, value.trim()).apply()
        }

    companion object {
        private const val PREFS = "lan_pairing"
        private const val KEY_HOST = "last_host"
    }
}
