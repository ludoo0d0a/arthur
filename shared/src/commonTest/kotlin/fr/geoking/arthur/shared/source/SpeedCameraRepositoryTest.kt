package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCamera
import fr.geoking.arthur.shared.domain.SpeedCameraSource
import fr.geoking.arthur.shared.domain.SpeedCameraType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SpeedCameraRepositoryTest {

    private class FakeSpeedCameraSource(
        override val id: String,
        override val displayName: String,
        private val cameras: List<SpeedCamera>,
    ) : SpeedCameraSource {
        var callCount = 0

        override suspend fun getSpeedCamerasByCountry(countryCode: String): List<SpeedCamera> {
            callCount++
            return cameras.filter { it.countryCode.equals(countryCode, ignoreCase = true) }
        }

        override suspend fun getSpeedCamerasByBoundingBox(boundingBox: BoundingBox): List<SpeedCamera> {
            callCount++
            return cameras.filter { boundingBox.contains(it.latitude, it.longitude) }
        }
    }

    @Test
    fun returnsPrimarySourceAndCachesResult() = runBlocking {
        val primaryCam = SpeedCamera(
            id = "primary-1",
            latitude = 50.8503,
            longitude = 4.3517,
            countryCode = "BE",
            maxSpeed = 50,
            type = SpeedCameraType.Fixed,
            provider = "Primary",
        )
        val primary = FakeSpeedCameraSource("primary", "Primary", listOf(primaryCam))
        val fallback = FakeSpeedCameraSource("fallback", "Fallback", emptyList())

        val repository = SpeedCameraRepository(primarySource = primary, fallbackSource = fallback)

        // First call fetches from primary
        val firstResults = repository.getFixedRadarsByCountry("BE")
        assertEquals(1, firstResults.size)
        assertEquals("primary-1", firstResults[0].id)
        assertEquals(1, primary.callCount)
        assertEquals(0, fallback.callCount)

        // Second call hits cache
        val cachedResults = repository.getFixedRadarsByCountry("BE")
        assertEquals(1, cachedResults.size)
        assertEquals(1, primary.callCount)
    }

    @Test
    fun fallsBackToSecondaryWhenPrimaryIsEmpty() = runBlocking {
        val fallbackCam = SpeedCamera(
            id = "fallback-1",
            latitude = 49.6116,
            longitude = 6.1319,
            countryCode = "LU",
            maxSpeed = 90,
            type = SpeedCameraType.Fixed,
            provider = "Fallback",
        )
        val primary = FakeSpeedCameraSource("primary", "Primary", emptyList())
        val fallback = FakeSpeedCameraSource("fallback", "Fallback", listOf(fallbackCam))

        val repository = SpeedCameraRepository(primarySource = primary, fallbackSource = fallback)

        val results = repository.getFixedRadarsByCountry("LU")
        assertEquals(1, results.size)
        assertEquals("fallback-1", results[0].id)
        assertEquals(1, primary.callCount)
        assertEquals(1, fallback.callCount)
    }

    @Test
    fun deduplicatesRadarsByLocationAndId() = runBlocking {
        val cam1 = SpeedCamera(
            id = "cam-1",
            latitude = 50.85030,
            longitude = 4.35170,
            countryCode = "BE",
        )
        // Duplicate location (~same lat/lng to 4 decimal places)
        val cam1Dup = SpeedCamera(
            id = "cam-1-dup",
            latitude = 50.85031,
            longitude = 4.35171,
            countryCode = "BE",
        )
        val cam2 = SpeedCamera(
            id = "cam-2",
            latitude = 51.20000,
            longitude = 3.20000,
            countryCode = "BE",
        )

        val primary = FakeSpeedCameraSource("primary", "Primary", listOf(cam1, cam1Dup, cam2))
        val repository = SpeedCameraRepository(primarySource = primary)

        val deduplicated = repository.getFixedRadarsByCountry("BE")
        assertEquals(2, deduplicated.size)
        assertEquals("cam-1", deduplicated[0].id)
        assertEquals("cam-2", deduplicated[1].id)
    }
}
