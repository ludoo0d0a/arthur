package fr.geoking.arthur.shared.engine

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FakePremiumEntitlement
import fr.geoking.arthur.shared.domain.FreeTierLimits
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.Source
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
            Artwork("g1", "Genart 1", sourceId = id, kind = ArtworkKind.Genart),
            Artwork("g2", "Genart 2", sourceId = id, kind = ArtworkKind.Genart),
            Artwork("g3", "Genart 3", sourceId = id, kind = ArtworkKind.Genart),
        )
    }

    @Test
    fun freeTier_capsPhotosFractalsGenart_andBlocksPremiumKinds() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(richSource),
            entitlement = FakePremiumEntitlement(isPremium = false),
            limits = FreeTierLimits(maxPhotoArtwork = 5, maxFractalPresets = 3, maxGenart = 2),
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = listOf("rich"), artworkIds = emptyList()))
        assertEquals(5, catalog.count { it.kind == ArtworkKind.Photo })
        assertEquals(3, catalog.count { it.kind == ArtworkKind.FractalPreset })
        assertEquals(2, catalog.count { it.kind == ArtworkKind.Genart })
        assertFalse(catalog.any { it.kind == ArtworkKind.PersonalPhoto })
        assertFalse(catalog.any { it.kind == ArtworkKind.CustomFractal })
    }

    @Test
    fun premium_unlocksAllKinds() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(richSource),
            entitlement = FakePremiumEntitlement(isPremium = true),
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = listOf("rich"), artworkIds = emptyList()))
        assertTrue(catalog.any { it.kind == ArtworkKind.PersonalPhoto })
        assertTrue(catalog.any { it.kind == ArtworkKind.CustomFractal })
        assertEquals(15, catalog.size)
    }

    @Test
    fun bundledPack_resolvesOrderedRotation() = runBlocking {
        val bundled = BundledPackSource()
        val engine = ContentEngine(
            sources = listOf(bundled),
            entitlement = FakePremiumEntitlement(isPremium = false),
        )
        val prepared = PreparedRotation(
            sourceIds = listOf(BundledPackSource.ID),
            artworkIds = listOf("bundled-3", "bundled-1"),
        )
        val rotation = engine.resolve(prepared)
        assertEquals(listOf("bundled-3", "bundled-1"), rotation.artworkIds)
    }

    @Test
    fun genartSource_freeTier_capsAtTwo() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(GenartSource()),
            entitlement = FakePremiumEntitlement(isPremium = false),
            limits = FreeTierLimits(maxGenart = 2),
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(GenartSource.ID), artworkIds = emptyList()),
        )
        assertEquals(2, catalog.size)
        assertTrue(catalog.all { it.kind == ArtworkKind.Genart })
        assertEquals(
            listOf(GenartSource.PARTICLES, GenartSource.PSEUDO3D),
            catalog.map { it.id },
        )
    }

    @Test
    fun genartSource_premium_includesAllEngines() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(GenartSource()),
            entitlement = FakePremiumEntitlement(isPremium = true),
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(GenartSource.ID), artworkIds = emptyList()),
        )
        assertEquals(GenartSource.defaultCatalog().size, catalog.size)
        assertTrue(catalog.all { it.kind == ArtworkKind.Genart })
    }

    @Test
    fun fractalSource_freeTier_capsPresets() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(FractalSource()),
            entitlement = FakePremiumEntitlement(isPremium = false),
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
            entitlement = FakePremiumEntitlement(isPremium = false),
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
                Artwork("g1", "Genart", sourceId = id, kind = ArtworkKind.Genart),
            )
        }
        val engine = ContentEngine(
            sources = listOf(source),
            entitlement = FakePremiumEntitlement(isPremium = false),
            limits = FreeTierLimits(maxPhotoArtwork = 5, maxGenart = 2),
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = listOf("mixed"), artworkIds = emptyList()))
        assertEquals(listOf("ok", "g1"), catalog.map { it.id })
    }
}
