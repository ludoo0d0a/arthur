package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import kotlin.random.Random

/**
 * Shared Ambient load loop for museum Sources that search one or more
 * [MuseumSearchKind] targets (Painting / Sculpture / Photo / All).
 */
internal object MuseumLoad {

    /**
     * Expand [kind] into concrete targets and load just **one** of them per call — the
     * one at [nextTargetIndex], which the caller advances every `load()` invocation.
     * This defers the other target(s) to later calls (pool renewal / load-more) instead
     * of fanning out into every target's search+hydrate subquery chain up front, and
     * lets a single call return the full [limit] from one target rather than splitting
     * it across all of them.
     */
    suspend fun acrossTargets(
        kind: MuseumSearchKind,
        limit: Int,
        random: Random,
        nextTargetIndex: () -> Int,
        loadTarget: suspend (target: MuseumSearchKind, perKind: Int) -> List<Artwork>,
    ): List<Artwork> {
        val targets = RemoteCategoryMapping.museumTargets(kind)
        if (targets.isEmpty() || limit <= 0) return emptyList()
        val target = targets[nextTargetIndex().mod(targets.size)]
        val loaded = runCatching { loadTarget(target, limit) }
            .onFailure { e -> if (e is kotlinx.coroutines.CancellationException) throw e }
            .getOrDefault(emptyList())
        return RemoteSample.sample(loaded, limit, random)
    }
}
