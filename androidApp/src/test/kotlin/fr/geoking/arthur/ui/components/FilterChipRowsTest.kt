package fr.geoking.arthur.ui.components

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
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
            title = "Bundled Sculpture",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Sculpture,
        ),
        Artwork(
            id = "photo-1",
            title = "Photo",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Photo,
        ),
    )

    @Test
    fun emptySourceSelection_meansAllForKind() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.SCULPTURE,
            selectedSourceIds = emptySet(),
        )
        assertEquals(
            listOf("rijks-sculpt", "bundled-sculpt"),
            filtered.map { it.id },
        )
    }

    @Test
    fun selectedSources_filterWithinKind() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.SCULPTURE,
            selectedSourceIds = setOf(RijksmuseumSource.ID),
        )
        assertEquals(listOf("rijks-sculpt"), filtered.map { it.id })
    }

    @Test
    fun metOnSculpture_canYieldEmpty() {
        val filtered = catalog.filterByCategoryAndSources(
            CategoryFilter.SCULPTURE,
            selectedSourceIds = setOf(MetSource.ID),
        )
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun toggleSource_addsAndRemoves() {
        val withMet = emptySet<String>().toggleSource(MetSource.ID)
        assertEquals(setOf(MetSource.ID), withMet)
        assertEquals(emptySet<String>(), withMet.toggleSource(MetSource.ID))
    }

    @Test
    fun categoryFlags_photoAndMuseum() {
        assertTrue(CategoryFilter.PHOTO.showsPhotoTopics())
        assertFalse(CategoryFilter.PHOTO.showsMuseumSources())
        assertTrue(CategoryFilter.SCULPTURE.showsMuseumSources())
        assertTrue(CategoryFilter.PAINTING.showsMuseumSources())
        assertFalse(CategoryFilter.ALL.showsPhotoTopics())
        assertFalse(CategoryFilter.ALL.showsMuseumSources())
    }
}
