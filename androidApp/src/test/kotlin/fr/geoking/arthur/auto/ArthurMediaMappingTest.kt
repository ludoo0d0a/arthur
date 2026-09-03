package fr.geoking.arthur.auto

import androidx.media.utils.MediaConstants
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure mapping tests for Media browse ids and album-art helpers (no device). */
class ArthurMediaMappingTest {
    @Test
    fun rootId_isStable() {
        assertEquals("arthur_root", ArthurMediaService.ROOT)
        assertEquals(ArthurMediaBrowse.ROOT, ArthurMediaService.ROOT)
    }

    @Test
    fun playableFlag_isSet() {
        assertTrue(
            android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE != 0,
        )
    }

    @Test
    fun browsableFlag_isSet() {
        assertTrue(
            android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE != 0,
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
        assertEquals(ArtworkKind.Genart, AmbientStillRenderer.kindForId("genart.aurora"))
        assertEquals(ArtworkKind.Genart, AmbientStillRenderer.kindForId("genart.snow"))
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

    @Test
    fun browse_contentStyleConstants_matchAaGridAndList() {
        // Host content-style hints (MediaConstants) — list folders, grid playable previews.
        assertEquals(1, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        assertEquals(2, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
        assertEquals(
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT",
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
        )
        assertEquals(
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT",
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
        )
    }

    @Test
    fun browse_folderIds_roundTrip() {
        assertEquals("folder:genart", ArthurMediaBrowse.folderId(GenartSource.ID))
        assertEquals(GenartSource.ID, ArthurMediaBrowse.sourceIdFromFolder("folder:genart"))
        assertTrue(ArthurMediaBrowse.isFolder("folder:genart"))
        assertFalse(ArthurMediaBrowse.isFolder("genart.particles"))
    }

    @Test
    fun browse_rootSourceOrder_prefersGenart() {
        val catalog = listOf(
            Artwork("bundled-1", "A", sourceId = "bundled", kind = ArtworkKind.Painting),
            Artwork("genart.snow", "Snow", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
            Artwork("fractal.julia", "Julia", sourceId = "fractal", kind = ArtworkKind.FractalPreset),
        )
        assertEquals(
            listOf(GenartSource.ID, "fractal", "bundled"),
            ArthurMediaBrowse.rootSourceIds(catalog),
        )
    }

    @Test
    fun browse_genartFolder_listsOnlyGenart() {
        val catalog = listOf(
            Artwork("genart.snow", "Snow", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
            Artwork("genart.aurora", "Aurora", sourceId = GenartSource.ID, kind = ArtworkKind.Genart),
            Artwork("fractal.julia", "Julia", sourceId = "fractal", kind = ArtworkKind.FractalPreset),
        )
        val children = ArthurMediaBrowse.childrenOf(
            ArthurMediaBrowse.folderId(GenartSource.ID),
            catalog,
        )
        assertEquals(listOf("genart.snow", "genart.aurora"), children.map { it.id })
        assertTrue(ArthurMediaBrowse.usesPreviewGrid(GenartSource.ID))
    }
}
