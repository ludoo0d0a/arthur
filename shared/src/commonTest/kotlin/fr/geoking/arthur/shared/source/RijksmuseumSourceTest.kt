package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class RijksmuseumSourceTest {
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
                  }
                }
            """.trimIndent(),
        )
        val source = RijksmuseumSource(httpGet = { url -> fixtures.getValue(url) })
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("rijks-SK-C-1726", art[0].id)
        assertEquals("Misty Sea", art[0].title)
        assertEquals("Jan Toorop", art[0].attribution)
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
