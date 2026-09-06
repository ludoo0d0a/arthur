package fr.geoking.arthur.source

import fr.geoking.arthur.auto.AmbientStillRenderer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageLoadingErrorTest {

    @Test
    fun ambientStillRenderer_producesErrorBitmap_whenImageFailsToLoad() {
        val missingArtwork = Artwork(
            id = "missing-photo-123",
            title = "Missing Photo",
            attribution = "Test",
            sourceId = "test",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://invalid.domain.example/nonexistent.jpg",
        )

        val bitmap = AmbientStillRenderer.render(missingArtwork, 1L)
        assertNotNull("Renderer must produce fallback bitmap on failure", bitmap)
        assertEquals(AmbientStillRenderer.SIZE, bitmap.width)
        assertEquals(AmbientStillRenderer.SIZE, bitmap.height)
    }

    @Test
    fun ambientStillRenderer_producesErrorBitmap_whenNoPathOrUrlProvided() {
        val emptyArtwork = Artwork(
            id = "empty-painting-456",
            title = "Empty Painting",
            attribution = "Test",
            sourceId = "test",
            kind = ArtworkKind.Painting,
        )

        val bitmap = AmbientStillRenderer.render(emptyArtwork, 1L)
        assertNotNull("Renderer must produce fallback bitmap when URL is missing", bitmap)
        assertEquals(AmbientStillRenderer.SIZE, bitmap.width)
        assertEquals(AmbientStillRenderer.SIZE, bitmap.height)
    }
}
