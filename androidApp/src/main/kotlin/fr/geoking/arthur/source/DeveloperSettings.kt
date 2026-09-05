package fr.geoking.arthur.source

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Debug-only prefs (developer menu). Not applied in release. */
class DeveloperSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _simulatePremium = MutableStateFlow(
        prefs.getBoolean(KEY_SIMULATE_PREMIUM, DEFAULT_SIMULATE_PREMIUM),
    )
    val simulatePremium: StateFlow<Boolean> = _simulatePremium.asStateFlow()

    fun setSimulatePremium(enabled: Boolean) {
        if (_simulatePremium.value == enabled) return
        prefs.edit().putBoolean(KEY_SIMULATE_PREMIUM, enabled).apply()
        _simulatePremium.value = enabled
    }

    companion object {
        const val DEFAULT_SIMULATE_PREMIUM = true
        private const val PREFS = "arthur_developer"
        private const val KEY_SIMULATE_PREMIUM = "simulate_premium"
    }
}
