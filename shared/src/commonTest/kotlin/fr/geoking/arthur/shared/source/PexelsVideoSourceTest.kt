package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

class PexelsVideoSourceTest {
    @Test
    fun loadsArtworkFromPexelsVideoFixtures() = runBlocking {
        val fixtures = mapOf(
            PexelsVideoSource.searchUrl() to """
                {
                  "videos": [
                    {
                      "id": 1448735,
                      "user": { "name": "Ruvim Miksanskiy" },
                      "video_files": [
                        {
                          "quality": "hd",
                          "width": 1920,
                          "height": 1080,
                          "link": "https://example.com/forest-hd.mp4"
                        },
                        {
                          "quality": "sd",
                          "width": 640,
                          "height": 360,
                          "link": "https://example.com/forest-sd.mp4"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = PexelsVideoSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("pexels-video-1448735", art[0].id)
        assertEquals(ArtworkKind.Video, art[0].kind)
        assertEquals("https://example.com/forest-hd.mp4", art[0].remoteUrl)
        assertEquals(PexelsVideoSource.ID, art[0].sourceId)
    }

    @Test
    fun blankApiKeyUsesOfflineFallback() = runBlocking {
        var called = false
        val cached = listOf(
            fr.geoking.arthur.shared.domain.Artwork(
                id = "pexels-video-1",
                title = "Cached",
                sourceId = PexelsVideoSource.ID,
                kind = ArtworkKind.Video,
                remoteUrl = "https://example.com/cached.mp4",
            ),
        )
        val source = PexelsVideoSource(
            apiKey = "",
            offlineFallback = { cached },
            httpGet = {
                called = true
                error("should not call")
            },
        )
        assertEquals(cached, source.load())
        assertFalse(called)
    }

    @Test
    fun searchUrlIncludesCategoryQuery() {
        assertEquals(
            "https://api.pexels.com/v1/videos/search?query=ocean&orientation=landscape&per_page=20",
            PexelsVideoSource.searchUrl("ocean"),
        )
    }

    @Test
    fun prefersLandscapeHdFile() {
        val url = PexelsVideoSource.pickVideoUrl(
            listOf(
                PexelsVideoFile(quality = "hd", width = 1080, height = 1920, link = "portrait.mp4"),
                PexelsVideoFile(quality = "hd", width = 1920, height = 1080, link = "landscape.mp4"),
                PexelsVideoFile(quality = "sd", width = 640, height = 360, link = "sd.mp4"),
            ),
        )
        assertEquals("landscape.mp4", url)
    }
}
