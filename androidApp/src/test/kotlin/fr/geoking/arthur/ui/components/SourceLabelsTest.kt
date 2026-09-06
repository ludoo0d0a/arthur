package fr.geoking.arthur.ui.components

import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.UnsplashSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceLabelsTest {
    @Test
    fun sourceLabelRes_knownSources() {
        assertEquals(R.string.source_met, sourceLabelRes(MetSource.ID))
        assertEquals(R.string.source_unsplash, sourceLabelRes(UnsplashSource.ID))
        assertEquals(R.string.source_louvre, sourceLabelRes(LouvreSource.ID))
        assertNull(sourceLabelRes("unknown-source"))
    }

    @Test
    fun authorForDisplay_stripsTrailingSourceSuffix() {
        val art = Artwork(
            id = "1",
            title = "Mist",
            attribution = "Jane Photographer / Unsplash",
            sourceId = UnsplashSource.ID,
            kind = ArtworkKind.Photo,
        )
        assertEquals("Jane Photographer", art.authorForDisplay("Unsplash"))
    }

    @Test
    fun authorForDisplay_keepsPlainArtist() {
        val art = Artwork(
            id = "1",
            title = "Wheat",
            attribution = "Vincent van Gogh",
            sourceId = MetSource.ID,
            kind = ArtworkKind.Painting,
        )
        assertEquals("Vincent van Gogh", art.authorForDisplay("The Met"))
    }
}
