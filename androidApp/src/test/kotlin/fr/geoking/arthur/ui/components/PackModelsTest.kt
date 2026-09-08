package fr.geoking.arthur.ui.components

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.PexelsVideoSource
import fr.geoking.arthur.shared.source.PixabayVideoSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackModelsTest {

    private val catalog = listOf(
        Artwork(
            id = "met-1",
            title = "Met Painting",
            sourceId = MetSource.ID,
            kind = ArtworkKind.Painting,
        ),
        Artwork(
            id = "rijks-sculpt",
            title = "Rijks Sculpture",
            sourceId = RijksmuseumSource.ID,
            kind = ArtworkKind.Sculpture,
        ),
        Artwork(
            id = "bundled-paint",
            title = "Study",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Painting,
        ),
        Artwork(
            id = "pexels-1",
            title = "Nature",
            sourceId = PexelsSource.ID,
            kind = ArtworkKind.Photo,
        ),
        Artwork(
            id = GenartSource.SNOW,
            title = "Snow",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        ),
        Artwork(
            id = GenartSource.GRASS,
            title = "Grass",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        ),
        Artwork(
            id = GenartSource.NEBULA,
            title = "Nebula",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        ),
        Artwork(
            id = GenartSource.BLOBS,
            title = "Morphing Blobs",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        ),
        Artwork(
            id = GenartSource.GRADIENT_MESH,
            title = "Gradient Mesh",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        ),
    )

    @Test
    fun museumRandom_includesMuseumSources_excludesBundled() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Museum))
        assertEquals(setOf("met-1", "rijks-sculpt"), pool.map { it.id }.toSet())
        assertFalse(pool.any { it.sourceId == BundledPackSource.ID })
    }

    @Test
    fun museumMet_onlyMet() {
        val pool = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Museum, MuseumTopic.Met.testTagSuffix),
        )
        assertEquals(listOf("met-1"), pool.map { it.id })
    }

    @Test
    fun paintingRandom_includesMet_excludesBundled() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Painting))
        assertEquals(listOf("met-1"), pool.map { it.id })
    }

    @Test
    fun genartWeather_vsNature() {
        val weather = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Genart, GenartTopic.Weather.testTagSuffix),
        )
        val nature = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Genart, GenartTopic.Nature.testTagSuffix),
        )
        assertEquals(listOf(GenartSource.SNOW), weather.map { it.id })
        assertEquals(listOf(GenartSource.GRASS), nature.map { it.id })
    }

    @Test
    fun genartRandom_includesAllGenartKinds() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Genart))
        assertEquals(
            setOf(
                GenartSource.SNOW,
                GenartSource.GRASS,
                GenartSource.NEBULA,
                GenartSource.BLOBS,
                GenartSource.GRADIENT_MESH,
            ),
            pool.map { it.id }.toSet(),
        )
    }

    @Test
    fun genartAbstract_includesTapetEngines() {
        val pool = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Genart, GenartTopic.Abstract.testTagSuffix),
        )
        assertEquals(
            listOf(GenartSource.BLOBS, GenartSource.GRADIENT_MESH),
            pool.map { it.id },
        )
    }

    @Test
    fun genartSubPacks_includeAbstract() {
        val suffixes = PackFamily.Genart.subPackTiles().map { it.testTagSuffix }
        assertTrue(suffixes.contains("genart_abstract"))
    }

    @Test
    fun photoRandom_includesPhotos() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Photo))
        assertEquals(listOf("pexels-1"), pool.map { it.id })
    }

    @Test
    fun subPackTiles_museumListsRandomThenEveryInstitutionSource() {
        val tiles = PackFamily.Museum.subPackTiles()
        assertEquals("museum_random", tiles.first().testTagSuffix)
        val institutionSuffixes = MuseumTopic.entries
            .filter { it.sourceId != null }
            .map { "museum_${it.testTagSuffix}" }
        assertEquals(institutionSuffixes, tiles.drop(1).map { it.testTagSuffix })
        assertTrue(tiles.any { it.testTagSuffix == "museum_europeana" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_harvard" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_smithsonian" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_louvre" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_wikimedia-streetart" })
        assertFalse(tiles.any { it.testTagSuffix.contains("suggestions") })
    }

    @Test
    fun paintingSubPacks_noAllOrSuggestions_oneTilePerMuseumTopic() {
        val suffixes = PackFamily.Painting.subPackTiles().map { it.testTagSuffix }
        assertEquals("painting_random", suffixes.first())
        assertFalse(suffixes.contains("painting_all"))
        assertFalse(suffixes.any { it.contains("suggestions") })
        assertTrue(suffixes.contains("painting_europeana"))
        assertEquals(MuseumTopic.entries.size, suffixes.size)
    }

    @Test
    fun photoSubPacks_includeMuseumInstitutionsAfterStockTopics() {
        val tiles = PackFamily.Photo.subPackTiles()
        val stockCount = StockPhotoCategory.entries.size
        assertEquals("photo_random", tiles.first().testTagSuffix)
        val museumTiles = tiles.drop(stockCount)
        val expectedMuseumSuffixes = MuseumTopic.entries
            .filter { it.sourceId != null }
            .map { "photo_museum_${it.testTagSuffix}" }
        assertEquals(expectedMuseumSuffixes, museumTiles.map { it.testTagSuffix })
    }

    @Test
    fun photoMuseumPick_searchesOnlyThatMuseum() {
        val selection = PackSelection(PackFamily.Photo, MuseumTopic.Met.testTagSuffix)
        assertEquals(MuseumTopic.Met, selection.museumTopicOrNull())
        assertEquals(null, selection.stockCategoryOrNull())
        assertEquals(listOf(MetSource.ID), selection.sourceIdsForAmbientLoad())

        val photoCatalog = listOf(
            Artwork(
                id = "met-photo",
                title = "Met Photo",
                sourceId = MetSource.ID,
                kind = ArtworkKind.Photo,
            ),
            Artwork(
                id = "pexels-1",
                title = "Nature",
                sourceId = PexelsSource.ID,
                kind = ArtworkKind.Photo,
            ),
        )
        val pool = resolvePackPool(photoCatalog, selection)
        assertEquals(listOf("met-photo"), pool.map { it.id })
    }

    @Test
    fun isGenartCustom() {
        assertTrue(
            PackSelection(PackFamily.Genart, GenartTopic.Custom.testTagSuffix).isGenartCustom(),
        )
        assertFalse(PackSelection(PackFamily.Genart).isGenartCustom())
    }

    @Test
    fun sourceIdsForAmbientLoad_metOnly() {
        val ids = PackSelection(
            PackFamily.Museum,
            MuseumTopic.Met.testTagSuffix,
        ).sourceIdsForAmbientLoad()
        assertEquals(listOf(MetSource.ID), ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_museumAll_allInstitutions() {
        val ids = PackSelection(PackFamily.Museum).sourceIdsForAmbientLoad()
        assertTrue(ids!!.contains(MetSource.ID))
        assertTrue(ids.contains(RijksmuseumSource.ID))
        assertFalse(ids.contains(BundledPackSource.ID))
    }

    @Test
    fun sourceIdsForAmbientLoad_everyMuseumInstitution() {
        for (topic in MuseumTopic.entries.filter { it.sourceId != null }) {
            val ids = PackSelection(
                PackFamily.Museum,
                topic.testTagSuffix,
            ).sourceIdsForAmbientLoad()
            assertEquals(listOf(topic.sourceId), ids)
        }
    }

    @Test
    fun sourceIdsForAmbientLoad_photoRandom_includesStockAndMuseumPhotoSearch_excludesBundled() {
        val ids = PackSelection(PackFamily.Photo).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(PexelsSource.ID in ids)
        assertTrue(UnsplashSource.ID in ids)
        assertTrue(RijksmuseumSource.ID in ids)
        assertTrue(ClevelandSource.ID in ids)
        assertTrue(MetSource.ID in ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_photoNature_remotePhotoSearchOnly() {
        val ids = PackSelection(
            PackFamily.Photo,
            StockPhotoCategory.Nature.query,
        ).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(PexelsSource.ID in ids)
        assertTrue(UnsplashSource.ID in ids)
        assertTrue(RijksmuseumSource.ID in ids)
        assertTrue(ClevelandSource.ID in ids)
        assertTrue(MetSource.ID in ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_videoRandom_threeVideoApis() {
        val ids = PackSelection(PackFamily.Video).sourceIdsForAmbientLoad()!!
        assertEquals(
            setOf(PexelsVideoSource.ID, UnsplashSource.ID, PixabayVideoSource.ID, CoverrSource.ID),
            ids.toSet(),
        )
    }

    @Test
    fun sourceIdsForAmbientLoad_videoNature_threeVideoApis() {
        val ids = PackSelection(
            PackFamily.Video,
            StockPhotoCategory.Nature.query,
        ).sourceIdsForAmbientLoad()!!
        assertEquals(
            setOf(PexelsVideoSource.ID, UnsplashSource.ID, PixabayVideoSource.ID, CoverrSource.ID),
            ids.toSet(),
        )
    }

    @Test
    fun sourceIdsForAmbientLoad_videoPexels_onlyThatSource() {
        val ids = PackSelection(
            PackFamily.Video,
            VideoTopic.Pexels.testTagSuffix,
        ).sourceIdsForAmbientLoad()
        assertEquals(listOf(PexelsVideoSource.ID), ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_everyVideoSource() {
        for (topic in VideoTopic.entries) {
            val ids = PackSelection(
                PackFamily.Video,
                topic.testTagSuffix,
            ).sourceIdsForAmbientLoad()
            assertEquals(listOf(topic.sourceId), ids)
        }
    }

    @Test
    fun videoSubPacks_listSourcesThenKeywords_excludeSuggestions() {
        val suffixes = PackFamily.Video.subPackTiles().map { it.testTagSuffix }
        val sourceSuffixes = VideoTopic.entries.map { "video_${it.testTagSuffix}" }
        assertEquals(sourceSuffixes, suffixes.take(VideoTopic.entries.size))
        assertTrue("video_random" in suffixes)
        assertTrue("video_nature" in suffixes)
        assertFalse(suffixes.any { it.contains("suggestions") })
        assertFalse(suffixes.any { it.contains("_all") })
    }

    @Test
    fun videoPexels_onlyPexelsInPool() {
        val catalog = listOf(
            Artwork(
                id = "pv-1",
                title = "Pexels clip",
                sourceId = PexelsVideoSource.ID,
                kind = ArtworkKind.Video,
            ),
            Artwork(
                id = "px-1",
                title = "Pixabay clip",
                sourceId = PixabayVideoSource.ID,
                kind = ArtworkKind.Video,
            ),
            Artwork(
                id = "cv-1",
                title = "Coverr clip",
                sourceId = CoverrSource.ID,
                kind = ArtworkKind.Video,
            ),
        )
        val pool = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Video, VideoTopic.Pexels.testTagSuffix),
        )
        assertEquals(listOf("pv-1"), pool.map { it.id })
    }

    @Test
    fun videoNature_allVideoSourcesInPool() {
        val catalog = listOf(
            Artwork(
                id = "pv-1",
                title = "Pexels clip",
                sourceId = PexelsVideoSource.ID,
                kind = ArtworkKind.Video,
            ),
            Artwork(
                id = "px-1",
                title = "Pixabay clip",
                sourceId = PixabayVideoSource.ID,
                kind = ArtworkKind.Video,
            ),
            Artwork(
                id = "photo-1",
                title = "Photo",
                sourceId = PexelsSource.ID,
                kind = ArtworkKind.Photo,
            ),
        )
        val pool = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Video, StockPhotoCategory.Nature.query),
        )
        assertEquals(listOf("pv-1", "px-1"), pool.map { it.id })
    }

    @Test
    fun sourceIdsForAmbientLoad_paintingRandom_capabilityTaggedSources_excludesBundled() {
        val ids = PackSelection(PackFamily.Painting).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(MetSource.ID in ids)
        assertTrue(RijksmuseumSource.ID in ids)
        assertFalse(PexelsSource.ID in ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_sculptureRandom_excludesPhotoOnlyAndStreetArtAndBundled() {
        val ids = PackSelection(PackFamily.Sculpture).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(MetSource.ID in ids)
        assertFalse(PexelsSource.ID in ids)
        assertFalse(WikimediaStreetArtSource.ID in ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_genart_usesInMemoryCatalog() {
        assertEquals(null, PackSelection(PackFamily.Genart).sourceIdsForAmbientLoad())
        assertEquals(
            null,
            PackSelection(
                PackFamily.Genart,
                GenartTopic.Fractal.testTagSuffix,
            ).sourceIdsForAmbientLoad(),
        )
        assertEquals(
            null,
            PackSelection(
                PackFamily.Genart,
                GenartTopic.Abstract.testTagSuffix,
            ).sourceIdsForAmbientLoad(),
        )
    }

    @Test
    fun allowsGenerativeAmbientFallback_onlyGenart() {
        assertTrue(PackSelection(PackFamily.Genart).allowsGenerativeAmbientFallback())
        assertFalse(PackSelection(PackFamily.Museum).allowsGenerativeAmbientFallback())
        assertFalse(
            PackSelection(PackFamily.Museum, MuseumTopic.Met.testTagSuffix)
                .allowsGenerativeAmbientFallback(),
        )
        assertFalse(PackSelection(PackFamily.Photo).allowsGenerativeAmbientFallback())
        assertFalse(PackSelection(PackFamily.Video).allowsGenerativeAmbientFallback())
        assertFalse(PackSelection(PackFamily.Painting).allowsGenerativeAmbientFallback())
        assertFalse(PackSelection(PackFamily.Sculpture).allowsGenerativeAmbientFallback())
    }
}
