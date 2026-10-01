package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCameraType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class LufopSpeedCameraSourceTest {

    @Test
    fun parsesGeoJsonFeatureCollectionCorrectly() = runBlocking {
        val geoJsonPayload = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [4.3517, 50.8503]
                  },
                  "properties": {
                    "id": "1001",
                    "speed": 50,
                    "type": "fixe",
                    "country": "BE",
                    "road": "Rue de la Loi",
                    "direction": "East"
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [6.1319, 49.6116]
                  },
                  "properties": {
                    "id": "1002",
                    "speed": 90,
                    "type": "troncon",
                    "country": "LU",
                    "road": "A1"
                  }
                }
              ]
            }
        """.trimIndent()

        val source = LufopSpeedCameraSource(
            httpGet = { geoJsonPayload }
        )

        val cameras = source.getSpeedCamerasByCountry("BE")
        assertEquals(2, cameras.size)

        val beCamera = cameras[0]
        assertEquals("lufop-1001", beCamera.id)
        assertEquals(50.8503, beCamera.latitude)
        assertEquals(4.3517, beCamera.longitude)
        assertEquals("BE", beCamera.countryCode)
        assertEquals(50, beCamera.maxSpeed)
        assertEquals(SpeedCameraType.Fixed, beCamera.type)
        assertEquals("Rue de la Loi", beCamera.road)
        assertEquals("East", beCamera.direction)

        val luCamera = cameras[1]
        assertEquals("lufop-1002", luCamera.id)
        assertEquals("LU", luCamera.countryCode)
        assertEquals(90, luCamera.maxSpeed)
        assertEquals(SpeedCameraType.Section, luCamera.type)
        assertEquals("A1", luCamera.road)
    }

    @Test
    fun parsesJsonListCorrectly() = runBlocking {
        val jsonListPayload = """
            [
              {
                "id": "2001",
                "lat": 52.5200,
                "lng": 13.4050,
                "speed": 80,
                "type": "fixed",
                "country": "DE",
                "road": "B1"
              },
              {
                "id": "2002",
                "lat": 40.4168,
                "lng": -3.7038,
                "vitesse": 120,
                "type": "red_light",
                "country": "ES",
                "road": "M-30"
              }
            ]
        """.trimIndent()

        val source = LufopSpeedCameraSource(
            httpGet = { jsonListPayload }
        )

        val cameras = source.getSpeedCamerasByCountry("DE")
        assertEquals(2, cameras.size)

        val deCamera = cameras[0]
        assertEquals("lufop-2001", deCamera.id)
        assertEquals("DE", deCamera.countryCode)
        assertEquals(80, deCamera.maxSpeed)
        assertEquals(SpeedCameraType.Fixed, deCamera.type)

        val esCamera = cameras[1]
        assertEquals("lufop-2002", esCamera.id)
        assertEquals("ES", esCamera.countryCode)
        assertEquals(120, esCamera.maxSpeed)
        assertEquals(SpeedCameraType.RedLight, esCamera.type)
    }

    @Test
    fun filtersByBoundingBox() = runBlocking {
        val geoJsonPayload = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [4.35, 50.85] },
                  "properties": { "id": "in_bbox", "country": "BE" }
                },
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [10.0, 60.0] },
                  "properties": { "id": "out_of_bbox", "country": "NO" }
                }
              ]
            }
        """.trimIndent()

        val source = LufopSpeedCameraSource(
            httpGet = { geoJsonPayload }
        )

        val bbox = BoundingBox(minLat = 50.0, minLng = 4.0, maxLat = 51.0, maxLng = 5.0)
        val filtered = source.getSpeedCamerasByBoundingBox(bbox)

        assertEquals(1, filtered.size)
        assertEquals("lufop-in_bbox", filtered[0].id)
    }

    @Test
    fun blankCountryReturnsEmpty() = runBlocking {
        val source = LufopSpeedCameraSource(
            httpGet = { error("Should not reach network") }
        )
        val cameras = source.getSpeedCamerasByCountry("  ")
        assertTrue(cameras.isEmpty())
    }
}
