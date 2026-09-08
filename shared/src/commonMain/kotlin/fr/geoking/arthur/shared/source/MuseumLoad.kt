package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import kotlin.random.Random

/**
 * Shared Ambient load loop for museum Sources that search one or more
 * [MuseumSearchKind] targets (Painting / Sculpture / Photo / All).
 */
internal object MuseumLoad {

    /**
     * Expand [kind] into concrete targets, load up to [limit] / N each, then sample
     * the merged list down to [limit].
     */
    suspend fun acrossTargets(
        kind: MuseumSearchKind,
        limit: Int,
        random: Random,
        loadTarget: suspend (target: MuseumSearchKind, perKind: Int) -> List<Artwork>,
    ): List<Artwork> {
        val targets = RemoteCategoryMapping.museumTargets(kind)
        if (targets.isEmpty() || limit <= 0) return emptyList()
        val perKind = (limit / targets.size).coerceAtLeast(1)
        val results = ArrayList<Artwork>(limit)
        for (target in targets) {
            // One failing target (timeout / 403 body) must not wipe siblings.
            results.addAll(
                runCatching { loadTarget(target, perKind) }
                    .onFailure { e -> if (e is kotlinx.coroutines.CancellationException) throw e }
                    .getOrDefault(emptyList())
            )
        }
        return RemoteSample.sample(results, limit, random)
    }
}
