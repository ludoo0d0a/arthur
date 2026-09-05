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
        assertEquals(setOf(ArtworkKind.Photo), unsplash.kinds)
        assertEquals(setOf(ArtworkKind.Photo), unsplash.remoteSearchKinds)
    }

    @Test
    fun rijksAndClevelandSupportMuseumPhotoSearch() {
        for (id in listOf(RijksmuseumSource.ID, ClevelandSource.ID)) {
            val support = SourceCapabilities.support(id)!!
            assertTrue(ArtworkKind.Photo in support.kinds)
            assertTrue(ArtworkKind.Photo in support.remoteSearchKinds)
            assertTrue(ArtworkKind.Painting in support.remoteSearchKinds)
            assertTrue(ArtworkKind.Sculpture in support.remoteSearchKinds)
        }
    }

    @Test
    fun metDoesNotOfferPhotoRemoteSearch() {
        val met = SourceCapabilities.support(MetSource.ID)!!
        assertFalse(ArtworkKind.Photo in met.kinds)
        assertFalse(ArtworkKind.Photo in met.remoteSearchKinds)
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
    fun photoRemoteSearchIdsIncludeStockAndMuseumPhotoApis() {
        val ids = SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Photo)
        assertTrue(PexelsSource.ID in ids)
        assertTrue(UnsplashSource.ID in ids)
        assertTrue(RijksmuseumSource.ID in ids)
        assertTrue(ClevelandSource.ID in ids)
        assertFalse(BundledPackSource.ID in ids)
        assertFalse(MetSource.ID in ids)
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
