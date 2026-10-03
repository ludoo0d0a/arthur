package fr.geoking.arthur.shared.source

import kotlin.random.Random

/**
 * Ambient / catalog variety helpers for remote museum (and stock) Sources.
 *
 * Prefer a provider random endpoint / sort when one exists (Harvard, Europeana,
 * Smithsonian, Artic `random_score`); otherwise jump to a random search window
 * and sample down to the Source limit.
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

    /**
     * Cleveland Open Access accepts large `skip` values; empty deep pages fall
     * back to [fetchWindow]'s first-offset retry.
     */
    const val CLEVELAND_MAX_START = 5_000

    /** Europeana refuses `start` beyond the first 1000 hits (use cursor past that). */
    const val EUROPEANA_MAX_START = 940

    /**
     * Met v1.1 search: `offset + limit` must not exceed 10_000.
     * With [SEARCH_POOL] as limit, keep start ≤ 10_000 − pool.
     */
    const val MET_MAX_START = 10_000 - SEARCH_POOL

    /**
     * Seed for provider random sorts (`sort=random:SEED`, `sort=random_SEED`,
     * Artic `random_score.seed`). Always non-negative.
     */
    fun randomSeed(random: Random = Random.Default): Int =
        random.nextInt(Int.MAX_VALUE)

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
     * 1-based page index in `1..maxPage`, advancing forward from [cursor] instead of
     * rolling a new random page — repeated [load] calls on the same Source instance
     * walk through the result set ("load more") rather than re-sampling overlapping
     * content, wrapping back to page 1 once [maxPage] is exhausted.
     */
    fun nextPage(cursor: Int, maxPage: Int = MAX_PAGE): Int {
        val max = maxPage.coerceAtLeast(1)
        return 1 + cursor.mod(max)
    }

    /**
     * 0-based start offset aligned to [pageSize], advancing forward from [cursor]
     * within `0..maxStart` instead of picking a new random offset — see [nextPage].
     */
    fun nextStart(cursor: Int, pageSize: Int, maxStart: Int = MAX_START): Int {
        val step = pageSize.coerceAtLeast(1)
        val maxAligned = (maxStart / step) * step
        if (maxAligned <= 0) return 0
        val slots = maxAligned / step + 1
        return cursor.mod(slots) * step
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

    /** RFC 3986 unreserved-safe percent-encoding for query parameter values. */
    fun percentEncode(value: String): String = buildString(value.length + 16) {
        for (ch in value) {
            when (ch) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9', '-', '_', '.', '~' -> append(ch)
                ' ' -> append("%20")
                else -> append('%')
                    .append(ch.code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
}
