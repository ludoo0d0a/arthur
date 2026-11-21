package fr.geoking.arthur.shared.marketplace

import fr.geoking.arthur.shared.source.GenartSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenartPackTopicsTest {
    @Test
    fun categoryPacks_arePairwiseDisjoint() {
        val sets = GenartPackTopics.categoryTopicSuffixes.map { suffix ->
            suffix to GenartPackTopics.engineIdsForTopic(suffix)
        }
        for (i in sets.indices) {
            for (j in (i + 1) until sets.size) {
                val overlap = sets[i].second.intersect(sets[j].second)
                assertTrue(
                    overlap.isEmpty(),
                    "overlap between ${sets[i].first} and ${sets[j].first}: $overlap",
                )
            }
        }
    }

    @Test
    fun everyEngine_isInExactlyOneCategoryPack() {
        val engineIds = GenartSource.defaultCatalog().map { it.id }.toSet()
        engineIds.forEach { id ->
            val covering = GenartPackTopics.topicsCoveringEngine(id)
            assertEquals(
                1,
                covering.size,
                "engine $id should be in exactly one pack, was $covering",
            )
        }
        val unionSize = GenartPackTopics.categoryTopicSuffixes.sumOf {
            GenartPackTopics.engineIdsForTopic(it).size
        }
        assertEquals(engineIds.size, unionSize)
    }
}
