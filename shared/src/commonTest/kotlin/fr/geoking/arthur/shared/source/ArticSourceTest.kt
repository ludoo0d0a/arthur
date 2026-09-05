package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ArticSourceTest {
    @Test
    fun loadsArtworkFromArticFixtures() = runBlocking {
        val fixtures = mapOf(
            ArticSource.searchUrl() to """
                {
                  "data": [
                    {
                      "id": 27992,
                      "title": "A Sunday on La Grande Jatte — 1884",
                      "artist_display": "Georges Seurat (French, 1859–1891)",
                      "image_id": "2d484387-2509-5e8e-2c43-22f9981972eb",
                      "is_public_domain": true
                    }
                  ],
                  "config": { "iiif_url": "https://www.artic.edu/iiif/2" }
                }
            """.trimIndent(),
        )
        val source = ArticSource(httpGet = { url -> fixtures.getValue(url) })
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("artic-27992", art[0].id)
        assertEquals("A Sunday on La Grande Jatte — 1884", art[0].title)
        assertEquals("Georges Seurat (French, 1859–1891)", art[0].attribution)
        assertEquals(
            "https://www.artic.edu/iiif/2/2d484387-2509-5e8e-2c43-22f9981972eb/full/843,/0/default.jpg",
            art[0].remoteUrl,
        )
        assertEquals(ArticSource.ID, art[0].sourceId)
    }

    @Test
    fun skipsNonPublicDomainOrMissingImage() = runBlocking {
        val fixtures = mapOf(
            ArticSource.searchUrl() to """
                {
                  "data": [
                    {
                      "id": 1,
                      "title": "Restricted",
                      "artist_display": "Someone",
                      "image_id": "abc",
                      "is_public_domain": false
                    },
                    {
                      "id": 2,
                      "title": "No Image",
                      "artist_display": "Someone",
                      "image_id": null,
                      "is_public_domain": true
                    }
                  ],
                  "config": { "iiif_url": "https://www.artic.edu/iiif/2" }
                }
            """.trimIndent(),
        )
        val source = ArticSource(httpGet = { url -> fixtures.getValue(url) })
        assertEquals(emptyList(), source.load())
    }

    @Test
    fun iiifImageUrlUsesRecommendedSize() {
        assertEquals(
            "https://www.artic.edu/iiif/2/abc/full/843,/0/default.jpg",
            ArticSource.iiifImageUrl("https://www.artic.edu/iiif/2", "abc"),
        )
    }
}
