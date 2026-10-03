package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SmithsonianSourceTest {
    @Test
    fun loadsCc0ArtworkFromFixtures() = runBlocking {
        val url = SmithsonianSource.searchUrl(
            apiKey = "test-key",
            limit = RemoteSample.SEARCH_POOL,
            sort = "random",
        )
        val fixtures = mapOf(
            url to """
                {
                  "response": {
                    "rows": [
                      {
                        "id": "row-1",
                        "title": "Painted Trillium",
                        "url": "edanmdm:saam_1970.355.472",
                        "content": {
                          "freetext": {
                            "name": [
                              { "label": "Artist", "content": "Mary Vaux Walcott" }
                            ]
                          },
                          "descriptiveNonRepeating": {
                            "title": { "label": "Title", "content": "Painted Trillium" },
                            "data_source": "Smithsonian American Art Museum",
                            "metadata_usage": { "access": "CC0" },
                            "online_media": {
                              "media": [
                                {
                                  "type": "Images",
                                  "usage": { "access": "CC0" },
                                  "content": "https://ids.si.edu/ids/deliveryService?id=SAAM-1",
                                  "resources": [
                                    {
                                      "label": "Screen Image",
                                      "url": "https://ids.si.edu/ids/download?id=SAAM-1_screen"
                                    }
                                  ]
                                }
                              ]
                            }
                          }
                        }
                      }
                    ]
                  }
                }
            """.trimIndent(),
        )
        val source = SmithsonianSource(
            apiKey = "test-key",
            httpGet = { fixtures.getValue(it) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("smithsonian-edanmdm-saam_1970.355.472", art[0].id)
        assertEquals("Painted Trillium", art[0].title)
        assertEquals(
            "Mary Vaux Walcott / Smithsonian American Art Museum",
            art[0].attribution,
        )
        assertEquals(
            "https://ids.si.edu/ids/download?id=SAAM-1_screen",
            art[0].remoteUrl,
        )
        assertEquals(ArtworkKind.Painting, art[0].kind)
        assertEquals(SmithsonianSource.ID, art[0].sourceId)
    }

    @Test
    fun blankApiKeyReturnsEmpty() = runBlocking {
        val source = SmithsonianSource(
            apiKey = "",
            httpGet = { error("no network") },
        )
        assertTrue(source.load().isEmpty())
    }

    @Test
    fun searchUrlEncodesQuery() {
        val url = SmithsonianSource.searchUrl(
            apiKey = "k",
            kind = MuseumSearchKind.Sculpture,
            sort = "random",
        )
        assertTrue(url.contains("api_key=k"))
        assertTrue(url.contains("sculpture"))
        assertTrue(url.contains("online_media_type%3AImages"))
        assertTrue(url.contains("start=0"))
        assertTrue(url.contains("sort=random"))
        assertFalse(url.contains("unit_code"))
    }

    @Test
    fun rethrowsCancellationExceptionWithoutLoggingError() = runBlocking {
        val errorLogger = fr.geoking.arthur.shared.error.ErrorLogger()
        val source = SmithsonianSource(
            apiKey = "test-key",
            httpGet = { throw kotlinx.coroutines.CancellationException("Scope left composition") },
            random = ZeroRandom,
            errorLogger = errorLogger,
        )
        try {
            source.load()
            kotlin.test.fail("Expected CancellationException to be thrown")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertEquals("Scope left composition", e.message)
        }
        assertEquals(0, errorLogger.errors.value.size)
    }
}
