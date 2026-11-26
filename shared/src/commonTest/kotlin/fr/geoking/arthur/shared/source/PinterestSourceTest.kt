package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PinterestSourceTest {
    @Test
    fun loadsArtworkFromPinterestFixtures() = runBlocking {
        val fixtures = mapOf(
            PinterestSource.searchUrl() to """
                {
                  "items": [
                    {
                      "id": "1234567890",
                      "title": "Golden Hour Mountains",
                      "alt_text": "Peaks at sunset",
                      "link": "https://www.pinterest.com/pin/1234567890/",
                      "media": {
                        "media_type": "image",
                        "images": {
                          "1200x": {
                            "width": 1200,
                            "height": 800,
                            "url": "https://i.pinimg.com/1200x/ab/cd/ef/abcdef.jpg"
                          },
                          "600x": {
                            "width": 600,
                            "height": 400,
                            "url": "https://i.pinimg.com/600x/ab/cd/ef/abcdef.jpg"
                          }
                        }
                      }
                    }
                  ],
                  "bookmark": "next-page-token"
                }
            """.trimIndent(),
        )
        val source = PinterestSource(
            accessToken = "test-token",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("pinterest-1234567890", art[0].id)
        assertEquals("Golden Hour Mountains", art[0].title)
        assertEquals("Pinterest", art[0].attribution)
        assertEquals(
            "https://i.pinimg.com/1200x/ab/cd/ef/abcdef.jpg",
            art[0].remoteUrl,
        )
        assertEquals(PinterestSource.ID, art[0].sourceId)
        assertEquals("https://www.pinterest.com/pin/1234567890/", art[0].externalUrl)
    }

    @Test
    fun blankAccessTokenUsesOfflineFallback() = runBlocking {
        var called = false
        val cached = listOf(
            fr.geoking.arthur.shared.domain.Artwork(
                id = "pinterest-1",
                title = "Cached",
                sourceId = PinterestSource.ID,
                kind = fr.geoking.arthur.shared.domain.ArtworkKind.Photo,
                localPath = "/tmp/cached.jpg",
            ),
        )
        val source = PinterestSource(
            accessToken = "",
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
    fun searchUrlIncludesTermAndCountry() {
        assertEquals(
            "https://api.pinterest.com/v5/search/partner/pins" +
                "?term=ocean&country_code=US&limit=10",
            PinterestSource.searchUrl("ocean"),
        )
        assertEquals(
            "https://api.pinterest.com/v5/search/partner/pins" +
                "?term=street%20art&country_code=FR&limit=10&bookmark=tok",
            PinterestSource.searchUrl(
                term = "street art",
                countryCode = "FR",
                bookmark = "tok",
            ),
        )
    }

    @Test
    fun skipsPinsWithoutImageUrl() = runBlocking {
        val fixtures = mapOf(
            PinterestSource.searchUrl() to """
                {"items":[{"id":"1","title":"No media","media":{"media_type":"image"}}]}
            """.trimIndent(),
        )
        val source = PinterestSource(
            accessToken = "test-token",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        assertEquals(emptyList(), source.load())
    }

    @Test
    fun advancesBookmarkOnSecondLoad() = runBlocking {
        val firstUrl = PinterestSource.searchUrl()
        val secondUrl = PinterestSource.searchUrl(bookmark = "page-2")
        val fixtures = mapOf(
            firstUrl to """
                {
                  "items": [
                    {
                      "id": "1",
                      "title": "A",
                      "media": {
                        "images": {
                          "600x": {"url": "https://i.pinimg.com/600x/a.jpg"}
                        }
                      }
                    }
                  ],
                  "bookmark": "page-2"
                }
            """.trimIndent(),
            secondUrl to """
                {
                  "items": [
                    {
                      "id": "2",
                      "title": "B",
                      "media": {
                        "images": {
                          "600x": {"url": "https://i.pinimg.com/600x/b.jpg"}
                        }
                      }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = PinterestSource(
            accessToken = "test-token",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        assertEquals("pinterest-1", source.load().single().id)
        assertEquals("pinterest-2", source.load().single().id)
    }
}
