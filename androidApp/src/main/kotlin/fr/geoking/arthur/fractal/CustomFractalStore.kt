package fr.geoking.arthur.fractal

import android.content.Context
import fr.geoking.arthur.db.CustomFractalDao
import fr.geoking.arthur.db.CustomFractalEntity
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.source.CustomFractalSource
import kotlinx.coroutines.runBlocking

/**
 * Persists authored Custom Fractal Artwork ids (encoded params) in Room 3 [CustomFractalDao].
 */
class CustomFractalStore(
    context: Context,
    private val customFractalDao: CustomFractalDao? = null,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        if (customFractalDao != null) {
            runBlocking {
                val dbEntities = customFractalDao.getAll()
                if (dbEntities.isEmpty()) {
                    val spItems = listFromPrefs()
                    val now = System.currentTimeMillis()
                    spItems.forEachIndexed { index, art ->
                        customFractalDao.insertOrUpdate(
                            CustomFractalEntity(
                                id = art.id,
                                title = art.title,
                                createdAt = now - index,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun list(): List<Artwork> {
        if (customFractalDao != null) {
            val entities = runBlocking { customFractalDao.getAll() }
            return entities.mapNotNull { entity ->
                val params = CustomFractalParams.fromArtworkId(entity.id) ?: return@mapNotNull null
                CustomFractalSource.artwork(entity.id, entity.title)
            }
        }
        return listFromPrefs()
    }

    fun save(params: CustomFractalParams): Artwork {
        val normalized = params.normalized()
        val id = normalized.toArtworkId()
        val title = normalized.toArtworkTitle()

        val existing = prefs.getString(KEY_IDS, "").orEmpty()
            .split(SEP)
            .filter { it.isNotBlank() }
            .toMutableList()
        if (id !in existing) {
            existing.add(0, id)
            prefs.edit().putString(KEY_IDS, existing.joinToString(SEP)).apply()
        }

        if (customFractalDao != null) {
            runBlocking {
                customFractalDao.insertOrUpdate(
                    CustomFractalEntity(
                        id = id,
                        title = title,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        return CustomFractalSource.artwork(id, title)
    }

    fun clear() {
        prefs.edit().remove(KEY_IDS).apply()
        if (customFractalDao != null) {
            runBlocking { customFractalDao.deleteAll() }
        }
    }

    private fun listFromPrefs(): List<Artwork> {
        val raw = prefs.getString(KEY_IDS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP)
            .mapNotNull { id ->
                val params = CustomFractalParams.fromArtworkId(id) ?: return@mapNotNull null
                CustomFractalSource.artwork(id, params.toArtworkTitle())
            }
    }

    companion object {
        private const val PREFS = "arthur_custom_fractal"
        private const val KEY_IDS = "ids"
        private const val SEP = "\n"
    }
}
