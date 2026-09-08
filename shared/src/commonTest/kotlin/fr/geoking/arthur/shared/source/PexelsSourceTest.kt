package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PexelsSourceTest {
    @Test
    fun loadsArtworkFromPexelsFixtures() = runBlocking {
        val fixtures = mapOf(
            PexelsSource.searchUrl(perPage = RemoteSample.SEARCH_POOL) to """
                {
                  "photos": [
                    {
                      "id": 2880507,
                      "alt": "Brown Rocks During Golden Hour",
                      "photographer": "eberhard grossgasteiger",
                      "src": {
                        "original": "https://images.pexels.com/photos/2880507/original.jpeg",
                        "large": "https://images.pexels.com/photos/2880507/large.jpeg",
                        "large2x": "https://images.pexels.com/photos/2880507/large2x.jpeg"
                      }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = PexelsSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("pexels-2880507", art[0].id)
        assertEquals("Brown Rocks During Golden Hour", art[0].title)
        assertEquals("eberhard grossgasteiger / Pexels", art[0].attribution)
        assertEquals(
            "https://images.pexels.com/photos/2880507/large2x.jpeg",
            art[0].remoteUrl,
        )
        assertEquals(PexelsSource.ID, art[0].sourceId)
    }

    @Test
    fun blankApiKeyUsesOfflineFallback() = runBlocking {
        var called = false
        val cached = listOf(
            fr.geoking.arthur.shared.domain.Artwork(
                id = "pexels-1",
                title = "Cached",
                sourceId = PexelsSource.ID,
                kind = fr.geoking.arthur.shared.domain.ArtworkKind.Photo,
                localPath = "/tmp/cached.jpg",
            ),
        )
        val source = PexelsSource(
            apiKey = "",
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
            "https://api.pexels.com/v1/search?query=ocean&orientation=landscape&per_page=20&page=1",
            PexelsSource.searchUrl("ocean"),
        )
    }

    @Test
    fun skipsPhotosWithoutImageUrl() = runBlocking {
        val fixtures = mapOf(
            PexelsSource.searchUrl(perPage = RemoteSample.SEARCH_POOL) to """
                {"photos":[{"id":1,"alt":"No src","photographer":"X","src":{}}]}
            """.trimIndent(),
        )
        val source = PexelsSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        assertEquals(emptyList(), source.load())
    }
}
