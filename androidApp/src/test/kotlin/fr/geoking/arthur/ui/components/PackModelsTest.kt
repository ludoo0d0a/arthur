package fr.geoking.arthur.ui.components

import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.ArtUniverse
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.FakePackOwnership
import fr.geoking.arthur.shared.marketplace.PackOwnership
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
import org.junit.Assert.assertNull
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
    fun museumDefault_random_includesMuseumSources_excludesBundled() {
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
    fun genartAbstract_includesWallpaperStyleEngines() {
        val abstractPool = resolvePackPool(
            catalog,
            PackSelection(PackFamily.Genart, GenartTopic.Abstract.testTagSuffix),
        )
        assertEquals(
            listOf(GenartSource.BLOBS, GenartSource.GRADIENT_MESH),
            abstractPool.map { it.id },
        )
    }

    @Test
    fun genartSubPacks_includeAbstractAndVintage() {
        val suffixes = PackFamily.Genart.subPackTiles().map { it.testTagSuffix }
        assertEquals("genart_all", suffixes.first())
        assertTrue(suffixes.contains("genart_abstract"))
        assertFalse(suffixes.contains("genart_tapet"))
        assertTrue(suffixes.contains("genart_vintage"))
        assertTrue(suffixes.contains("genart_animals"))
        assertTrue(suffixes.contains("genart_storm_strobe"))
        assertTrue(suffixes.contains("genart_physarum"))
        assertTrue(suffixes.contains("genart_reactions"))
        val animalsIdx = suffixes.indexOf("genart_animals")
        val fractalIdx = suffixes.indexOf("genart_fractal")
        assertTrue(animalsIdx > suffixes.indexOf("genart_illusions"))
        assertTrue(animalsIdx < fractalIdx)
        assertEquals("genart_custom", suffixes.last())
    }

    @Test
    fun genartMonetizedTopic_isPackLockedWithoutOwnership() {
        val nature = PackSelection(PackFamily.Genart, GenartTopic.Nature.testTagSuffix)
        assertEquals("genart_nature", nature.sellablePackIdOrNull())
        assertTrue(nature.isPackLocked(PackOwnership.NONE))
        val owned = FakePackOwnership(setOf("genart_nature"))
        assertFalse(nature.isPackLocked(owned))
        assertNull(
            PackSelection(PackFamily.Genart, GenartTopic.All.testTagSuffix).sellablePackIdOrNull(),
        )
    }

    @Test
    fun genartVintage_sellablePackAndHiddenFromHome() {
        val vintage = PackSelection(PackFamily.Genart, GenartTopic.Vintage.testTagSuffix)
        assertEquals("genart_vintage", vintage.sellablePackIdOrNull())
        assertTrue(vintage.isPackLocked(PackOwnership.NONE))
        assertFalse(visibleHomePackFamilies().contains(PackFamily.VintageGames))
        assertEquals(R.drawable.pack_vintage_games, PackCovers.genart(GenartTopic.Vintage))
    }

    @Test
    fun artUniverse_mapsFamilies() {
        assertEquals(ArtUniverse.Gallery, PackFamily.Museum.artUniverse())
        assertEquals(ArtUniverse.Gallery, PackFamily.Photo.artUniverse())
        assertEquals(ArtUniverse.Gallery, PackFamily.Video.artUniverse())
        assertEquals(ArtUniverse.Generative, PackFamily.Genart.artUniverse())
        assertNull(PackFamily.Sound.artUniverse())
    }

    @Test
    fun visibleHomePackFamilies_filtersByUniverse() {
        val gallery = visibleHomePackFamilies(ArtUniverse.Gallery)
        assertTrue(gallery.contains(PackFamily.Museum))
        assertTrue(gallery.contains(PackFamily.Photo))
        assertFalse(gallery.contains(PackFamily.Genart))

        val generative = visibleHomePackFamilies(ArtUniverse.Generative)
        assertTrue(generative.contains(PackFamily.Genart))
        assertFalse(generative.contains(PackFamily.Museum))
        assertFalse(generative.contains(PackFamily.Photo))
    }

    @Test
    fun defaultPackSelection_perUniverse() {
        assertEquals(PackFamily.Museum, ArtUniverse.Gallery.defaultPackSelection().family)
        assertEquals(PackFamily.Genart, ArtUniverse.Generative.defaultPackSelection().family)
    }

    @Test
    fun soundSubPacks_includeEssentialsAndMonetizedSkus() {
        val tiles = PackFamily.Sound.subPackTiles()
        assertEquals("sound_essentials", tiles.first().testTagSuffix)
        assertEquals(null, tiles.first().sellablePackId)
        assertTrue(tiles.any { it.testTagSuffix == "sound_hearth_weather" })
        assertTrue(tiles.any { it.testTagSuffix == "sound_solo_violin" })
        assertTrue(tiles.any { it.testTagSuffix == "sound_arcade_chips" })
        assertFalse(tiles.any { it.testTagSuffix == "sound_downloaded_music" })
        val hearth = tiles.first { it.testTagSuffix == "sound_hearth_weather" }
        assertEquals("audio_hearth_weather", hearth.sellablePackId)
        assertEquals(R.drawable.pack_audio_hearth_weather, hearth.coverRes)
        val chips = tiles.first { it.testTagSuffix == "sound_arcade_chips" }
        assertEquals("audio_arcade_chips", chips.sellablePackId)
    }

    @Test
    fun downloadedMusic_isRootFamilyWithoutSubPacks() {
        assertTrue(PackFamily.DownloadedMusic.subPackTiles().isEmpty())
        assertEquals(
            AudioPackTopic.DownloadedMusic.testTagSuffix,
            PackFamily.DownloadedMusic.defaultSubId(),
        )
        val selection = PackSelection(
            PackFamily.DownloadedMusic,
            PackFamily.DownloadedMusic.defaultSubId(),
        )
        // FeatureFlag.musicEnabled=false gates Sound Player / Neural routes.
        assertFalse(selection.opensSoundPlayer())
        assertFalse(selection.opensNeuralMusic())
        assertEquals(AudioPackTopic.DownloadedMusic, selection.audioPackTopicOrNull())
        assertEquals(
            AudioPackTopic.DownloadedMusic.sellablePackId,
            PackFamily.DownloadedMusic.homeTile().sellablePackId,
        )
        assertFalse(visibleHomePackFamilies().contains(PackFamily.DownloadedMusic))
        // FeatureFlag.personalPhotosEnabled=false: Personal Photos home tile hidden.
        assertFalse(visibleHomePackFamilies().contains(PackFamily.Personal))
    }

    @Test
    fun genartSubPacks_includeVintageGamesTopic() {
        val tiles = PackFamily.Genart.subPackTiles()
        assertTrue(tiles.any { it.testTagSuffix == "genart_vintage" })
        val vintage = tiles.first { it.testTagSuffix == "genart_vintage" }
        assertEquals("genart_vintage", vintage.sellablePackId)
        assertEquals(R.drawable.pack_vintage_games, vintage.coverRes)
        // Arcade chips only when music feature is on.
        assertFalse(
            PackSelection(PackFamily.Genart, GenartTopic.Vintage.testTagSuffix)
                .appliesArcadeChipsOnAmbientStart(),
        )
    }

    @Test
    fun soundPool_isEmpty() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Sound, "essentials"))
        assertTrue(pool.isEmpty())
    }

    @Test
    fun neuralSubPacks_includeSixNeuralEnginePacks() {
        val tiles = PackFamily.Neural.subPackTiles()
        assertEquals(6, tiles.size)
        assertEquals(
            listOf(
                "neural_atmospheres_nature",
                "neural_museums_classical",
                "neural_genart_digital",
                "neural_stem",
                "neural_wavetable",
                "neural_executorch",
            ),
            tiles.map { it.testTagSuffix },
        )
        assertEquals(R.string.audio_pack_atmospheres_nature, tiles[0].titleRes)
        assertEquals(R.string.audio_pack_museums_classical, tiles[1].titleRes)
        assertEquals(R.string.audio_pack_genart_digital, tiles[2].titleRes)
        // FeatureFlag.musicEnabled=false: neural / sound player routes stay closed.
        assertFalse(PackSelection(PackFamily.Neural, "atmospheres_nature").opensNeuralMusic())
        assertFalse(PackSelection(PackFamily.Neural, "museums_classical").opensNeuralMusic())
        assertFalse(PackSelection(PackFamily.Neural, "genart_digital").opensNeuralMusic())
        assertFalse(PackSelection(PackFamily.Neural, "executorch").opensNeuralMusic())
        assertFalse(PackSelection(PackFamily.Neural, "atmospheres_nature").opensSoundPlayer())
        assertFalse(PackSelection(PackFamily.Neural, "stem").opensSoundPlayer())
        assertFalse(PackSelection(PackFamily.Neural, "wavetable").opensSoundPlayer())
        assertFalse(PackSelection(PackFamily.Neural, "stem").opensNeuralMusic())
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Neural, "stem"))
        assertTrue(pool.isEmpty())
        assertFalse(visibleHomePackFamilies().contains(PackFamily.Neural))
        // Dedicated neural engines and downloaded music are not listed under Procedural Music.
        val soundTags = PackFamily.Sound.subPackTiles().map { it.testTagSuffix }
        assertFalse(soundTags.contains("sound_atmospheres_nature"))
        assertFalse(soundTags.contains("sound_museums_classical"))
        assertFalse(soundTags.contains("sound_genart_digital"))
        assertFalse(soundTags.contains("sound_stem"))
        assertFalse(soundTags.contains("sound_wavetable"))
        assertFalse(soundTags.contains("sound_executorch"))
        assertFalse(soundTags.contains("sound_downloaded_music"))
    }

    @Test
    fun genartItemCounts_computedWhenCatalogProvided() {
        val homeTile = PackFamily.Genart.homeTile(catalog)
        assertEquals(5, homeTile.itemCount)

        val subTiles = PackFamily.Genart.subPackTiles(catalog)
        val abstractTile = subTiles.first { it.testTagSuffix == "genart_abstract" }
        assertEquals(2, abstractTile.itemCount)
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
    fun sortedByGenartNumber_ordersByLeadingNumber() {
        val unorganized = listOf(
            Artwork(id = "150", title = "#150 - Milky Way Galaxy", sourceId = "genart", kind = ArtworkKind.Genart),
            Artwork(id = "2", title = "#2 - Wire Lattice", sourceId = "genart", kind = ArtworkKind.Genart),
            Artwork(id = "10", title = "#10 - Grass in Wind", sourceId = "genart", kind = ArtworkKind.Genart),
            Artwork(id = "1", title = "#1 - Drifting Particles", sourceId = "genart", kind = ArtworkKind.Genart),
        )
        val sorted = unorganized.sortedByGenartNumber()
        assertEquals(listOf("#1 - Drifting Particles", "#2 - Wire Lattice", "#10 - Grass in Wind", "#150 - Milky Way Galaxy"), sorted.map { art -> art.title })
    }

    @Test
    fun sampleRotationPool_sequentialRotatedOrder_whenSeedProvided() {
        val genartList = (1..5).map { i ->
            Artwork(id = "art-$i", title = "#$i - Item $i", sourceId = "genart", kind = ArtworkKind.Genart)
        }
        val sampled = fr.geoking.arthur.auto.AmbientAlbumArt.sampleRotationPool(
            pool = genartList,
            seed = genartList[2], // art-3 (#3)
        )
        assertEquals(listOf("art-3", "art-4", "art-5", "art-1", "art-2"), sampled.map { it.id })
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
    fun sourceIdsForAmbientLoad_museumNull_defaultsToRandom() {
        val ids = PackSelection(PackFamily.Museum).sourceIdsForAmbientLoad()!!
        assertEquals(MuseumTopic.institutions.mapNotNull { it.sourceId }.toSet(), ids.toSet())
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

    @Test
    fun genartPackCovers_areUniqueExceptSharedRandom() {
        val covers = GenartTopic.entries
            .filter { it != GenartTopic.Random }
            .map { it to PackCovers.genart(it) }
        val duplicates = covers.groupBy { it.second }.filter { it.value.size > 1 }
        assertTrue(
            "duplicate genart coverRes: $duplicates",
            duplicates.isEmpty(),
        )
        // Vintage Games cover must not collide with other genart topic covers.
        assertEquals(R.drawable.pack_vintage_games, PackCovers.genart(GenartTopic.Vintage))
        assertTrue(
            covers.none { it.first != GenartTopic.Vintage && it.second == R.drawable.pack_vintage_games },
        )
    }
}
