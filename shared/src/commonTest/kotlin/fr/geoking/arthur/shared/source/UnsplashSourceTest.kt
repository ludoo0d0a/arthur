package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class UnsplashSourceTest {
    @Test
    fun loadsArtworkFromUnsplashFixtures() = runBlocking {
        val fixtures = mapOf(
            UnsplashSource.searchUrl() to """
                {
                  "results": [
                    {
                      "id": "abc123",
                      "description": "Misty forest path",
                      "alt_description": "trees in fog",
                      "urls": {
                        "regular": "https://images.unsplash.com/photo-abc123?w=1080",
                        "full": "https://images.unsplash.com/photo-abc123?fm=jpg"
                      },
                      "user": { "name": "Jane Photographer" }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = UnsplashSource(
            accessKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("unsplash-abc123", art[0].id)
        assertEquals("Misty forest path", art[0].title)
        assertEquals("Jane Photographer / Unsplash", art[0].attribution)
        assertEquals(
            "https://images.unsplash.com/photo-abc123?w=1080",
            art[0].remoteUrl,
        )
        assertEquals(UnsplashSource.ID, art[0].sourceId)
    }

    @Test
    fun blankAccessKeyUsesOfflineFallback() = runBlocking {
        var called = false
        val cached = listOf(
            fr.geoking.arthur.shared.domain.Artwork(
                id = "unsplash-1",
                title = "Cached",
                sourceId = UnsplashSource.ID,
                kind = fr.geoking.arthur.shared.domain.ArtworkKind.Photo,
                localPath = "/tmp/cached.jpg",
            ),
        )
        val source = UnsplashSource(
            accessKey = "  ",
            offlineFallback = { cached },
            httpGet = {
                called = true
                error("should not call")
            },
        )
        assertEquals(cached, source.load())
        assertEquals(false, called)
    }

    @Test
    fun searchUrlIncludesCategoryQuery() {
        assertEquals(
            "https://api.unsplash.com/search/photos?query=city&orientation=landscape&per_page=8",
            UnsplashSource.searchUrl("city"),
        )
    }

    @Test
    fun fallsBackToAltDescriptionWhenDescriptionMissing() = runBlocking {
        val fixtures = mapOf(
            UnsplashSource.searchUrl() to """
                {
                  "results": [
                    {
                      "id": "xyz",
                      "alt_description": "calm lake",
                      "urls": { "regular": "https://images.unsplash.com/photo-xyz" },
                      "user": { "name": "Alex" }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = UnsplashSource(
            accessKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
        )
        assertEquals("calm lake", source.load().single().title)
    }
}
