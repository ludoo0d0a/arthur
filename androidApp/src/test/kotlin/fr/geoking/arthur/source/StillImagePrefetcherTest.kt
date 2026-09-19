package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StillImagePrefetcherTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun ensureCached_skipsWhenAlreadyOnDisk() = runBlocking {
        val cache = ArtworkImageCache(context)
        val art = Artwork(
            id = "prefetch-skip",
            title = "Cached",
            sourceId = "test",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.invalid/missing.jpg",
        )
        cache.putImage(art.id, TestStillBytes.jpeg())
        assertTrue(cache.hasImage(art.id))

        // Would fail if it attempted a network download to example.invalid.
        StillImagePrefetcher.ensureCached(cache, art)
        assertTrue(cache.hasImage(art.id))
    }

    @Test
    fun ensureCached_skipsWhenAllowNetworkFalse() = runBlocking {
        val cache = ArtworkImageCache(context)
        val art = Artwork(
            id = "prefetch-wifi",
            title = "Wifi",
            sourceId = "test",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.invalid/missing.jpg",
        )
        StillImagePrefetcher.ensureCached(cache, art, allowNetwork = false)
        assertFalse(cache.hasImage(art.id))
    }

    @Test
    fun ensureCached_skipsWhenLocalPathPresent() = runBlocking {
        val cache = ArtworkImageCache(context)
        val art = Artwork(
            id = "prefetch-local",
            title = "Local",
            sourceId = "test",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.invalid/missing.jpg",
            localPath = "/tmp/does-not-matter.jpg",
        )
        StillImagePrefetcher.ensureCached(cache, art)
        assertFalse(cache.hasImage(art.id))
    }

    @Test
    fun ensureCached_skipsWhenRemoteUrlBlank() = runBlocking {
        val cache = ArtworkImageCache(context)
        val art = Artwork(
            id = "prefetch-blank",
            title = "Blank",
            sourceId = "test",
            kind = ArtworkKind.Photo,
            remoteUrl = "  ",
        )
        StillImagePrefetcher.ensureCached(cache, art)
        assertFalse(cache.hasImage(art.id))
    }
}
