package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.shared.domain.ArtUniverse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the active art universe (Gallery vs Generative).
 * [activeUniverse] is null only before the first preference is chosen.
 */
class UniverseSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _activeUniverse = MutableStateFlow(loadUniverse())
    val activeUniverse: StateFlow<ArtUniverse?> = _activeUniverse.asStateFlow()

    private val _hasSwitchedOnce = MutableStateFlow(prefs.getBoolean(KEY_SWITCHED_ONCE, false))
    val hasSwitchedOnce: StateFlow<Boolean> = _hasSwitchedOnce.asStateFlow()

    /**
     * Sets the active universe. [isFirstSelection] should be true only from the
     * first-run picker (analytics: universe_selected); later switches use
     * universe_switched and mark [hasSwitchedOnce].
     */
    fun setActiveUniverse(universe: ArtUniverse, isFirstSelection: Boolean = false) {
        val previous = _activeUniverse.value
        if (previous == universe && !isFirstSelection) return
        prefs.edit().putString(KEY_UNIVERSE, universe.storageKey).apply()
        _activeUniverse.value = universe
        if (!isFirstSelection && previous != null && previous != universe) {
            markSwitchedOnce()
        }
    }

    fun markSwitchedOnce() {
        if (_hasSwitchedOnce.value) return
        prefs.edit().putBoolean(KEY_SWITCHED_ONCE, true).apply()
        _hasSwitchedOnce.value = true
    }

    private fun loadUniverse(): ArtUniverse? =
        ArtUniverse.fromStorageKey(prefs.getString(KEY_UNIVERSE, null))

    companion object {
        private const val PREFS = "arthur_universe"
        private const val KEY_UNIVERSE = "active_universe"
        private const val KEY_SWITCHED_ONCE = "has_switched_once"
    }
}
