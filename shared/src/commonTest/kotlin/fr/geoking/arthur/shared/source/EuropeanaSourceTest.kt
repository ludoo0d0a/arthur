package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class EuropeanaSourceTest {
    @Test
    fun loadsArtworkFromEuropeanaFixtures() = runBlocking {
        val fixtures = mapOf(
            EuropeanaSource.searchUrl(limit = RemoteSample.SEARCH_POOL) to """
                {
                  "items": [
                    {
                      "id": "/9200365/BibliographicResource_3000118436166",
                      "title": ["Starry Night"],
                      "dcCreator": ["Vincent van Gogh"],
                      "dataProvider": ["Museum Example"],
                      "edmIsShownBy": ["https://example.com/starry.jpg"],
                      "edmPreview": ["https://example.com/starry-thumb.jpg"],
                      "type": "IMAGE"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = EuropeanaSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals(
            "europeana-9200365-BibliographicResource_3000118436166",
            art[0].id,
        )
        assertEquals("Starry Night", art[0].title)
        assertEquals("Vincent van Gogh / Museum Example / Europeana", art[0].attribution)
        assertEquals("https://example.com/starry.jpg", art[0].remoteUrl)
        assertEquals(ArtworkKind.Painting, art[0].kind)
        assertEquals(EuropeanaSource.ID, art[0].sourceId)
    }

    @Test
    fun blankApiKeyReturnsEmpty() = runBlocking {
        val source = EuropeanaSource(
            apiKey = "",
            httpGet = { error("should not call network") },
        )
        assertTrue(source.load().isEmpty())
    }

    @Test
    fun prefersIsShownByOverPreviewAndSkipsMissingImage() = runBlocking {
        val fixtures = mapOf(
            EuropeanaSource.searchUrl(limit = RemoteSample.SEARCH_POOL) to """
                {
                  "items": [
                    {
                      "id": "/a/1",
                      "title": ["No Image"],
                      "type": "IMAGE"
                    },
                    {
                      "id": "/a/2",
                      "title": ["Preview Only"],
                      "edmPreview": ["https://example.com/preview.jpg"]
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = EuropeanaSource(
            apiKey = "test-key",
            httpGet = { url -> fixtures.getValue(url) },
            random = ZeroRandom,
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("europeana-a-2", art[0].id)
        assertEquals("https://example.com/preview.jpg", art[0].remoteUrl)
    }

    @Test
    fun searchUrlIncludesOpenImageFilters() {
        assertEquals(
            "https://api.europeana.eu/record/v2/search.json" +
                "?query=sculpture&reusability=open&media=true&qf=TYPE%3AIMAGE" +
                "&rows=20&start=1&profile=standard",
            EuropeanaSource.searchUrl(kind = MuseumSearchKind.Sculpture),
        )
    }
}
