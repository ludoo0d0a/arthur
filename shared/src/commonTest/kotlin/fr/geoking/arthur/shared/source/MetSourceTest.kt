package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class MetSourceTest {
    @Test
    fun loadsArtworkFromMetFixtures() = runBlocking {
        val objectId = 436121
        val fixtures = mapOf(
            MetSource.SEARCH_URL to """
                {
                  "total": 1,
                  "objectIDs": [$objectId]
                }
            """.trimIndent(),
            MetSource.objectUrl(objectId) to """
                {
                  "objectID": $objectId,
                  "isPublicDomain": true,
                  "title": "Wheat Field with Cypresses",
                  "artistDisplayName": "Vincent van Gogh",
                  "primaryImage": "https://images.metmuseum.org/CRDImages/ep/original/DT1567.jpg",
                  "primaryImageSmall": "https://images.metmuseum.org/CRDImages/ep/web-large/DT1567.jpg"
                }
            """.trimIndent(),
        )
        val source = MetSource(httpGet = { url -> fixtures.getValue(url) })
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals("met-$objectId", art[0].id)
        assertEquals("Wheat Field with Cypresses", art[0].title)
        assertEquals("Vincent van Gogh", art[0].attribution)
        assertEquals(
            "https://images.metmuseum.org/CRDImages/ep/original/DT1567.jpg",
            art[0].remoteUrl,
        )
        assertEquals(MetSource.ID, art[0].sourceId)
    }

    @Test
    fun skipsNonPublicDomainOrMissingImage() = runBlocking {
        val fixtures = mapOf(
            MetSource.SEARCH_URL to """{"total":2,"objectIDs":[1,2]}""",
            MetSource.objectUrl(1) to """
                {
                  "objectID": 1,
                  "isPublicDomain": false,
                  "title": "Restricted",
                  "artistDisplayName": "Someone",
                  "primaryImage": "https://images.metmuseum.org/restricted.jpg"
                }
            """.trimIndent(),
            MetSource.objectUrl(2) to """
                {
                  "objectID": 2,
                  "isPublicDomain": true,
                  "title": "No Image",
                  "artistDisplayName": "Someone",
                  "primaryImage": "",
                  "primaryImageSmall": ""
                }
            """.trimIndent(),
        )
        val source = MetSource(httpGet = { url -> fixtures.getValue(url) })
        assertEquals(emptyList(), source.load())
    }

    @Test
    fun parseSearchIdsReadsObjectIDs() {
        val ids = MetSource.parseSearchIds("""{"total":2,"objectIDs":[10,20]}""")
        assertEquals(listOf(10, 20), ids)
    }

    @Test
    fun searchUrlIncludesCategoryMedium() {
        assertEquals(
            "https://collectionapi.metmuseum.org/public/collection/v1/search" +
                "?q=sculpture&medium=Sculpture&hasImages=true&isPublicDomain=true",
            MetSource.searchUrl(MuseumSearchKind.Sculpture),
        )
    }

    @Test
    fun loadsSculptureKindFromCategoryQuery() = runBlocking {
        val objectId = 200668
        val fixtures = mapOf(
            MetSource.searchUrl(MuseumSearchKind.Sculpture) to """
                {"total":1,"objectIDs":[$objectId]}
            """.trimIndent(),
            MetSource.objectUrl(objectId) to """
                {
                  "objectID": $objectId,
                  "isPublicDomain": true,
                  "title": "Marble Head",
                  "artistDisplayName": "Unknown",
                  "primaryImage": "https://images.metmuseum.org/sculpt.jpg"
                }
            """.trimIndent(),
        )
        val source = MetSource(
            httpGet = { url -> fixtures.getValue(url) },
            kind = { MuseumSearchKind.Sculpture },
        )
        val art = source.load()
        assertEquals(1, art.size)
        assertEquals(ArtworkKind.Sculpture, art[0].kind)
    }
}
