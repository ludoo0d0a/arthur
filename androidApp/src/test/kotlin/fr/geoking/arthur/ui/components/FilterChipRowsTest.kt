package fr.geoking.arthur.ui.components

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.source.UnsplashSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterChipRowsTest {

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
            id = "bundled-sculpt",
            title = "Marble Light",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Sculpture,
        ),
        Artwork(
            id = "bundled-paint",
            title = "Study in Blue",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Painting,
        ),
        Artwork(
            id = "bundled-3",
            title = "Harbor Grain",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Photo,
        ),
        Artwork(
            id = "pexels-1",
            title = "Nature Stock",
            sourceId = PexelsSource.ID,
            kind = ArtworkKind.Photo,
        ),
        Artwork(
            id = "unsplash-1",
            title = "City Stock",
            sourceId = UnsplashSource.ID,
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
            id = GenartSource.PARTICLES,
            title = "Particles",
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
            id = GenartSource.BREATH_CIRCLES,
            title = "Breath Circles",
            sourceId = GenartSource.ID,
            kind = ArtworkKind.Genart,
        ),
        Artwork(
            id = "fractal.julia",
            title = "Julia",
            sourceId = "fractal",
            kind = ArtworkKind.FractalPreset,
        ),
        Artwork(
            id = "customfractal-1",
            title = "My Fractal",
            sourceId = "customfractal",
            kind = ArtworkKind.CustomFractal,
        ),
    )

    @Test
    fun categoryOrder_allGenartPaintingPhotoSculptureThenPersonal() {
        assertEquals(
            listOf(
                CategoryFilter.ALL,
                CategoryFilter.GENART,
                CategoryFilter.PAINTING,
                CategoryFilter.PHOTO,
                CategoryFilter.SCULPTURE,
                CategoryFilter.PERSONAL,
            ),
            CategoryFilter.entries.toList(),
        )
    }

    @Test
    fun genartCategory_includesFractalKinds() {
        assertTrue(CategoryFilter.GENART.matches(ArtworkKind.Genart))
        assertTrue(CategoryFilter.GENART.matches(ArtworkKind.FractalPreset))
        assertTrue(CategoryFilter.GENART.matches(ArtworkKind.CustomFractal))
        assertFalse(CategoryFilter.GENART.matches(ArtworkKind.Photo))
    }

    @Test
    fun genartFractal_showsPresetsOnly() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Fractal,
        )
        assertEquals(listOf("fractal.julia"), filtered.map { it.id })
    }

    @Test
    fun genartCustom_showsCustomFractalsOnly() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Custom,
        )
        assertEquals(listOf("customfractal-1"), filtered.map { it.id })
    }

    @Test
    fun genartNature_showsNatureEngines() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Nature,
        )
        assertEquals(listOf(GenartSource.GRASS), filtered.map { it.id })
    }

    @Test
    fun genartWeather_showsWeatherEngines() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Weather,
        )
        assertEquals(listOf(GenartSource.SNOW), filtered.map { it.id })
    }

    @Test
    fun genartGeometry_showsGeometryEngines() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Geometry,
        )
        assertEquals(listOf(GenartSource.PARTICLES), filtered.map { it.id })
    }

    @Test
    fun genartAbstract_showsAbstractEngines() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Abstract,
        )
        assertEquals(
            listOf(GenartSource.BLOBS, GenartSource.BREATH_CIRCLES),
            filtered.map { it.id },
        )
    }

    @Test
    fun genartPlanets_showsPlanetEngines() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.GENART,
            genartTopic = GenartTopic.Planets,
        )
        assertEquals(listOf(GenartSource.NEBULA), filtered.map { it.id })
    }

    @Test
    fun sculptureSuggestions_showsOnlyBundled() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.SCULPTURE,
            museumTopic = MuseumTopic.Suggestions,
        )
        assertEquals(listOf("bundled-sculpt"), filtered.map { it.id })
    }

    @Test
    fun sculptureRijks_showsMuseumOnly() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.SCULPTURE,
            museumTopic = MuseumTopic.Rijksmuseum,
        )
        assertEquals(listOf("rijks-sculpt"), filtered.map { it.id })
    }

    @Test
    fun paintingMet_showsMuseumAndHidesSuggestions() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.PAINTING,
            museumTopic = MuseumTopic.Met,
        )
        assertEquals(listOf("met-1"), filtered.map { it.id })
        assertFalse(filtered.any { it.sourceId == BundledPackSource.ID })
    }

    @Test
    fun categoryFlags_photoMuseumGenart() {
        assertTrue(CategoryFilter.PHOTO.showsPhotoTopics())
        assertFalse(CategoryFilter.PHOTO.showsMuseumTopics())
        assertTrue(CategoryFilter.GENART.showsGenartTopics())
        assertFalse(CategoryFilter.GENART.showsPhotoTopics())
        assertTrue(CategoryFilter.SCULPTURE.showsMuseumTopics())
        assertTrue(CategoryFilter.PAINTING.showsMuseumTopics())
        assertFalse(CategoryFilter.ALL.showsGenartTopics())
    }

    @Test
    fun photoSuggestions_showsOnlyBundledPhotos() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.PHOTO,
            stockCategory = StockPhotoCategory.Suggestions,
        )
        assertEquals(listOf("bundled-3"), filtered.map { it.id })
    }

    @Test
    fun photoNature_showsStockAndHidesHarborGrain() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.PHOTO,
            stockCategory = StockPhotoCategory.Nature,
        )
        assertEquals(listOf("pexels-1", "unsplash-1"), filtered.map { it.id })
        assertFalse(filtered.any { it.id == "bundled-3" })
    }

    @Test
    fun photoRandom_showsRemoteStockLikeNature() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.PHOTO,
            stockCategory = StockPhotoCategory.Random,
        )
        assertEquals(listOf("pexels-1", "unsplash-1"), filtered.map { it.id })
    }

    @Test
    fun resolve_emptyMuseumSubcategory_hidesSubfiltersAndShowsCategoryMatches() {
        val resolved = resolveCategoryCatalog(
            catalog = catalog,
            category = CategoryFilter.SCULPTURE,
            museumTopic = MuseumTopic.Met, // no Met sculptures in fixture
        )
        assertFalse(resolved.showSubfilters)
        assertEquals(listOf("rijks-sculpt", "bundled-sculpt"), resolved.items.map { it.id })
    }

    @Test
    fun resolve_matchingGenartSubcategory_keepsSubfilters() {
        val resolved = resolveCategoryCatalog(
            catalog = catalog,
            category = CategoryFilter.GENART,
            genartTopic = GenartTopic.Weather,
        )
        assertTrue(resolved.showSubfilters)
        assertEquals(listOf(GenartSource.SNOW), resolved.items.map { it.id })
    }

    @Test
    fun resolve_emptyPhotoSubcategory_keepsTopicsAndFallsBackToCategory() {
        val noStock = catalog.filterNot {
            it.sourceId == PexelsSource.ID || it.sourceId == UnsplashSource.ID
        }
        val resolved = resolveCategoryCatalog(
            catalog = noStock,
            category = CategoryFilter.PHOTO,
            stockCategory = StockPhotoCategory.Nature,
        )
        assertTrue(resolved.showSubfilters)
        assertEquals(listOf("bundled-3"), resolved.items.map { it.id })
    }

    @Test
    fun catalogPage_showsFirstTwentyThenLoadMore() {
        val items = (1..25).map { i ->
            Artwork(
                id = "a-$i",
                title = "A$i",
                sourceId = BundledPackSource.ID,
                kind = ArtworkKind.Photo,
            )
        }
        assertEquals(20, items.takeCatalogPage(CatalogPageSize).size)
        assertTrue(items.canLoadMoreCatalog(CatalogPageSize))
        assertFalse(items.canLoadMoreCatalog(25))
        assertEquals(25, items.takeCatalogPage(40).size)
    }
}
