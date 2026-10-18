package fr.geoking.arthur.source

import android.content.Context
import fr.geoking.arthur.db.InvalidArtworkDao
import fr.geoking.arthur.db.InvalidArtworkEntity
import kotlinx.coroutines.runBlocking
import java.util.Collections
import java.util.LinkedHashSet

/**
 * Short denylist of artwork ids that failed permanently (non-retryable download or
 * undecodable image). Cap [MAX_IDS] FIFO so renews can eventually retry.
 * Backed by Room 3 [InvalidArtworkDao] with coroutine-first operations.
 */
class InvalidArtworkStore(
    context: Context,
    private val invalidArtworkDao: InvalidArtworkDao? = null,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val ids: LinkedHashSet<String> = LinkedHashSet(load())

    init {
        if (invalidArtworkDao != null) {
            runBlocking {
                val dbIds = invalidArtworkDao.getAllIds()
                if (dbIds.isEmpty() && ids.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    ids.forEach { id ->
                        invalidArtworkDao.markInvalid(InvalidArtworkEntity(id, now))
                    }
                } else if (dbIds.isNotEmpty()) {
                    ids.clear()
                    ids.addAll(dbIds)
                }
            }
        }
    }

    @Synchronized
    fun isInvalid(artworkId: String): Boolean {
        return artworkId in ids
    }

    @Synchronized
    fun markInvalid(artworkId: String) {
        if (artworkId.isBlank()) return
        if (!ids.add(artworkId)) return
        while (ids.size > MAX_IDS) {
            val oldest = ids.iterator().next()
            ids.remove(oldest)
        }
        persist()
        if (invalidArtworkDao != null) {
            runBlocking {
                invalidArtworkDao.markInvalid(InvalidArtworkEntity(artworkId, System.currentTimeMillis()))
                invalidArtworkDao.trimOldest(MAX_IDS)
            }
        }
    }

    @Synchronized
    fun clear(artworkId: String) {
        if (ids.remove(artworkId)) persist()
        if (invalidArtworkDao != null) {
            runBlocking {
                invalidArtworkDao.removeInvalid(artworkId)
            }
        }
    }

    @Synchronized
    fun snapshot(): Set<String> {
        if (invalidArtworkDao != null) {
            val dbIds = runBlocking { invalidArtworkDao.getAllIds() }
            return Collections.unmodifiableSet(LinkedHashSet(dbIds))
        }
        return Collections.unmodifiableSet(LinkedHashSet(ids))
    }

    private fun load(): List<String> =
        prefs.getString(KEY_IDS, "").orEmpty()
            .split(SEP)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun persist() {
        prefs.edit().putString(KEY_IDS, ids.joinToString(SEP)).apply()
    }

    companion object {
        const val MAX_IDS = 200
        private const val PREFS = "arthur_invalid_artwork"
        private const val KEY_IDS = "ids"
        private const val SEP = "\n"
    }
}
