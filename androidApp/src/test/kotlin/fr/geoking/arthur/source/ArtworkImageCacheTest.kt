package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ArtworkImageCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

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
        cache.putImage(art.id, ByteArray(2_000) { 0x4F })

        val loaded = cache.loadCached(StockPhotoCategory.Nature.query, PexelsSource.ID)
        assertEquals(1, loaded.size)
        assertEquals(art.id, loaded[0].id)
        assertTrue(loaded[0].localPath != null)
        assertTrue(cache.hasImage(art.id))
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
        cache.putImage(nature.id, ByteArray(2_000) { 0x4F })
        cache.putImage(city.id, ByteArray(2_000) { 0x4F })

        val loaded = cache.loadCachedStock(StockPhotoCategory.Random, PexelsSource.ID)
        assertEquals(setOf(nature.id, city.id), loaded.map { it.id }.toSet())
    }

    @Test
    fun genartCache_limitedToMaxThirtyLru() {
        val cache = ArtworkImageCache(context)
        val bytes = ByteArray(2_000) { 0x41 }
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
        cache.putImage(artId, ByteArray(2_000) { 0x33 })

        val file = cache.downloadAndCache(artId, "https://invalid-url-should-not-be-called.example")
        assertTrue(file != null)
        assertTrue(file!!.exists())
        assertEquals(2_000L, file.length())
    }
}
