package fr.geoking.arthur.shared.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class RijksmuseumSourceTest {
    @Test
    fun parsesFixtureIntoArtwork() = runBlocking {
        val fixture = """
            {
              "artObjects": [
                {
                  "objectNumber": "SK-C-5",
                  "title": "The Night Watch",
                  "principalOrFirstMaker": "Rembrandt van Rijn",
                  "webImage": { "url": "https://example.com/nightwatch.jpg" }
                }
              ]
            }
        """.trimIndent()
        val source = RijksmuseumSource(fetchCollectionJson = { fixture })
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("rijks-SK-C-5", art[0].id)
        assertEquals("The Night Watch", art[0].title)
        assertEquals("Rembrandt van Rijn", art[0].attribution)
        assertEquals("https://example.com/nightwatch.jpg", art[0].remoteUrl)
    }
}
