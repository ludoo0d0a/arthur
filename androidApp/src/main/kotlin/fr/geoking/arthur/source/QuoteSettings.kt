package fr.geoking.arthur.source

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User preference: show famous quotes on Ambient slides. */
class QuoteSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _showQuotes = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_QUOTES, DEFAULT_SHOW_QUOTES),
    )
    val showQuotes: StateFlow<Boolean> = _showQuotes.asStateFlow()

    fun setShowQuotes(enabled: Boolean) {
        if (_showQuotes.value == enabled) return
        prefs.edit().putBoolean(KEY_SHOW_QUOTES, enabled).apply()
        _showQuotes.value = enabled
    }

    companion object {
        const val DEFAULT_SHOW_QUOTES = true
        private const val PREFS = "arthur_quotes"
        private const val KEY_SHOW_QUOTES = "show_quotes"
    }
}
