package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

class PixabayVideoSourceTest {
    @Test
    fun loadsArtworkFromPixabayFixtures() = runBlocking {
        val url = PixabayVideoSource.searchUrl("test-key")
        val fixtures = mapOf(
            url to """
                {
                  "hits": [
                    {
                      "id": 125,
                      "tags": "flowers, yellow, blossom",
                      "user": "Coverr-Free-Footage",
                      "videos": {
                        "medium": {
                          "url": "https://cdn.pixabay.com/video/medium.mp4",
                          "width": 1280,
                          "height": 720
                        },
                        "large": {
                          "url": "https://cdn.pixabay.com/video/large.mp4",
                          "width": 1920,
                          "height": 1080
                        }
                      }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = PixabayVideoSource(
            apiKey = "test-key",
            httpGet = { u -> fixtures.getValue(u) },
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("pixabay-video-125", art[0].id)
        assertEquals("flowers", art[0].title)
        assertEquals(ArtworkKind.Video, art[0].kind)
        assertEquals("https://cdn.pixabay.com/video/medium.mp4", art[0].remoteUrl)
    }

    @Test
    fun blankApiKeyUsesOfflineFallback() = runBlocking {
        var called = false
        val source = PixabayVideoSource(
            apiKey = "",
            offlineFallback = { emptyList() },
            httpGet = {
                called = true
                error("should not call")
            },
        )
        assertEquals(emptyList(), source.load())
        assertFalse(called)
    }
}
