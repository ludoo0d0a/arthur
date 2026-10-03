package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource
import fr.geoking.arthur.ui.components.MuseumTopic
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.PhotoTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists the user's default screensaver pack selection for Android TV Dream / screensaver. */
class ScreensaverSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _defaultPack = MutableStateFlow(loadDefaultPack())
    val defaultPack: StateFlow<PackSelection?> = _defaultPack.asStateFlow()

    fun setDefaultPack(selection: PackSelection?) {
        if (_defaultPack.value == selection) return
        prefs.edit().apply {
            if (selection == null) {
                remove(KEY_FAMILY)
                remove(KEY_SUB_ID)
            } else {
                putString(KEY_FAMILY, selection.family.name)
                putString(KEY_SUB_ID, selection.subId)
            }
        }.apply()
        _defaultPack.value = selection
    }

    private fun loadDefaultPack(): PackSelection? {
        val familyName = prefs.getString(KEY_FAMILY, null) ?: return null
        val family = runCatching { PackFamily.valueOf(familyName) }.getOrNull() ?: return null
        val subId = prefs.getString(KEY_SUB_ID, null)
        val raw = PackSelection(family, subId)
        val migrated = migrateLegacySelection(raw)
        if (migrated != raw) {
            prefs.edit()
                .putString(KEY_FAMILY, migrated.family.name)
                .putString(KEY_SUB_ID, migrated.subId)
                .apply()
        }
        return migrated
    }

    /**
     * Former Museum → Wikimedia Street Art tile now lives under Photo → Wikimedia.
     */
    private fun migrateLegacySelection(selection: PackSelection): PackSelection {
        if (selection.family == PackFamily.Museum &&
            selection.subId == WikimediaStreetArtSource.ID
        ) {
            return PackSelection(PackFamily.Photo, PhotoTopic.Wikimedia.testTagSuffix)
        }
        return selection
    }

    companion object {
        val DEFAULT_PACK_SELECTION = PackSelection(PackFamily.Museum, MuseumTopic.Random.testTagSuffix)

        private const val PREFS = "arthur_screensaver"
        private const val KEY_FAMILY = "default_pack_family"
        private const val KEY_SUB_ID = "default_pack_sub_id"
    }
}
