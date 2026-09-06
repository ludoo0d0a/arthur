package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class HarvardSourceTest {
    @Test
    fun loadsArtworkFromHarvardFixtures() = runBlocking {
        val url = HarvardSource.searchUrl(
            apiKey = "test-key",
            limit = RemoteSample.SEARCH_POOL,
        )
        val fixtures = mapOf(
            url to """
                {
                  "records": [
                    {
                      "id": 299843,
                      "title": "Self-Portrait Dedicated to Paul Gauguin",
                      "primaryimageurl": "https://nrs.harvard.edu/urn-3:HUAM:300024C_dynmc",
                      "people": [
                        { "role": "Artist", "displayname": "Vincent van Gogh" }
                      ],
                      "classification": "Paintings"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = HarvardSource(
            apiKey = "test-key",
            httpGet = { fixtures.getValue(it) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("harvard-299843", art[0].id)
        assertEquals("Self-Portrait Dedicated to Paul Gauguin", art[0].title)
        assertEquals("Vincent van Gogh / Harvard Art Museums", art[0].attribution)
        assertEquals(ArtworkKind.Painting, art[0].kind)
        assertEquals(HarvardSource.ID, art[0].sourceId)
    }

    @Test
    fun blankApiKeyReturnsEmpty() = runBlocking {
        val source = HarvardSource(
            apiKey = "",
            httpGet = { error("no network") },
        )
        assertTrue(source.load().isEmpty())
    }

    @Test
    fun searchUrlRequiresPublicImagePermission() {
        assertEquals(
            "https://api.harvardartmuseums.org/object" +
                "?apikey=k&classification=Photographs&hasimage=1&q=imagepermissionlevel%3A0&size=20" +
                "&page=1&sort=random&fields=id,title,primaryimageurl,people,classification,url,images",
            HarvardSource.searchUrl(apiKey = "k", kind = MuseumSearchKind.Photo),
        )
    }
}
