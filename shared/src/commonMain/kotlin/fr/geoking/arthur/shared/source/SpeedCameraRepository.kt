package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCamera
import fr.geoking.arthur.shared.domain.SpeedCameraSource

/**
 * Repository responsible for retrieving and deduplicating fixed speed cameras from multiple sources.
 */
class SpeedCameraRepository(
    private val primarySource: SpeedCameraSource,
    private val fallbackSource: SpeedCameraSource? = null,
) {
    private val countryCache = mutableMapOf<String, List<SpeedCamera>>()

    /**
     * Retrieve fixed speed cameras for a given country code (e.g. "BE", "LU", "DE", "ES", "PT", "CH", "FR", etc.).
     */
    suspend fun getFixedRadarsByCountry(countryCode: String, forceRefresh: Boolean = false): List<SpeedCamera> {
        val normalizedCountry = countryCode.uppercase().trim()
        if (normalizedCountry.isBlank()) return emptyList()

        if (!forceRefresh && countryCache.containsKey(normalizedCountry)) {
            return countryCache[normalizedCountry].orEmpty()
        }

        var results = primarySource.getSpeedCamerasByCountry(normalizedCountry)
        if (results.isEmpty() && fallbackSource != null) {
            results = fallbackSource.getSpeedCamerasByCountry(normalizedCountry)
        }

        val deduplicated = deduplicateRadars(results)
        countryCache[normalizedCountry] = deduplicated
        return deduplicated
    }

    /**
     * Retrieve fixed speed cameras within a geographic bounding box.
     */
    suspend fun getFixedRadarsByBoundingBox(boundingBox: BoundingBox): List<SpeedCamera> {
        var results = primarySource.getSpeedCamerasByBoundingBox(boundingBox)
        if (results.isEmpty() && fallbackSource != null) {
            results = fallbackSource.getSpeedCamerasByBoundingBox(boundingBox)
        }
        return deduplicateRadars(results)
    }

    private fun deduplicateRadars(cameras: List<SpeedCamera>): List<SpeedCamera> {
        if (cameras.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<SpeedCamera>()

        for (camera in cameras) {
            // Deduplicate by ID or location key rounded to ~10 meters (4 decimal places)
            val key = "${camera.countryCode}_${(camera.latitude * 10000).toInt()}_${(camera.longitude * 10000).toInt()}"
            if (seen.add(key) && seen.add(camera.id)) {
                result.add(camera)
            }
        }
        return result
    }

    fun clearCache() {
        countryCache.clear()
    }
}
