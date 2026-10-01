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
    fun museumDefault_metOnly_excludesBundled() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Museum))
        assertEquals(listOf("met-1"), pool.map { it.id })
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
    fun genartAll_includesAllGenartKindsInOrder() {
        val pool = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Genart, GenartTopic.All.testTagSuffix),
        )
        assertEquals(
            listOf(
                GenartSource.SNOW,
                GenartSource.GRASS,
                GenartSource.NEBULA,
                GenartSource.BLOBS,
                GenartSource.GRADIENT_MESH,
            ),
            pool.map { it.id },
        )
    }

    @Test
    fun genartRandom_shufflesListAtCreation() {
        // Create a catalog with enough items to ensure shuffled list differs from input catalog order.
        val testCatalog = (1..30).map { i ->
            Artwork(
                id = "genart-$i",
                title = "Genart $i",
                sourceId = GenartSource.ID,
                kind = ArtworkKind.Genart,
            )
        }
        val pool = resolvePackPool(testCatalog, PackSelection(PackFamily.Genart, GenartTopic.Random.testTagSuffix))
        assertEquals(30, pool.size)
        // Set of elements is identical, but order is shuffled at creation time.
        assertEquals(testCatalog.map { it.id }.toSet(), pool.map { it.id }.toSet())
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
    fun genartSubPacks_includeAllAbstractAndTapet() {
        val suffixes = PackFamily.Genart.subPackTiles().map { it.testTagSuffix }
        assertEquals("genart_all", suffixes.first())
        assertTrue(suffixes.contains("genart_abstract"))
        assertTrue(suffixes.contains("genart_tapet"))
    }

    @Test
    fun genartItemCounts_computedWhenCatalogProvided() {
        val homeTile = PackFamily.Genart.homeTile(catalog)
        assertEquals(5, homeTile.itemCount)

        val subTiles = PackFamily.Genart.subPackTiles(catalog)
        val tapetTile = subTiles.first { it.testTagSuffix == "genart_tapet" }
        assertEquals(2, tapetTile.itemCount)
        val weatherTile = subTiles.first { it.testTagSuffix == "genart_weather" }
        assertEquals(1, weatherTile.itemCount)
    }

    @Test
    fun photoRandom_includesPhotos() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Photo))
        assertEquals(listOf("pexels-1"), pool.map { it.id })
    }

    @Test
    fun museumRandom_includesAllMuseumSources_excludesBundled() {
        val louvre = Artwork(
            id = "louvre-1",
            title = "Louvre Painting",
            sourceId = fr.geoking.arthur.shared.source.LouvreSource.ID,
            kind = ArtworkKind.Painting,
        )
        val pool = resolvePackPool(
            catalog + louvre,
            PackSelection(PackFamily.Museum, MuseumTopic.Random.testTagSuffix),
        )
        assertEquals(
            setOf("met-1", "rijks-sculpt", "louvre-1"),
            pool.map { it.id }.toSet(),
        )
        assertFalse(pool.any { it.sourceId == BundledPackSource.ID })
    }

    @Test
    fun subPackTiles_museumListsRandomThenEveryInstitution_noWikimedia() {
        val tiles = PackFamily.Museum.subPackTiles()
        assertEquals("museum_random", tiles.first().testTagSuffix)
        val institutionSuffixes = MuseumTopic.institutions.map { "museum_${it.testTagSuffix}" }
        assertEquals(institutionSuffixes, tiles.drop(1).map { it.testTagSuffix })
        assertEquals(1 + MuseumTopic.institutions.size, tiles.size)
        assertTrue(tiles.any { it.testTagSuffix == "museum_europeana" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_harvard" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_smithsonian" })
        assertTrue(tiles.any { it.testTagSuffix == "museum_louvre" })
        assertFalse(tiles.any { it.testTagSuffix == "museum_wikimedia-streetart" })
        assertFalse(tiles.any { it.testTagSuffix.contains("suggestions") })
    }

    @Test
    fun paintingSubPacks_randomThenInstitutionsWithoutLouvre() {
        val suffixes = PackFamily.Painting.subPackTiles().map { it.testTagSuffix }
        assertEquals("painting_random", suffixes.first())
        assertFalse(suffixes.contains("painting_all"))
        assertFalse(suffixes.any { it.contains("suggestions") })
        assertTrue(suffixes.contains("painting_europeana"))
        assertFalse(suffixes.contains("painting_louvre"))
        assertFalse(suffixes.contains("painting_wikimedia-streetart"))
        assertEquals(1 + MuseumTopic.paintingSculptureInstitutions.size, suffixes.size)
    }

    @Test
    fun photoSubPacks_includeProviderSourcesBeforeStockTopics_noMuseums() {
        val tiles = PackFamily.Photo.subPackTiles()
        val providerCount = PhotoTopic.entries.size
        val expectedProviderSuffixes = PhotoTopic.entries.map { "photo_${it.testTagSuffix}" }
        assertEquals(expectedProviderSuffixes, tiles.take(providerCount).map { it.testTagSuffix })
        assertTrue(tiles.any { it.testTagSuffix == "photo_wikimedia-streetart" })

        val stockTiles = tiles.drop(providerCount)
        assertEquals(StockPhotoCategory.entries.size, stockTiles.size)
        assertEquals("photo_random", stockTiles.first().testTagSuffix)
        assertFalse(tiles.any { it.testTagSuffix.startsWith("photo_museum_") })
    }

    @Test
    fun photoProviderPick_searchesOnlyThatProvider() {
        val selection = PackSelection(PackFamily.Photo, PhotoTopic.Pexels.testTagSuffix)
        assertEquals(PhotoTopic.Pexels, selection.photoSourceOrNull())
        assertEquals(null, selection.stockCategoryOrNull())
        assertEquals(listOf(PexelsSource.ID), selection.sourceIdsForAmbientLoad())

        val photoCatalog = listOf(
            Artwork(
                id = "pexels-photo",
                title = "Pexels Photo",
                sourceId = PexelsSource.ID,
                kind = ArtworkKind.Photo,
            ),
            Artwork(
                id = "unsplash-photo",
                title = "Unsplash Photo",
                sourceId = UnsplashSource.ID,
                kind = ArtworkKind.Photo,
            ),
        )
        val pool = resolvePackPool(photoCatalog, selection)
        assertEquals(listOf("pexels-photo"), pool.map { it.id })
    }

    @Test
    fun photoMuseumPick_noLongerResolvedAsMuseumSource() {
        // Met id under Photo is not a PhotoTopic — treated as stock query "met" → Random.
        val selection = PackSelection(PackFamily.Photo, MuseumTopic.Met.testTagSuffix)
        assertEquals(null, selection.museumTopicOrNull())
        assertEquals(null, selection.photoSourceOrNull())
        assertEquals(
            fr.geoking.arthur.shared.source.SourceCapabilities.sourceIdsForPhotoProviders(),
            selection.sourceIdsForAmbientLoad(),
        )
    }

    @Test
    fun isGenartCustom() {
        assertTrue(
            PackSelection(PackFamily.Genart, GenartTopic.Custom.testTagSuffix).isGenartCustom(),
        )
        assertFalse(PackSelection(PackFamily.Genart).isGenartCustom())
    }

    @Test
    fun sourceIdsForAmbientLoad_museumRandom_allInstitutionsIncludingLouvre() {
        val ids = PackSelection(
            PackFamily.Museum,
            MuseumTopic.Random.testTagSuffix,
        ).sourceIdsForAmbientLoad()!!
        assertEquals(MuseumTopic.institutions.mapNotNull { it.sourceId }.toSet(), ids.toSet())
        assertTrue(fr.geoking.arthur.shared.source.LouvreSource.ID in ids)
        assertFalse(BundledPackSource.ID in ids)
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
    fun sourceIdsForAmbientLoad_museumNull_defaultsToMet() {
        val ids = PackSelection(PackFamily.Museum).sourceIdsForAmbientLoad()
        assertEquals(listOf(MetSource.ID), ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_everyMuseumInstitution() {
        for (topic in MuseumTopic.institutions) {
            val ids = PackSelection(
                PackFamily.Museum,
                topic.testTagSuffix,
            ).sourceIdsForAmbientLoad()
            assertEquals(listOf(topic.sourceId), ids)
        }
    }

    @Test
    fun sourceIdsForAmbientLoad_photoRandom_photoProvidersOnly_excludesMuseumsAndBundled() {
        val ids = PackSelection(PackFamily.Photo).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(PexelsSource.ID in ids)
        assertTrue(UnsplashSource.ID in ids)
        assertTrue(WikimediaStreetArtSource.ID in ids)
        assertFalse(RijksmuseumSource.ID in ids)
        assertFalse(ClevelandSource.ID in ids)
        assertFalse(MetSource.ID in ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_photoNature_photoProvidersOnly() {
        val ids = PackSelection(
            PackFamily.Photo,
            StockPhotoCategory.Nature.query,
        ).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(PexelsSource.ID in ids)
        assertTrue(UnsplashSource.ID in ids)
        assertTrue(WikimediaStreetArtSource.ID in ids)
        assertFalse(RijksmuseumSource.ID in ids)
        assertFalse(ClevelandSource.ID in ids)
        assertFalse(MetSource.ID in ids)
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
    fun sourceIdsForAmbientLoad_everyPhotoProvider() {
        for (topic in PhotoTopic.entries) {
            val ids = PackSelection(
                PackFamily.Photo,
                topic.testTagSuffix,
            ).sourceIdsForAmbientLoad()
            assertEquals(listOf(topic.sourceId), ids)
        }
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
    fun sourceIdsForAmbientLoad_paintingRandom_excludesLouvreAndBundled() {
        val ids = PackSelection(PackFamily.Painting).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(MetSource.ID in ids)
        assertTrue(RijksmuseumSource.ID in ids)
        assertFalse(fr.geoking.arthur.shared.source.LouvreSource.ID in ids)
        assertFalse(PexelsSource.ID in ids)
    }

    @Test
    fun sourceIdsForAmbientLoad_sculptureRandom_excludesLouvrePhotoOnlyStreetArtAndBundled() {
        val ids = PackSelection(PackFamily.Sculpture).sourceIdsForAmbientLoad()!!
        assertFalse(BundledPackSource.ID in ids)
        assertTrue(MetSource.ID in ids)
        assertFalse(PexelsSource.ID in ids)
        assertFalse(WikimediaStreetArtSource.ID in ids)
        assertFalse(fr.geoking.arthur.shared.source.LouvreSource.ID in ids)
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
        assertFalse(PackSelection(PackFamily.Personal).allowsGenerativeAmbientFallback())
    }
}
