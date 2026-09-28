package fr.geoking.arthur.source

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** User preferences for famous quotes on Ambient slides. */
class QuoteSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _showQuotes = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_QUOTES, DEFAULT_SHOW_QUOTES),
    )
    val showQuotes: StateFlow<Boolean> = _showQuotes.asStateFlow()

    private val _provider = MutableStateFlow(loadProvider())
    val provider: StateFlow<QuoteProvider> = _provider.asStateFlow()

    fun setShowQuotes(enabled: Boolean) {
        if (_showQuotes.value == enabled) return
        prefs.edit().putBoolean(KEY_SHOW_QUOTES, enabled).apply()
        _showQuotes.value = enabled
    }

    fun setProvider(provider: QuoteProvider) {
        if (_provider.value == provider) return
        prefs.edit().putString(KEY_PROVIDER, provider.id).apply()
        _provider.value = provider
    }

    private fun loadProvider(): QuoteProvider {
        val stored = prefs.getString(KEY_PROVIDER, null)
        if (stored != null) {
            return QuoteProvider.fromId(stored) ?: defaultProvider()
        }
        return defaultProvider()
    }

    private fun defaultProvider(): QuoteProvider {
        val lang = appContext.resources.configuration.locales[0]?.language
            ?: Locale.getDefault().language
        return QuoteProvider.defaultForLanguage(lang)
    }

    companion object {
        const val DEFAULT_SHOW_QUOTES = true
        private const val PREFS = "arthur_quotes"
        private const val KEY_SHOW_QUOTES = "show_quotes"
        private const val KEY_PROVIDER = "quote_provider"
    }
}
