package fr.geoking.arthur.source

import kotlin.random.Random

/** Muzei-like random next pick: prefer unseen, then non-recent, then any eligible. */
object AmbientStillPicker {
    fun pickNextRandom(
        poolIds: List<String>,
        currentId: String?,
        seenIds: Set<String>,
        recentIds: List<String>,
        eligibleIds: Set<String>? = null,
        random: Random = Random.Default,
    ): String? {
        if (poolIds.isEmpty()) return null
        val base = if (eligibleIds != null) {
            poolIds.filter { it in eligibleIds }
        } else {
            poolIds
        }
        if (base.isEmpty()) return null

        fun pick(from: List<String>): String? =
            from.filter { it != currentId }.randomOrNull(random)
                ?: from.randomOrNull(random)

        val unseen = base.filter { it !in seenIds }
        pick(unseen)?.let { return it }

        val recentSet = recentIds.toSet()
        val nonRecent = base.filter { it !in recentSet }
        pick(nonRecent)?.let { return it }

        return pick(base)
    }
}
