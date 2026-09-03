package fr.geoking.arthur.fractal

import android.content.Context
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.source.CustomFractalSource

/**
 * Persists authored Custom Fractal Artwork ids (encoded params) in SharedPreferences.
 */
class CustomFractalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(): List<Artwork> {
        val raw = prefs.getString(KEY_IDS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP)
            .mapNotNull { id ->
                val params = CustomFractalParams.fromArtworkId(id) ?: return@mapNotNull null
                CustomFractalSource.artwork(id, params.toArtworkTitle())
            }
    }

    fun save(params: CustomFractalParams): Artwork {
        val normalized = params.normalized()
        val id = normalized.toArtworkId()
        val existing = prefs.getString(KEY_IDS, "").orEmpty()
            .split(SEP)
            .filter { it.isNotBlank() }
            .toMutableList()
        if (id !in existing) {
            existing.add(0, id)
            prefs.edit().putString(KEY_IDS, existing.joinToString(SEP)).apply()
        }
        return CustomFractalSource.artwork(id, normalized.toArtworkTitle())
    }

    fun clear() {
        prefs.edit().remove(KEY_IDS).apply()
    }

    companion object {
        private const val PREFS = "arthur_custom_fractal"
        private const val KEY_IDS = "ids"
        private const val SEP = "\n"
    }
}
