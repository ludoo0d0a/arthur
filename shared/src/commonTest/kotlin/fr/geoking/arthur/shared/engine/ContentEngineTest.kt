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
            Artwork("p1", "Photo 1", sourceId = id, kind = ArtworkKind.Photo),
            Artwork("p2", "Photo 2", sourceId = id, kind = ArtworkKind.Photo),
            Artwork("p3", "Photo 3", sourceId = id, kind = ArtworkKind.Photo),
            Artwork("p4", "Photo 4", sourceId = id, kind = ArtworkKind.Photo),
            Artwork("p5", "Photo 5", sourceId = id, kind = ArtworkKind.Photo),
            Artwork("p6", "Photo 6", sourceId = id, kind = ArtworkKind.Photo),
            Artwork("perso", "Mine", sourceId = id, kind = ArtworkKind.PersonalPhoto),
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
}
