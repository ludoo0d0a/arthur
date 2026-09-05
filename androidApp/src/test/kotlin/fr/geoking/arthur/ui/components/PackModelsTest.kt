package fr.geoking.arthur.ui.components

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
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
    )

    @Test
    fun museumAll_includesMuseumSources_excludesBundled() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Museum))
        assertEquals(listOf("met-1", "rijks-sculpt"), pool.map { it.id })
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
    fun paintingAll_includesBundledAndMet() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Painting))
        assertEquals(listOf("met-1", "bundled-paint"), pool.map { it.id })
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
    fun genartAll_includesAllGenartKinds() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Genart))
        assertEquals(
            listOf(GenartSource.SNOW, GenartSource.GRASS, GenartSource.NEBULA),
            pool.map { it.id },
        )
    }

    @Test
    fun photoAll_includesPhotos() {
        val pool = resolvePackPool(catalog, PackSelection(PackFamily.Photo))
        assertEquals(listOf("pexels-1"), pool.map { it.id })
    }

    @Test
    fun subPackTiles_museumListsEveryInstitutionSource() {
        val tiles = PackFamily.Museum.subPackTiles()
        assertEquals("museum_all", tiles.first().testTagSuffix)
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
    fun paintingSubPacks_includeSuggestionsAndEveryMuseum() {
        val suffixes = PackFamily.Painting.subPackTiles().map { it.testTagSuffix }
        assertTrue(suffixes.contains("painting_all"))
        assertTrue(suffixes.contains("painting_suggestions"))
        assertTrue(suffixes.contains("painting_europeana"))
        assertEquals(
            MuseumTopic.entries.size + 1, // All + every MuseumTopic
            suffixes.size,
        )
    }

    @Test
    fun isGenartCustom() {
        assertTrue(
            PackSelection(PackFamily.Genart, GenartTopic.Custom.testTagSuffix).isGenartCustom(),
        )
        assertFalse(PackSelection(PackFamily.Genart).isGenartCustom())
    }
}
