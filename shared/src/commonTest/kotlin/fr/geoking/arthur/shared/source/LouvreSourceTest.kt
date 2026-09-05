package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class LouvreSourceTest {
    @Test
    fun loadsArtworkFromLouvreFixtures() = runBlocking {
        val ark = "cl010062370"
        val fixtures = mapOf(
            LouvreSource.objectUrl(ark) to """
                {
                  "arkId": "$ark",
                  "title": "La Joconde",
                  "creator": [{ "label": "Léonard de Vinci" }],
                  "collection": "Département des Peintures",
                  "image": [
                    {
                      "urlImage": "https://collections.louvre.fr/media/cache/large/joconde.jpg",
                      "copyright": "© Musée du Louvre",
                      "position": 0
                    }
                  ]
                }
            """.trimIndent(),
        )
        val source = LouvreSource(
            httpGet = { fixtures.getValue(it) },
            arkIds = { listOf(ark) },
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("louvre-$ark", art[0].id)
        assertEquals("La Joconde", art[0].title)
        assertEquals("Léonard de Vinci / Musée du Louvre", art[0].attribution)
        assertEquals(
            "https://collections.louvre.fr/media/cache/large/joconde.jpg",
            art[0].remoteUrl,
        )
        assertEquals(ArtworkKind.Painting, art[0].kind)
        assertEquals(LouvreSource.ID, art[0].sourceId)
    }

    @Test
    fun skipsMissingImage() = runBlocking {
        val ark = "cl010000001"
        val fixtures = mapOf(
            LouvreSource.objectUrl(ark) to """
                { "arkId": "$ark", "title": "No Image", "image": [] }
            """.trimIndent(),
        )
        val source = LouvreSource(
            httpGet = { fixtures.getValue(it) },
            arkIds = { listOf(ark) },
        )
        assertTrue(source.load().isEmpty())
    }

    @Test
    fun objectUrlUsesArkPath() {
        assertEquals(
            "https://collections.louvre.fr/ark:/53355/cl010062370.json",
            LouvreSource.objectUrl("cl010062370"),
        )
    }
}
