package fr.geoking.arthur.shared.engine

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FreeTierLimits
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.marketplace.FakePackOwnership
import fr.geoking.arthur.shared.marketplace.GenartPackTopics
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ContentEngineTest {

    private val richSource = object : Source {
        override val id = "rich"
        override val displayName = "Rich"
        override suspend fun load() = listOf(
            Artwork("p1", "Photo 1", sourceId = id, kind = ArtworkKind.Photo, remoteUrl = "https://example.com/1.jpg"),
            Artwork("p2", "Photo 2", sourceId = id, kind = ArtworkKind.Photo, remoteUrl = "https://example.com/2.jpg"),
            Artwork("p3", "Photo 3", sourceId = id, kind = ArtworkKind.Photo, remoteUrl = "https://example.com/3.jpg"),
            Artwork("p4", "Photo 4", sourceId = id, kind = ArtworkKind.Photo, remoteUrl = "https://example.com/4.jpg"),
            Artwork("p5", "Photo 5", sourceId = id, kind = ArtworkKind.Photo, remoteUrl = "https://example.com/5.jpg"),
            Artwork("p6", "Photo 6", sourceId = id, kind = ArtworkKind.Photo, remoteUrl = "https://example.com/6.jpg"),
            Artwork("perso", "Mine", sourceId = id, kind = ArtworkKind.PersonalPhoto, localPath = "/tmp/mine.jpg"),
            Artwork("custom", "Tap fractal", sourceId = id, kind = ArtworkKind.CustomFractal),
            Artwork("fp1", "Preset 1", sourceId = id, kind = ArtworkKind.FractalPreset),
            Artwork("fp2", "Preset 2", sourceId = id, kind = ArtworkKind.FractalPreset),
            Artwork("fp3", "Preset 3", sourceId = id, kind = ArtworkKind.FractalPreset),
            Artwork("fp4", "Preset 4", sourceId = id, kind = ArtworkKind.FractalPreset),
            Artwork(GenartSource.PARTICLES, "Genart 1", sourceId = id, kind = ArtworkKind.Genart),
            Artwork(GenartSource.PSEUDO3D, "Genart 2", sourceId = id, kind = ArtworkKind.Genart),
            Artwork(GenartSource.BLOBS, "Genart 3", sourceId = id, kind = ArtworkKind.Genart),
        )
    }

    @Test
    fun freeTier_capsPhotosFractals_andBlocksLockedPacks() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(richSource),
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxPhotoArtwork = 5, maxFractalPresets = 3),
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = listOf("rich"), artworkIds = emptyList()))
        assertEquals(5, catalog.count { it.kind == ArtworkKind.Photo })
        assertEquals(3, catalog.count { it.kind == ArtworkKind.FractalPreset })
        assertEquals(2, catalog.count { it.kind == ArtworkKind.Genart })
        assertTrue(catalog.any { it.id == GenartSource.PARTICLES })
        assertTrue(catalog.any { it.id == GenartSource.PSEUDO3D })
        assertFalse(catalog.any { it.id == GenartSource.BLOBS })
        assertFalse(catalog.any { it.kind == ArtworkKind.PersonalPhoto })
        assertFalse(catalog.any { it.kind == ArtworkKind.CustomFractal })
    }

    @Test
    fun owningPacks_unlocksPersonalCustomAndGenartTopics() = runBlocking {
        val ownership = FakePackOwnership(
            owned = setOf(
                MarketplaceCatalog.PERSONAL_PHOTOS_ID,
                MarketplaceCatalog.genartPackId(GenartPackTopics.CUSTOM),
                MarketplaceCatalog.genartPackId(GenartPackTopics.TAPET),
                MarketplaceCatalog.genartPackId(GenartPackTopics.FRACTAL),
            ),
        )
        val engine = ContentEngine(
            sources = listOf(richSource),
            packOwnership = ownership,
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = listOf("rich"), artworkIds = emptyList()))
        assertTrue(catalog.any { it.kind == ArtworkKind.PersonalPhoto })
        assertTrue(catalog.any { it.kind == ArtworkKind.CustomFractal })
        assertTrue(catalog.any { it.id == GenartSource.BLOBS })
        assertEquals(4, catalog.count { it.kind == ArtworkKind.FractalPreset })
        assertEquals(15, catalog.size)
    }

    @Test
    fun bundledPack_resolvesOrderedRotation() = runBlocking {
        val bundled = BundledPackSource()
        val engine = ContentEngine(
            sources = listOf(bundled),
            packOwnership = PackOwnership.NONE,
        )
        val prepared = PreparedRotation(
            sourceIds = listOf(BundledPackSource.ID),
            artworkIds = listOf("bundled-3", "bundled-1"),
        )
        val rotation = engine.resolve(prepared)
        assertEquals(listOf("bundled-3", "bundled-1"), rotation.artworkIds)
    }

    @Test
    fun genartSource_freeAllowlist_only() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(GenartSource()),
            packOwnership = PackOwnership.NONE,
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(GenartSource.ID), artworkIds = emptyList()),
        )
        assertEquals(MarketplaceCatalog.freeGenartEngineIds.size, catalog.size)
        assertTrue(catalog.all { it.id in MarketplaceCatalog.freeGenartEngineIds })
        assertTrue(catalog.all { it.kind == ArtworkKind.Genart })
    }

    @Test
    fun genartSource_owningAllTopicPacks_includesMappedEngines() = runBlocking {
        val ownership = FakePackOwnership().also { it.unlockAll() }
        val engine = ContentEngine(
            sources = listOf(GenartSource()),
            packOwnership = ownership,
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(GenartSource.ID), artworkIds = emptyList()),
        )
        val expected = GenartSource.defaultCatalog().map { it.id }.filter { id ->
            id in MarketplaceCatalog.freeGenartEngineIds ||
                GenartPackTopics.topicsCoveringEngine(id).isNotEmpty()
        }.toSet()
        assertEquals(expected, catalog.map { it.id }.toSet())
        assertTrue(catalog.all { it.kind == ArtworkKind.Genart })
    }

    @Test
    fun genartSource_owningTapet_includesAbstractTapetEngines() = runBlocking {
        val ownership = FakePackOwnership(
            owned = setOf(
                MarketplaceCatalog.genartPackId(GenartPackTopics.TAPET),
                MarketplaceCatalog.genartPackId(GenartPackTopics.ABSTRACT),
            ),
        )
        val engine = ContentEngine(
            sources = listOf(GenartSource()),
            packOwnership = ownership,
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(GenartSource.ID), artworkIds = emptyList()),
        )
        val ids = catalog.map { it.id }.toSet()
        assertTrue(ids.contains(GenartSource.BLOBS))
        assertTrue(ids.contains(GenartSource.NOISE_FIELD))
        assertTrue(ids.contains(GenartSource.VORONOI))
        assertTrue(ids.contains(GenartSource.SILK))
        assertTrue(ids.contains(GenartSource.GRADIENT_MESH))
        assertTrue(ids.contains(GenartSource.ARC_MOSAIC))
        assertTrue(ids.contains(GenartSource.BREATH_CIRCLES))
        assertTrue(ids.contains(GenartSource.RIBBONS))
    }

    @Test
    fun fractalSource_freeTier_capsPresets() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(FractalSource()),
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxFractalPresets = 3),
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(FractalSource.ID), artworkIds = emptyList()),
        )
        assertEquals(3, catalog.size)
        assertTrue(catalog.all { it.kind == ArtworkKind.FractalPreset })
    }

    @Test
    fun freeTier_keepsAllBundledSuggestionPhotos() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(BundledPackSource()),
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxPhotoArtwork = 2),
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(BundledPackSource.ID), artworkIds = emptyList()),
        )
        assertEquals(BundledPackSource.defaultPack().size, catalog.size)
        assertEquals(6, catalog.count { it.kind == ArtworkKind.Photo })
        assertEquals(6, catalog.count { it.kind == ArtworkKind.Painting })
        assertEquals(6, catalog.count { it.kind == ArtworkKind.Sculpture })
        assertTrue(catalog.any { it.id == "bundled-3" && it.title == "Harbor Grain" })
    }

    @Test
    fun freeTier_skipsStillsWithoutImages() = runBlocking {
        val source = object : Source {
            override val id = "mixed"
            override val displayName = "Mixed"
            override suspend fun load() = listOf(
                Artwork("empty", "Empty", sourceId = id, kind = ArtworkKind.Photo),
                Artwork(
                    "ok",
                    "OK",
                    sourceId = id,
                    kind = ArtworkKind.Photo,
                    remoteUrl = "https://example.com/a.jpg",
                ),
                Artwork(GenartSource.PARTICLES, "Genart", sourceId = id, kind = ArtworkKind.Genart),
            )
        }
        val engine = ContentEngine(
            sources = listOf(source),
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxPhotoArtwork = 5),
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = listOf("mixed"), artworkIds = emptyList()))
        assertEquals(setOf("ok", GenartSource.PARTICLES), catalog.map { it.id }.toSet())
        assertEquals(2, catalog.size)
    }

    @Test
    fun freeTier_roundRobinsStillsAcrossSources() = runBlocking {
        fun stills(sourceId: String, count: Int) = object : Source {
            override val id = sourceId
            override val displayName = sourceId
            override suspend fun load() = (1..count).map { i ->
                Artwork(
                    id = "$sourceId-$i",
                    title = "$sourceId $i",
                    sourceId = sourceId,
                    kind = ArtworkKind.Painting,
                    remoteUrl = "https://example.com/$sourceId-$i.jpg",
                )
            }
        }
        val engine = ContentEngine(
            sources = listOf(
                stills("artic", 10),
                stills("europeana", 10),
                stills("harvard", 10),
                stills("smithsonian", 10),
            ),
            packOwnership = PackOwnership.NONE,
            limits = FreeTierLimits(maxPhotoArtwork = 8),
        )
        val catalog = engine.catalog(
            PreparedRotation(
                sourceIds = listOf("artic", "europeana", "harvard", "smithsonian"),
                artworkIds = emptyList(),
            ),
        )
        assertEquals(8, catalog.size)
        val bySource = catalog.groupingBy { it.sourceId }.eachCount()
        assertEquals(2, bySource["artic"])
        assertEquals(2, bySource["europeana"])
        assertEquals(2, bySource["harvard"])
        assertEquals(2, bySource["smithsonian"])
    }

    @Test
    fun catalog_resilientToSourceLoadException() = runBlocking {
        val failingSource = object : Source {
            override val id = "failing"
            override val displayName = "Failing Source"
            override suspend fun load(): List<Artwork> {
                throw IllegalStateException("Network or API error")
            }
        }
        val goodSource = object : Source {
            override val id = "good"
            override val displayName = "Good Source"
            override suspend fun load() = listOf(
                Artwork(GenartSource.PARTICLES, "Good Art", sourceId = id, kind = ArtworkKind.Genart),
            )
        }
        val engine = ContentEngine(
            sources = listOf(failingSource, goodSource),
            packOwnership = PackOwnership.NONE,
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf("failing", "good"), artworkIds = emptyList()),
        )
        assertEquals(1, catalog.size)
        assertEquals(GenartSource.PARTICLES, catalog.first().id)
    }
}
