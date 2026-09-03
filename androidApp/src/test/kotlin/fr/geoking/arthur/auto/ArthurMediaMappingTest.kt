package fr.geoking.arthur.auto

import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure mapping tests for Media browse ids and album-art helpers (no device). */
class ArthurMediaMappingTest {
    @Test
    fun rootId_isStable() {
        assertEquals("arthur_root", ArthurMediaService.ROOT)
    }

    @Test
    fun playableFlag_isSet() {
        assertTrue(
            android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE != 0,
        )
    }

    @Test
    fun rotationInterval_isTwentySeconds() {
        assertEquals(20_000L, AmbientAlbumArt.ROTATION_INTERVAL_MS)
    }

    @Test
    fun advanceIndex_wraps() {
        assertEquals(1, AmbientAlbumArt.advanceIndex(0, 3))
        assertEquals(0, AmbientAlbumArt.advanceIndex(2, 3))
        assertEquals(0, AmbientAlbumArt.advanceIndex(0, 0))
    }

    @Test
    fun authority_usesPackageSuffix() {
        assertEquals("fr.geoking.arthur.albumart", AmbientAlbumArt.authority("fr.geoking.arthur"))
    }

    @Test
    fun parsePathSegments_roundTrips() {
        val parsed = AmbientAlbumArt.parsePathSegments(listOf("art", "genart.particles", "7"))
        assertEquals("genart.particles", parsed!!.first)
        assertEquals(7L, parsed.second)
    }

    @Test
    fun sanitize_stripsUnsafePathChars() {
        assertEquals("genart.particles", AmbientAlbumArt.sanitize("genart.particles"))
        assertEquals("rijks-SK-C-5", AmbientAlbumArt.sanitize("rijks-SK-C-5"))
        assertEquals("a_b_c", AmbientAlbumArt.sanitize("a/b c"))
    }

    @Test
    fun fileName_embedsGeneration() {
        assertEquals(
            "genart.particles__3.png",
            AmbientAlbumArt.fileName("genart.particles", 3L),
        )
    }

    @Test
    fun kindForId_mapsPrefixes() {
        assertEquals(ArtworkKind.Genart, AmbientStillRenderer.kindForId("genart.tunnel"))
        assertEquals(ArtworkKind.Genart, AmbientStillRenderer.kindForId("genart.sphere"))
        assertEquals(ArtworkKind.Genart, AmbientStillRenderer.kindForId("genart.waves"))
        assertEquals(ArtworkKind.Genart, AmbientStillRenderer.kindForId("genart.micro"))
        assertEquals(ArtworkKind.FractalPreset, AmbientStillRenderer.kindForId("fractal.mandelbrot"))
        assertEquals(
            ArtworkKind.CustomFractal,
            AmbientStillRenderer.kindForId("customfractal.v1.c7.m0.p200_700_500_200_800_700"),
        )
        assertEquals(ArtworkKind.Painting, AmbientStillRenderer.kindForId("rijks-SK-C-5"))
    }

    @Test
    fun parsePathSegments_rejectsMalformed() {
        assertNull(AmbientAlbumArt.parsePathSegments(listOf("nope")))
        assertNull(AmbientAlbumArt.parsePathSegments(listOf("art", "only")))
        assertNull(AmbientAlbumArt.parsePathSegments(listOf("art", "id", "x")))
    }
}
