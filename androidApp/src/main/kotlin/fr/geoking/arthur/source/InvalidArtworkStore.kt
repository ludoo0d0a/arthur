package fr.geoking.arthur.source

import android.content.Context
import java.util.Collections
import java.util.LinkedHashSet

/**
 * Short denylist of artwork ids that failed permanently (non-retryable download or
 * undecodable image). Cap [MAX_IDS] FIFO so renews can eventually retry.
 */
class InvalidArtworkStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val ids: LinkedHashSet<String> = LinkedHashSet(load())

    @Synchronized
    fun isInvalid(artworkId: String): Boolean = artworkId in ids

    @Synchronized
    fun markInvalid(artworkId: String) {
        if (artworkId.isBlank()) return
        if (!ids.add(artworkId)) return
        while (ids.size > MAX_IDS) {
            val oldest = ids.iterator().next()
            ids.remove(oldest)
        }
        persist()
    }

    @Synchronized
    fun clear(artworkId: String) {
        if (ids.remove(artworkId)) persist()
    }

    @Synchronized
    fun snapshot(): Set<String> = Collections.unmodifiableSet(LinkedHashSet(ids))

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
