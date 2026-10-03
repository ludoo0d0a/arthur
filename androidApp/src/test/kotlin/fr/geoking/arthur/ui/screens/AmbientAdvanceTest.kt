package fr.geoking.arthur.ui.screens

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.source.ArtworkImageCache
import fr.geoking.arthur.source.TestStillBytes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AmbientAdvanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pickNextReadyArtwork_skipsFailedIdAndUncachedStill() = runBlocking {
        val cache = ArtworkImageCache(context)
        val a = still("a", remoteUrl = "https://example.com/a.jpg")
        val b = still("b", remoteUrl = "https://example.com/b.jpg")
        val c = still("c", remoteUrl = "https://example.com/c.jpg")
        cache.putImage(c.id, TestStillBytes.jpeg())

        val next = pickNextReadyArtwork(
            pool = listOf(a, b, c),
            fromId = a.id,
            delta = +1,
            skipIds = setOf(b.id),
            eligibleIds = setOf(a.id, b.id, c.id),
            imageCache = cache,
            allowNetwork = false,
        )

        assertEquals(c.id, next?.id)
    }

    @Test
    fun pickNextReadyArtwork_returnsNullWhenNothingReady() = runBlocking {
        val cache = ArtworkImageCache(context)
        val a = still("a", remoteUrl = "https://example.com/a.jpg")
        val b = still("b", remoteUrl = "https://example.com/b.jpg")

        val next = pickNextReadyArtwork(
            pool = listOf(a, b),
            fromId = a.id,
            delta = +1,
            skipIds = emptySet(),
            eligibleIds = setOf(a.id, b.id),
            imageCache = cache,
            allowNetwork = false,
        )

        assertNull(next)
    }

    @Test
    fun pickNextReadyArtwork_acceptsGenerativeWithoutCache() = runBlocking {
        val cache = ArtworkImageCache(context)
        val photo = still("photo", remoteUrl = "https://example.com/p.jpg")
        val gen = Artwork(
            id = "genart.pond",
            title = "Pond",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )

        val next = pickNextReadyArtwork(
            pool = listOf(photo, gen),
            fromId = photo.id,
            delta = +1,
            skipIds = emptySet(),
            eligibleIds = setOf(photo.id, gen.id),
            imageCache = cache,
            allowNetwork = false,
        )

        assertEquals(gen.id, next?.id)
    }

    private fun still(id: String, remoteUrl: String) = Artwork(
        id = id,
        title = id,
        sourceId = "test",
        kind = ArtworkKind.Photo,
        remoteUrl = remoteUrl,
    )
}
