package fr.geoking.arthur.shared.source

import kotlin.random.Random

/**
 * Ambient / catalog variety helpers for remote museum (and stock) Sources.
 *
 * Prefer a provider random endpoint when one exists; otherwise sample a large
 * search window and renew via a new page offset or a fresh [load] call.
 */
object RemoteSample {

    /** How many search hits to pull before sampling down to the Source limit. */
    const val SEARCH_POOL = 60

    /** Upper bound for 1-based page APIs (Harvard, …). */
    const val MAX_PAGE = 20

    /**
     * Artic (and similar ES search APIs) reject windows past ~[maxHits] results
     * (`Invalid number of results`). Cap 1-based pages so `(page-1)*pool < maxHits`.
     */
    fun maxPageForHitWindow(pool: Int = SEARCH_POOL, maxHits: Int = 1000): Int =
        (maxHits / pool.coerceAtLeast(1)).coerceAtLeast(1)

    /** Upper bound for 0-based start offsets (Smithsonian), in result rows. */
    const val MAX_START = 200

    /** Europeana refuses `start` beyond the first 1000 hits (use cursor past that). */
    const val EUROPEANA_MAX_START = 940

    /**
     * Random subset of [items] (size [count]).
     * Uses index draws (not [List.shuffled]) so a [Random] that always returns 0
     * yields a stable prefix — useful for fixture tests.
     */
    fun <T> sample(items: List<T>, count: Int, random: Random = Random.Default): List<T> {
        if (count <= 0 || items.isEmpty()) return emptyList()
        if (items.size <= count) return items.toList()
        val remaining = items.indices.toMutableList()
        val chosen = ArrayList<T>(count)
        repeat(count) {
            val pick = random.nextInt(remaining.size)
            chosen.add(items[remaining.removeAt(pick)])
        }
        return chosen
    }

    /** 1-based page index in `1..maxPage`. */
    fun randomPage(maxPage: Int = MAX_PAGE, random: Random = Random.Default): Int {
        val max = maxPage.coerceAtLeast(1)
        return 1 + random.nextInt(max)
    }

    /**
     * 0-based start offset aligned to [pageSize], within `0..maxStart`.
     * Europeana uses 1-based starts — add 1 at the call site.
     */
    fun randomStart(
        pageSize: Int,
        maxStart: Int = MAX_START,
        random: Random = Random.Default,
    ): Int {
        val step = pageSize.coerceAtLeast(1)
        val maxAligned = (maxStart / step) * step
        if (maxAligned <= 0) return 0
        val slots = maxAligned / step
        return random.nextInt(slots + 1) * step
    }

    /**
     * Fetch a search window at [randomOffset]; if empty and offset ≠ [firstOffset],
     * retry at [firstOffset] (deep pages / skips often return nothing).
     */
    suspend fun fetchWindow(
        randomOffset: Int,
        firstOffset: Int,
        fetch: suspend (offset: Int) -> String,
        isEmpty: (String) -> Boolean,
    ): String {
        val body = fetch(randomOffset)
        return if (randomOffset != firstOffset && isEmpty(body)) {
            fetch(firstOffset)
        } else {
            body
        }
    }
}
