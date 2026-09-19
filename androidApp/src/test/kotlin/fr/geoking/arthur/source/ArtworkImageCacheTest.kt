package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ArtworkImageCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val jpegBytes get() = TestStillBytes.jpeg()

    @Test
    fun remembersAndLoadsCachedPhotosForCategory() {
        val cache = ArtworkImageCache(context)
        val art = Artwork(
            id = "pexels-42",
            title = "Forest",
            attribution = "A / Pexels",
            sourceId = PexelsSource.ID,
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/a.jpg",
        )
        cache.remember(listOf(art), StockPhotoCategory.Nature.query)
        cache.putImage(art.id, jpegBytes)

        val loaded = cache.loadCached(StockPhotoCategory.Nature.query, PexelsSource.ID)
        assertEquals(1, loaded.size)
        assertEquals(art.id, loaded[0].id)
        assertTrue(loaded[0].localPath != null)
        assertTrue(cache.hasImage(art.id))
        assertTrue(cache.hasDecodableImage(art.id))
    }

    @Test
    fun loadCachedStock_randomMergesRemoteTopicBuckets() {
        val cache = ArtworkImageCache(context)
        val nature = Artwork(
            id = "pexels-nature",
            title = "Forest",
            sourceId = PexelsSource.ID,
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/n.jpg",
        )
        val city = Artwork(
            id = "pexels-city",
            title = "Skyline",
            sourceId = PexelsSource.ID,
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/c.jpg",
        )
        cache.remember(listOf(nature), StockPhotoCategory.Nature.query)
        cache.remember(listOf(city), StockPhotoCategory.City.query)
        cache.putImage(nature.id, jpegBytes)
        cache.putImage(city.id, jpegBytes)

        val loaded = cache.loadCachedStock(StockPhotoCategory.Random, PexelsSource.ID)
        assertEquals(setOf(nature.id, city.id), loaded.map { it.id }.toSet())
    }

    @Test
    fun genartCache_limitedToMaxThirtyLru() {
        val cache = ArtworkImageCache(context)
        val bytes = jpegBytes
        for (i in 1..ArtworkImageCache.MAX_GENART + 5) {
            val art = Artwork(
                id = "genart.item$i",
                title = "G$i",
                sourceId = "genart",
                kind = ArtworkKind.Genart,
            )
            cache.putImage(art.id, bytes)
            cache.rememberGenart(art)
        }
        val loaded = cache.loadCachedGenart()
        assertEquals(ArtworkImageCache.MAX_GENART, loaded.size)
        assertTrue(loaded.none { it.id == "genart.item1" })
        assertTrue(loaded.any { it.id == "genart.item${ArtworkImageCache.MAX_GENART + 5}" })
    }

    @Test
    fun stockPhotoSettingsPersistsCategory() {
        val settings = StockPhotoSettings(context)
        settings.category = StockPhotoCategory.Ocean
        assertEquals(StockPhotoCategory.Ocean, StockPhotoSettings(context).category)
    }

    @Test
    fun stockPhotoSettingsDefaultsToRandom() {
        context.getSharedPreferences("arthur_stock_photo", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        assertEquals(StockPhotoCategory.Random, StockPhotoSettings(context).category)
    }

    @Test
    fun downloadAndCache_returnsExistingFileIfAlreadyPresent() {
        val cache = ArtworkImageCache(context)
        val artId = "test-art-123"
        cache.putImage(artId, jpegBytes)

        val file = cache.downloadAndCache(artId, "https://invalid-url-should-not-be-called.example")
        assertTrue(file.exists())
        assertTrue(file.length() > 1_000L)
    }

    @Test
    fun downloadAndCache_cacheOnlyMissWhenNoFile() {
        val cache = ArtworkImageCache(context)
        val artId = "test-art-uncached-wifi"
        try {
            cache.downloadAndCache(
                artId,
                "https://example.com/should-not-download.jpg",
                allowNetwork = false,
            )
            org.junit.Assert.fail("Expected RemoteStillCacheOnlyMiss")
        } catch (e: RemoteStillCacheOnlyMiss) {
            assertEquals(artId, e.artworkId)
        }
    }

    @Test
    fun downloadAndCache_cacheOnlyReturnsExistingDecodable() {
        val cache = ArtworkImageCache(context)
        val artId = "test-art-cached-wifi"
        cache.putImage(artId, jpegBytes)
        val file = cache.downloadAndCache(
            artId,
            "https://example.com/should-not-download.jpg",
            allowNetwork = false,
        )
        assertTrue(file.exists())
    }

    @Test
    fun validCachedFileOrNull_returnsNullWhenMissing() {
        val cache = ArtworkImageCache(context)
        assertEquals(null, cache.validCachedFileOrNull("missing-art"))
    }

    @Test
    fun purgeInvalid_deletesFileAndMarksStore() {
        val store = InvalidArtworkStore(context)
        store.clear("corrupt-art")
        val cache = ArtworkImageCache(context, store)
        val artId = "corrupt-art"
        cache.putImage(artId, TestStillBytes.jpeg())
        assertTrue(cache.hasImage(artId))
        cache.purgeInvalid(artId)
        assertFalse(cache.hasImage(artId))
        assertTrue(store.isInvalid(artId))
    }

    @Test
    fun clearAll_removesFilesAndIndex() {
        val cache = ArtworkImageCache(context)
        val art = Artwork(
            id = "pexels-clear",
            title = "Clear me",
            sourceId = PexelsSource.ID,
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/c.jpg",
        )
        cache.remember(listOf(art), StockPhotoCategory.Nature.query)
        cache.putImage(art.id, jpegBytes)
        assertEquals(1, cache.entryCount())
        assertTrue(cache.totalBytes() > 0L)

        cache.clearAll()
        assertEquals(0, cache.entryCount())
        assertEquals(0L, cache.totalBytes())
        assertFalse(cache.hasImage(art.id))
        assertTrue(cache.loadCached(StockPhotoCategory.Nature.query, PexelsSource.ID).isEmpty())
    }

    @Test
    fun downloadAndCache_throwsExceptionOnFailure() {
        val cache = ArtworkImageCache(context)
        val artId = "test-art-uncached"
        try {
            cache.downloadAndCache(artId, "http://127.0.0.1:1/nonexistent.jpg")
            org.junit.Assert.fail("Expected exception on download failure")
        } catch (e: Exception) {
            assertTrue(e.message != null)
        }
    }
}
