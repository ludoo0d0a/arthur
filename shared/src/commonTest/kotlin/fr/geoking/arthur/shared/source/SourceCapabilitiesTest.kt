package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceCapabilitiesTest {

    @Test
    fun stockSourcesSupportPhotoSearchOnly() {
        val pexels = SourceCapabilities.support(PexelsSource.ID)!!
        assertEquals(setOf(ArtworkKind.Photo), pexels.kinds)
        assertEquals(setOf(ArtworkKind.Photo), pexels.remoteSearchKinds)

        val unsplash = SourceCapabilities.support(UnsplashSource.ID)!!
        assertEquals(setOf(ArtworkKind.Photo, ArtworkKind.Video), unsplash.kinds)
        assertEquals(setOf(ArtworkKind.Photo, ArtworkKind.Video), unsplash.remoteSearchKinds)

        val pexelsVideo = SourceCapabilities.support(PexelsVideoSource.ID)!!
        assertEquals(setOf(ArtworkKind.Video), pexelsVideo.kinds)
        assertEquals(setOf(ArtworkKind.Video), pexelsVideo.remoteSearchKinds)

        val pixabay = SourceCapabilities.support(PixabayVideoSource.ID)!!
        assertEquals(setOf(ArtworkKind.Video), pixabay.kinds)

        val coverr = SourceCapabilities.support(CoverrSource.ID)!!
        assertEquals(setOf(ArtworkKind.Video), coverr.kinds)
    }

    @Test
    fun museumsSupportPaintingSculptureAndPhotoSearch() {
        for (id in listOf(
            MetSource.ID,
            RijksmuseumSource.ID,
            ArticSource.ID,
            ClevelandSource.ID,
            EuropeanaSource.ID,
            HarvardSource.ID,
            SmithsonianSource.ID,
            LouvreSource.ID,
        )) {
            val support = SourceCapabilities.support(id)!!
            assertTrue(ArtworkKind.Photo in support.kinds, id)
            assertTrue(ArtworkKind.Photo in support.remoteSearchKinds, id)
            assertTrue(ArtworkKind.Painting in support.remoteSearchKinds, id)
            assertTrue(ArtworkKind.Sculpture in support.remoteSearchKinds, id)
        }
    }

    @Test
    fun bundledIsCuratedWithoutRemoteSearch() {
        val bundled = SourceCapabilities.support(BundledPackSource.ID)!!
        assertTrue(ArtworkKind.Photo in bundled.kinds)
        assertTrue(ArtworkKind.Painting in bundled.kinds)
        assertTrue(ArtworkKind.Sculpture in bundled.kinds)
        assertTrue(bundled.remoteSearchKinds.isEmpty())
    }

    @Test
    fun photoRemoteSearchIdsIncludeStockAndAllMuseums() {
        val ids = SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Photo)
        assertTrue(PexelsSource.ID in ids)
        assertTrue(UnsplashSource.ID in ids)
        assertTrue(MetSource.ID in ids)
        assertTrue(RijksmuseumSource.ID in ids)
        assertTrue(ClevelandSource.ID in ids)
        assertFalse(BundledPackSource.ID in ids)
        assertFalse(PexelsVideoSource.ID in ids)
    }

    @Test
    fun kindAmbientIdsExcludeBundled() {
        val ids = SourceCapabilities.sourceIdsForKindAmbient(ArtworkKind.Painting)
        assertTrue(BundledPackSource.ID !in ids)
        assertTrue(MetSource.ID in ids)
        assertTrue(PexelsSource.ID !in ids)
    }

    @Test
    fun videoRemoteSearchIdsIncludeVideoApis() {
        val ids = SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Video)
        assertEquals(
            setOf(PexelsVideoSource.ID, UnsplashSource.ID, PixabayVideoSource.ID, CoverrSource.ID),
            ids.toSet(),
        )
    }

    @Test
    fun paintingSupportingIdsIncludeBundledAndMuseums() {
        val ids = SourceCapabilities.sourceIdsSupporting(ArtworkKind.Painting)
        assertTrue(BundledPackSource.ID in ids)
        assertTrue(MetSource.ID in ids)
        assertTrue(WikimediaStreetArtSource.ID in ids)
        assertFalse(PexelsSource.ID in ids)
    }
}
