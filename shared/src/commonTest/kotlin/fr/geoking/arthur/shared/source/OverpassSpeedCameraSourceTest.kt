package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCameraType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class OverpassSpeedCameraSourceTest {

    @Test
    fun parsesOverpassJsonElementsCorrectly() = runBlocking {
        val overpassPayload = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 123456,
                  "lat": 38.7223,
                  "lon": -9.1393,
                  "tags": {
                    "highway": "speed_camera",
                    "maxspeed": "80",
                    "ref": "A2",
                    "direction": "south",
                    "addr:country": "PT"
                  }
                },
                {
                  "type": "node",
                  "id": 654321,
                  "lat": 46.2044,
                  "lon": 6.1432,
                  "tags": {
                    "highway": "speed_camera",
                    "maxspeed": "50 km/h",
                    "camera:type": "red_light",
                    "name": "Route de Chêne",
                    "addr:country": "CH"
                  }
                }
              ]
            }
        """.trimIndent()

        val source = OverpassSpeedCameraSource(
            httpPostOrGet = { _, _ -> overpassPayload }
        )

        val cameras = source.getSpeedCamerasByCountry("PT")
        assertEquals(2, cameras.size)

        val ptCamera = cameras[0]
        assertEquals("osm-123456", ptCamera.id)
        assertEquals(38.7223, ptCamera.latitude)
        assertEquals(-9.1393, ptCamera.longitude)
        assertEquals("PT", ptCamera.countryCode)
        assertEquals(80, ptCamera.maxSpeed)
        assertEquals(SpeedCameraType.Fixed, ptCamera.type)
        assertEquals("A2", ptCamera.road)
        assertEquals("south", ptCamera.direction)

        val chCamera = cameras[1]
        assertEquals("osm-654321", chCamera.id)
        assertEquals("CH", chCamera.countryCode)
        assertEquals(50, chCamera.maxSpeed)
        assertEquals(SpeedCameraType.RedLight, chCamera.type)
        assertEquals("Route de Chêne", chCamera.road)
    }

    @Test
    fun generatesCorrectQueries() {
        val countryQuery = OverpassSpeedCameraSource.buildCountryQuery("BE")
        assertTrue(countryQuery.contains("""area["ISO3166-1"="BE"]"""))

        val bbox = BoundingBox(minLat = 49.0, minLng = 5.0, maxLat = 50.0, maxLng = 6.0)
        val bboxQuery = OverpassSpeedCameraSource.buildBboxQuery(bbox)
        assertTrue(bboxQuery.contains("(49.0,5.0,50.0,6.0)"))
    }
}
