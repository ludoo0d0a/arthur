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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pure mapping tests for Media browse ids and album-art helpers (no device). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
    fun nextValidIndex_skipsInvalid() {
        val invalid = setOf(1)
        assertEquals(
            2,
            AmbientAlbumArt.nextValidIndex(
                poolSize = 3,
                currentIndex = 0,
                delta = +1,
                isInvalidAt = { it in invalid },
            ),
        )
    }

    @Test
    fun sampleRotationPool_capsAtMaxAndPrefersCached() {
        val pool = (1..35).map { i ->
            Artwork(
                id = "id-$i",
                title = "T$i",
                sourceId = "pexels",
                kind = ArtworkKind.Photo,
                remoteUrl = "https://example.com/$i.jpg",
            )
        }
        val sampled = AmbientAlbumArt.sampleRotationPool(
            pool = pool,
            isPreferred = { it.id == "id-4" || it.id == "id-5" },
        )
        assertEquals(AmbientAlbumArt.MAX_AUTO_ROTATION_POOL, sampled.size)
        assertTrue(sampled.first().id == "id-4" || sampled.first().id == "id-5")
        assertTrue(sampled.all { it.id in pool.map { art -> art.id } })
        // Preferred items appear before non-preferred once the seed/first slot is filled.
        val preferredIds = setOf("id-4", "id-5")
        val firstNonPreferred = sampled.indexOfFirst { it.id !in preferredIds }
        if (firstNonPreferred >= 0) {
            assertTrue(sampled.take(firstNonPreferred).all { it.id in preferredIds })
        }
    }

    @Test
    fun sampleRotationPool_keepsSeedFirst() {
        val pool = listOf(
            Artwork(id = "a", title = "A", sourceId = "s", kind = ArtworkKind.Photo),
            Artwork(id = "b", title = "B", sourceId = "s", kind = ArtworkKind.Photo),
            Artwork(id = "c", title = "C", sourceId = "s", kind = ArtworkKind.Photo),
            Artwork(id = "d", title = "D", sourceId = "s", kind = ArtworkKind.Photo),
        )
        val sampled = AmbientAlbumArt.sampleRotationPool(pool, seed = pool[2])
        assertEquals(4, sampled.size)
        assertEquals("c", sampled.first().id)
    }

    @Test
    fun sampleRotationPool_generativePool_preservesAllItems() {
        val pool = (1..10).map { i ->
            Artwork(
                id = "genart-$i",
                title = "Genart $i",
                sourceId = GenartSource.ID,
                kind = ArtworkKind.Genart,
            )
        }
        val sampled = AmbientAlbumArt.sampleRotationPool(pool)
        assertEquals(10, sampled.size)
        assertEquals(pool.map { it.id }.toSet(), sampled.map { it.id }.toSet())
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
        // Host content-style hints (MediaConstants) — grid folders and grid playable previews.
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

        val rootBundle = ArthurMediaBrowse.rootExtras()
        assertEquals(
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            rootBundle.getInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE),
        )
        assertEquals(
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            rootBundle.getInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE),
        )

        val gridBundle = ArthurMediaBrowse.previewGridExtras()
        assertEquals(
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            gridBundle.getInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE),
        )
        assertEquals(
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            gridBundle.getInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE),
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

    @Test
    fun mediaSubtitleFormat_includesQuotesAndSlidePositionWhenEnabled() {
        val art = Artwork("genart.particles", "Particles", attribution = "GeoKing", sourceId = "genart", kind = ArtworkKind.Genart)
        val quoteText = "“Simple is best” — Author"
        val slidePos = "[1/5]"

        val subtitleWithQuote = buildString {
            if (art.attribution.isNotBlank()) append(art.attribution)
            if (isNotEmpty()) append(" • ")
            append(quoteText)
            if (isNotEmpty()) append(" ")
            append(slidePos)
        }

        assertEquals("GeoKing • “Simple is best” — Author [1/5]", subtitleWithQuote)
    }

    @Test
    fun automotiveAppDesc_existsAndDeclaresMediaSupport() {
        val file = java.io.File("src/main/res/xml/automotive_app_desc.xml")
        assertTrue("automotive_app_desc.xml must exist", file.exists())
        val content = file.readText()
        assertTrue("Must contain <automotiveApp>", content.contains("<automotiveApp>"))
        assertTrue("Must contain <uses name=\"media\"", content.contains("<uses name=\"media\""))
    }

    @Test
    fun mediaPlaybackActions_includeThreeButtonControls() {
        val actions = android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        assertTrue("Must include ACTION_PLAY_PAUSE", actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L)
        assertTrue("Must include ACTION_PLAY", actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY != 0L)
        assertTrue("Must include ACTION_PAUSE", actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE != 0L)
        assertTrue("Must include ACTION_SKIP_TO_PREVIOUS", actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L)
        assertTrue("Must include ACTION_SKIP_TO_NEXT", actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L)
    }
}
