package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ClevelandSourceTest {
    @Test
    fun loadsArtworkFromClevelandFixtures() = runBlocking {
        val fixtures = mapOf(
            ClevelandSource.searchUrl() to """
                {
                  "data": [
                    {
                      "id": 94979,
                      "title": "Nathaniel Hurd",
                      "type": "Painting",
                      "share_license_status": "CC0",
                      "creators": [
                        {
                          "description": "John Singleton Copley (American, 1738–1815)",
                          "name": "John Singleton Copley"
                        }
                      ],
                      "images": {
                        "web": {
                          "url": "https://openaccess-cdn.clevelandart.org/1915.534/1915.534_web.jpg"
                        },
                        "print": {
                          "url": "https://openaccess-cdn.clevelandart.org/1915.534/1915.534_print.jpg"
                        }
                      }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = ClevelandSource(httpGet = { url -> fixtures.getValue(url) })
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("cleveland-94979", art[0].id)
        assertEquals("Nathaniel Hurd", art[0].title)
        assertEquals("John Singleton Copley (American, 1738–1815)", art[0].attribution)
        assertEquals(
            "https://openaccess-cdn.clevelandart.org/1915.534/1915.534_print.jpg",
            art[0].remoteUrl,
        )
        assertEquals(ArtworkKind.Painting, art[0].kind)
        assertEquals(ClevelandSource.ID, art[0].sourceId)
    }

    @Test
    fun skipsMissingImageAndPrefersWebWhenPrintAbsent() = runBlocking {
        val fixtures = mapOf(
            ClevelandSource.searchUrl() to """
                {
                  "data": [
                    {
                      "id": 1,
                      "title": "No Image",
                      "type": "Painting",
                      "images": {}
                    },
                    {
                      "id": 2,
                      "title": "Web Only",
                      "type": "Sculpture",
                      "creators": [{ "name": "Unknown" }],
                      "images": {
                        "web": {
                          "url": "https://openaccess-cdn.clevelandart.org/web.jpg"
                        }
                      }
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = ClevelandSource(httpGet = { url -> fixtures.getValue(url) })
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("cleveland-2", art[0].id)
        assertEquals(
            "https://openaccess-cdn.clevelandart.org/web.jpg",
            art[0].remoteUrl,
        )
        assertEquals(ArtworkKind.Sculpture, art[0].kind)
        assertEquals("Unknown", art[0].attribution)
    }
}
