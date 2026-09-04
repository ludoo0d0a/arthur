package fr.geoking.arthur.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveAmbientArtworkTest {

    private val still = Artwork("bundled-1", "Study", sourceId = "bundled", kind = ArtworkKind.Painting)
    private val particles = Artwork("genart.particles", "Particles", sourceId = "genart", kind = ArtworkKind.Genart)
    private val mandelbrot = Artwork("fractal.mandelbrot", "Mandelbrot", sourceId = "fractal", kind = ArtworkKind.FractalPreset)
    private val catalog = listOf(still, particles, mandelbrot)

    @Test
    fun prefersRequestedGenerative() {
        assertEquals(mandelbrot, resolveAmbientArtwork(catalog, mandelbrot.id))
        assertEquals(particles, resolveAmbientArtwork(catalog, particles.id))
    }

    @Test
    fun requestedStillIsHonored() {
        assertEquals(still, resolveAmbientArtwork(catalog, still.id))
    }

    @Test
    fun missingOrUnrequestedFallsBackToFirstGenerative() {
        assertEquals(particles, resolveAmbientArtwork(catalog, null))
        assertEquals(particles, resolveAmbientArtwork(catalog, "missing"))
    }

    @Test
    fun emptyCatalogReturnsNull() {
        assertNull(resolveAmbientArtwork(emptyList(), null))
    }

    @Test
    fun stillOnlyCatalogKeepsStill() {
        assertEquals(still, resolveAmbientArtwork(listOf(still), still.id))
    }
}
