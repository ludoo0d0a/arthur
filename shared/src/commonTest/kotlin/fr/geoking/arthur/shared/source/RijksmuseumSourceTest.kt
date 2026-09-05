package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class RijksmuseumSourceTest {
    private fun fixtureGet(fixtures: Map<String, String>): suspend (String) -> String = { url ->
        fixtures[url] ?: """{"orderedItems":[]}"""
    }

    @Test
    fun loadsArtworkFromLinkedArtFixtures() = runBlocking {
        val fixtures = mapOf(
            RijksmuseumSource.SEARCH_URL to """
                {
                  "orderedItems": [
                    { "id": "https://id.rijksmuseum.nl/200100988", "type": "HumanMadeObject" }
                  ]
                }
            """.trimIndent(),
            "https://id.rijksmuseum.nl/200100988" to """
                {
                  "id": "https://id.rijksmuseum.nl/200100988",
                  "type": "HumanMadeObject",
                  "identified_by": [
                    { "type": "Name", "content": "Misty Sea" },
                    {
                      "type": "Identifier",
                      "content": "SK-C-1726",
                      "classified_as": [
                        { "id": "https://id.rijksmuseum.nl/22015218", "type": "Type" }
                      ]
                    }
                  ],
                  "produced_by": {
                    "referred_to_by": [
                      { "type": "LinguisticObject", "content": "Jan Toorop" }
                    ]
                  },
                  "representation": [
                    {
                      "id": "https://lh3.googleusercontent.com/test-image.jpg"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = RijksmuseumSource(httpGet = fixtureGet(fixtures))
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("rijks-SK-C-1726", art[0].id)
        assertEquals("Misty Sea", art[0].title)
        assertEquals("Jan Toorop", art[0].attribution)
        assertEquals("https://lh3.googleusercontent.com/test-image.jpg", art[0].remoteUrl)
    }

    @Test
    fun loadsArtworkWithShowsAndSculptureKind() = runBlocking {
        val fixtures = mapOf(
            RijksmuseumSource.SEARCH_SCULPTURE_URL to """
                {
                  "orderedItems": [
                    { "id": "https://id.rijksmuseum.nl/200105975", "type": "HumanMadeObject" }
                  ]
                }
            """.trimIndent(),
            "https://id.rijksmuseum.nl/200105975" to """
                {
                  "id": "https://id.rijksmuseum.nl/200105975",
                  "type": "HumanMadeObject",
                  "identified_by": [
                    { "type": "Name", "content": "Uma" }
                  ],
                  "classified_as": [
                    { "id": "https://id.rijksmuseum.nl/220297", "type": "Type" }
                  ],
                  "shows": [
                    { "id": "https://id.rijksmuseum.nl/vitem1", "type": "VisualItem" }
                  ]
                }
            """.trimIndent(),
            "https://id.rijksmuseum.nl/vitem1" to """
                {
                  "id": "https://id.rijksmuseum.nl/vitem1",
                  "type": "VisualItem",
                  "digitally_shown_by": [
                    { "id": "https://id.rijksmuseum.nl/dobj1", "type": "DigitalObject" }
                  ]
                }
            """.trimIndent(),
            "https://id.rijksmuseum.nl/dobj1" to """
                {
                  "id": "https://id.rijksmuseum.nl/dobj1",
                  "type": "DigitalObject",
                  "access_point": [
                    { "id": "https://iiif.micr.io/tqMQL/full/max/0/default.jpg", "type": "DigitalObject" }
                  ]
                }
            """.trimIndent(),
        )
        val source = RijksmuseumSource(httpGet = fixtureGet(fixtures))
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("rijks-200105975", art[0].id)
        assertEquals("Uma", art[0].title)
        assertEquals(ArtworkKind.Sculpture, art[0].kind)
        assertEquals("https://iiif.micr.io/tqMQL/full/max/0/default.jpg", art[0].remoteUrl)
    }

    @Test
    fun skipsArtworkWithoutResolvableImage() = runBlocking {
        val fixtures = mapOf(
            RijksmuseumSource.SEARCH_URL to """
                {
                  "orderedItems": [
                    { "id": "https://id.rijksmuseum.nl/200100001", "type": "HumanMadeObject" }
                  ]
                }
            """.trimIndent(),
            "https://id.rijksmuseum.nl/200100001" to """
                {
                  "id": "https://id.rijksmuseum.nl/200100001",
                  "type": "HumanMadeObject",
                  "identified_by": [
                    { "type": "Name", "content": "No Image" }
                  ]
                }
            """.trimIndent(),
        )
        val source = RijksmuseumSource(httpGet = fixtureGet(fixtures))
        assertEquals(emptyList(), source.load())
    }

    @Test
    fun parseSearchIdsReadsOrderedItems() {
        val ids = RijksmuseumSource.parseSearchIds(
            """{"orderedItems":[{"id":"https://id.rijksmuseum.nl/1"},{"id":"https://id.rijksmuseum.nl/2"}]}""",
        )
        assertEquals(
            listOf("https://id.rijksmuseum.nl/1", "https://id.rijksmuseum.nl/2"),
            ids,
        )
    }
}
