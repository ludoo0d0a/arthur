package fr.geoking.arthur.ui.screens

import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmbientPrefetchRenewalTest {

    private val art1 = Artwork("art-1", "Title 1", "Author 1", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/1.jpg")
    private val art2 = Artwork("art-2", "Title 2", "Author 2", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/2.jpg")
    private val art3 = Artwork("art-3", "Title 3", "Author 3", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/3.jpg")
    private val art4 = Artwork("art-4", "Title 4", "Author 4", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/4.jpg")

    @Test
    fun shouldPrefetchNextPool_triggersNearEndOfSourcePage() {
        val page = AmbientAlbumArt.SOURCE_CATALOG_PAGE
        val lead = AmbientAlbumArt.POOL_RENEW_LEAD
        // Arriving around the 19th/20th of a 20-item page (DEFAULT_LIMIT - 2).
        assertFalse(AmbientAlbumArt.shouldPrefetchNextPool(seenCount = page - lead - 1, poolSize = page))
        assertTrue(AmbientAlbumArt.shouldPrefetchNextPool(seenCount = page - lead, poolSize = page))
        assertTrue(AmbientAlbumArt.shouldPrefetchNextPool(seenCount = page - 1, poolSize = page))
        assertTrue(AmbientAlbumArt.shouldPrefetchNextPool(seenCount = page, poolSize = page))
    }

    @Test
    fun shouldPrefetchNextPool_waitsUntilNearEndAfterAppend() {
        val grown = 40
        assertFalse(AmbientAlbumArt.shouldPrefetchNextPool(seenCount = 19, poolSize = grown))
        assertTrue(AmbientAlbumArt.shouldPrefetchNextPool(seenCount = grown - 2, poolSize = grown))
    }

    @Test
    fun appendToRotationPool_addsDistinctAndKeepsOrder() {
        val current = listOf(art1, art2)
        val incoming = listOf(art2, art3, art4)
        val appended = AmbientAlbumArt.appendToRotationPool(current, incoming)
        assertEquals(listOf(art1, art2, art3, art4).map { it.id }, appended.map { it.id })
    }

    @Test
    fun appendToRotationPool_noopWhenNoNewIds() {
        val current = listOf(art1, art2)
        val appended = AmbientAlbumArt.appendToRotationPool(current, listOf(art1, art2))
        assertEquals(current, appended)
    }

    @Test
    fun appendToRotationPool_dropsOldestWhenOverMaxKeepingCurrent() {
        val current = (1..5).map { i ->
            Artwork("id-$i", "T$i", "A", "s", ArtworkKind.Photo, remoteUrl = "https://example.com/$i.jpg")
        }
        val incoming = (6..8).map { i ->
            Artwork("id-$i", "T$i", "A", "s", ArtworkKind.Photo, remoteUrl = "https://example.com/$i.jpg")
        }
        val appended = AmbientAlbumArt.appendToRotationPool(
            current = current,
            incoming = incoming,
            maxSize = 6,
            keepId = "id-5",
        )
        assertEquals(6, appended.size)
        assertTrue(appended.any { it.id == "id-5" })
        assertTrue(appended.any { it.id == "id-8" })
        assertFalse(appended.any { it.id == "id-1" })
    }
}
