package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SmithsonianSourceTest {
    @Test
    fun loadsCc0ArtworkFromFixtures() = runBlocking {
        val url = SmithsonianSource.searchUrl(apiKey = "test-key")
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
        val url = SmithsonianSource.searchUrl(apiKey = "k", kind = MuseumSearchKind.Sculpture)
        assertTrue(url.contains("api_key=k"))
        assertTrue(url.contains("unit_code:SAAM"))
        assertTrue(url.contains("sculpture"))
        assertTrue(url.contains("online_media_type:Images"))
        assertTrue(url.contains("start=0"))
    }
}
