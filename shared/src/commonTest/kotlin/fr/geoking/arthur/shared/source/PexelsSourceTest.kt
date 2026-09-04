package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PexelsSourceTest {
    @Test
    fun loadsArtworkFromPexelsFixtures() = runBlocking {
        val fixtures = mapOf(
            PexelsSource.searchUrl() to """
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
    fun blankApiKeySkipsNetwork() = runBlocking {
        var called = false
        val source = PexelsSource(
            apiKey = "",
            httpGet = {
                called = true
                error("should not call")
            },
        )
        assertEquals(emptyList(), source.load())
        assertEquals(false, called)
    }

    @Test
    fun skipsPhotosWithoutImageUrl() = runBlocking {
        val fixtures = mapOf(
            PexelsSource.searchUrl() to """
                {"photos":[{"id":1,"alt":"No src","photographer":"X","src":{}}]}
            """.trimIndent(),
        )
        val source = PexelsSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
        )
        assertEquals(emptyList(), source.load())
    }
}
