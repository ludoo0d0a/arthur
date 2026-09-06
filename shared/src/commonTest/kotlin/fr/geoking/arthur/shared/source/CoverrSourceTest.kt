package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

class CoverrSourceTest {
    @Test
    fun loadsArtworkFromCoverrFixtures() = runBlocking {
        val fixtures = mapOf(
            CoverrSource.searchUrl() to """
                {
                  "hits": [
                    {
                      "id": "S1YbPl1NfI",
                      "title": "Ocean Waves",
                      "is_vertical": false,
                      "urls": { "mp4": "https://storage.coverr.co/videos/ocean.mp4?token=x" }
                    },
                    {
                      "id": "vertical1",
                      "title": "Portrait Clip",
                      "is_vertical": true,
                      "urls": { "mp4": "https://storage.coverr.co/videos/portrait.mp4?token=x" }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = CoverrSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("coverr-S1YbPl1NfI", art[0].id)
        assertEquals("Ocean Waves", art[0].title)
        assertEquals(ArtworkKind.Video, art[0].kind)
        assertEquals(CoverrSource.ID, art[0].sourceId)
    }

    @Test
    fun blankApiKeyUsesOfflineFallback() = runBlocking {
        var called = false
        val source = CoverrSource(
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

    @Test
    fun searchUrlRequestsSignedUrls() {
        assertEquals(
            "https://api.coverr.co/videos?query=nature&urls=true&page_size=20&sort=popular",
            CoverrSource.searchUrl("nature"),
        )
    }
}
