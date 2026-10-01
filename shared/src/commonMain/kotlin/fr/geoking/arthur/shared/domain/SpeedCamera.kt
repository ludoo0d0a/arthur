package fr.geoking.arthur.shared.domain

import kotlinx.serialization.Serializable

/**
 * Type of fixed speed enforcement radar.
 */
@Serializable
enum class SpeedCameraType {
    Fixed,
    Section,
    RedLight,
    Distance,
    Construction,
    Unknown,
}

/**
 * Geographic bounding box defined by minimum and maximum latitude/longitude.
 */
@Serializable
data class BoundingBox(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double,
) {
    fun contains(latitude: Double, longitude: Double): Boolean {
        return latitude in minLat..maxLat && longitude in minLng..maxLng
    }
}

/**
 * Represents a fixed speed camera record in Europe.
 */
@Serializable
data class SpeedCamera(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    /** ISO 3166-1 alpha-2 country code (e.g., "BE", "LU", "DE", "ES", "PT", "CH", "FR"). */
    val countryCode: String,
    /** Maximum allowed speed in km/h, if known (e.g., 50, 80, 110, 120, 130). Null if unspecified. */
    val maxSpeed: Int? = null,
    /** Type of camera (fixed, section/tronçon, red light, etc.). */
    val type: SpeedCameraType = SpeedCameraType.Fixed,
    /** Road name or location description (e.g. "A1 - Km 42", "N4", "Rue de la Loi"). */
    val road: String = "",
    /** Orientation or direction of camera enforcement in degrees or description (e.g. "North", "180", "Towards Brussels"). */
    val direction: String = "",
    /** Data provider name (e.g., "Lufop", "Overpass/OSM"). */
    val provider: String = "",
)

/**
 * Interface for fixed speed camera data sources.
 */
interface SpeedCameraSource {
    val id: String
    val displayName: String

    /**
     * Fetch fixed speed cameras for a given ISO 3166-1 alpha-2 country code (e.g. "BE", "LU", "DE", "ES", "PT", "CH", "FR").
     */
    suspend fun getSpeedCamerasByCountry(countryCode: String): List<SpeedCamera>

    /**
     * Fetch fixed speed cameras within a geographic bounding box.
     */
    suspend fun getSpeedCamerasByBoundingBox(boundingBox: BoundingBox): List<SpeedCamera>
}
