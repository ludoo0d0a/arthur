package fr.geoking.arthur.shared.engine

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FreeTierLimits
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.marketplace.PackOwnership
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentEngineFlowTest {

    private class TestSource(
        override val id: String,
        private val count: Int = 10,
    ) : Source {
        override val displayName = id
        var lastLimitPassed: Int? = null

        override suspend fun load(): List<Artwork> = load(20)

        override suspend fun load(limit: Int): List<Artwork> {
            lastLimitPassed = limit
            return (1..limit.coerceAtMost(count)).map { i ->
                Artwork(
                    id = "$id-$i",
                    title = "$id $i",
                    sourceId = id,
                    kind = ArtworkKind.Painting,
                    remoteUrl = "https://example.com/$id-$i.jpg",
                )
            }
        }
    }

    @Test
    fun catalogFlow_emitsProgressivelyAndUsesAdaptiveLimits() = runBlocking {
        val s1 = TestSource("s1")
        val s2 = TestSource("s2")
        val s3 = TestSource("s3")
        val engine = ContentEngine(
            sources = listOf(s1, s2, s3),
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxPhotoArtwork = 12),
        )

        val prepared = PreparedRotation(sourceIds = listOf("s1", "s2", "s3"), artworkIds = emptyList())
        val emissions = engine.catalogFlow(prepared).toList()

        assertTrue(emissions.isNotEmpty())
        val finalEmission = emissions.last()
        assertTrue(finalEmission.isNotEmpty())

        // 12 / 3 sources = 4 per source
        assertEquals(4, s1.lastLimitPassed)
        assertEquals(4, s2.lastLimitPassed)
        assertEquals(4, s3.lastLimitPassed)
    }

    @Test
    fun catalog_multiSource_appliesAdaptiveLimit() = runBlocking {
        val sources = (1..9).map { TestSource("met-$it", 20) }
        val engine = ContentEngine(
            sources = sources,
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxPhotoArtwork = 24),
        )

        val prepared = PreparedRotation(sourceIds = sources.map { it.id }, artworkIds = emptyList())
        val result = engine.catalog(prepared)

        // 24 / 9 = 2.66 -> maxOf(3, ceil(24/9)) = 3 per source; fair sample caps to 24
        assertEquals(24, result.size)
        assertEquals(3, sources.first().lastLimitPassed)
    }
}
