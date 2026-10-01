package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.BoundingBox
import fr.geoking.arthur.shared.domain.SpeedCamera
import fr.geoking.arthur.shared.domain.SpeedCameraSource
import fr.geoking.arthur.shared.domain.SpeedCameraType
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Speed camera provider for Lufop / OpenSpeedCam data.
 * Supports all European countries covered by Lufop (BE, LU, DE, ES, PT, CH, FR, IT, NL, AT, PL, GB, SE, NO, FI, etc.).
 */
class LufopSpeedCameraSource(
    private val httpGet: suspend (url: String) -> String,
    private val errorLogger: ErrorLogger? = null,
) : SpeedCameraSource {

    override val id: String = ID
    override val displayName: String = "Lufop / OpenSpeedCam"

    override suspend fun getSpeedCamerasByCountry(countryCode: String): List<SpeedCamera> {
        val normalizedCountry = countryCode.uppercase().trim()
        if (normalizedCountry.isBlank()) return emptyList()

        return runCatching {
            val url = countryUrl(normalizedCountry)
            val jsonText = httpGet(url)
            parseGeoJsonOrList(jsonText, defaultCountry = normalizedCountry)
        }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            errorLogger?.log(
                sourceId = id,
                category = ErrorClassifier.classify(null, e),
                message = "Failed to fetch Lufop radars for country $normalizedCountry",
                details = e.stackTraceToString().take(300),
                throwable = e,
            )
        }.getOrDefault(emptyList())
    }

    override suspend fun getSpeedCamerasByBoundingBox(boundingBox: BoundingBox): List<SpeedCamera> {
        return runCatching {
            val url = bboxUrl(boundingBox)
            val jsonText = httpGet(url)
            val allCameras = parseGeoJsonOrList(jsonText, defaultCountry = "")
            allCameras.filter { boundingBox.contains(it.latitude, it.longitude) }
        }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            errorLogger?.log(
                sourceId = id,
                category = ErrorClassifier.classify(null, e),
                message = "Failed to fetch Lufop radars for bbox $boundingBox",
                details = e.stackTraceToString().take(300),
                throwable = e,
            )
        }.getOrDefault(emptyList())
    }

    internal fun parseGeoJsonOrList(jsonText: String, defaultCountry: String): List<SpeedCamera> {
        if (jsonText.isBlank()) return emptyList()

        return runCatching {
            val rootElement = json.parseToJsonElement(jsonText)
            if (rootElement is JsonObject && rootElement["type"]?.jsonPrimitive?.content == "FeatureCollection") {
                val features = rootElement["features"]?.jsonArray ?: return emptyList()
                features.mapNotNull { parseFeature(it.jsonObject, defaultCountry) }
            } else if (rootElement is JsonObject && rootElement.containsKey("radars")) {
                val list = rootElement["radars"]?.jsonArray ?: return emptyList()
                list.mapNotNull { parseRadarObject(it.jsonObject, defaultCountry) }
            } else if (rootElement is kotlinx.serialization.json.JsonArray) {
                rootElement.mapNotNull { parseRadarObject(it.jsonObject, defaultCountry) }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseFeature(featureObj: JsonObject, defaultCountry: String): SpeedCamera? {
        val geometry = featureObj["geometry"]?.jsonObject ?: return null
        val coords = geometry["coordinates"]?.jsonArray ?: return null
        if (coords.size < 2) return null
        val lon = coords[0].jsonPrimitive.doubleOrNull ?: return null
        val lat = coords[1].jsonPrimitive.doubleOrNull ?: return null

        val props = featureObj["properties"]?.jsonObject ?: JsonObject(emptyMap())
        val rawId = props["id"]?.jsonPrimitive?.content
            ?: props["radar_id"]?.jsonPrimitive?.content
            ?: "${lat}_${lon}"

        val rawSpeed = props["speed"]?.jsonPrimitive?.intOrNull
            ?: props["vitesse"]?.jsonPrimitive?.intOrNull
            ?: props["maxspeed"]?.jsonPrimitive?.intOrNull

        val rawType = props["type"]?.jsonPrimitive?.content
            ?: props["radar_type"]?.jsonPrimitive?.content
            ?: "fixed"

        val country = props["country"]?.jsonPrimitive?.content
            ?: props["pays"]?.jsonPrimitive?.content
            ?: defaultCountry

        val road = props["road"]?.jsonPrimitive?.content
            ?: props["voie"]?.jsonPrimitive?.content
            ?: props["description"]?.jsonPrimitive?.content
            ?: ""

        val direction = props["direction"]?.jsonPrimitive?.content
            ?: props["orientation"]?.jsonPrimitive?.content
            ?: ""

        return SpeedCamera(
            id = "lufop-$rawId",
            latitude = lat,
            longitude = lon,
            countryCode = country.uppercase(),
            maxSpeed = rawSpeed,
            type = parseType(rawType),
            road = road,
            direction = direction,
            provider = displayName,
        )
    }

    private fun parseRadarObject(obj: JsonObject, defaultCountry: String): SpeedCamera? {
        val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
            ?: obj["latitude"]?.jsonPrimitive?.doubleOrNull
            ?: return null
        val lon = obj["lng"]?.jsonPrimitive?.doubleOrNull
            ?: obj["lon"]?.jsonPrimitive?.doubleOrNull
            ?: obj["longitude"]?.jsonPrimitive?.doubleOrNull
            ?: return null

        val rawId = obj["id"]?.jsonPrimitive?.content ?: "${lat}_${lon}"
        val rawSpeed = obj["speed"]?.jsonPrimitive?.intOrNull
            ?: obj["vitesse"]?.jsonPrimitive?.intOrNull
        val rawType = obj["type"]?.jsonPrimitive?.content ?: "fixed"
        val country = obj["country"]?.jsonPrimitive?.content ?: defaultCountry
        val road = obj["road"]?.jsonPrimitive?.content ?: obj["voie"]?.jsonPrimitive?.content ?: ""
        val direction = obj["direction"]?.jsonPrimitive?.content ?: ""

        return SpeedCamera(
            id = "lufop-$rawId",
            latitude = lat,
            longitude = lon,
            countryCode = country.uppercase(),
            maxSpeed = rawSpeed,
            type = parseType(rawType),
            road = road,
            direction = direction,
            provider = displayName,
        )
    }

    private fun parseType(raw: String): SpeedCameraType {
        val lower = raw.lowercase()
        return when {
            lower.contains("troncon") || lower.contains("section") || lower.contains("distance") -> SpeedCameraType.Section
            lower.contains("feu") || lower.contains("red_light") || lower.contains("signal") -> SpeedCameraType.RedLight
            lower.contains("chantier") || lower.contains("construction") -> SpeedCameraType.Construction
            lower.contains("fixe") || lower.contains("fixed") || lower.contains("speed_camera") -> SpeedCameraType.Fixed
            else -> SpeedCameraType.Fixed
        }
    }

    companion object {
        const val ID = "lufop"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** List of all European country codes supported by Lufop dataset. */
        val SUPPORTED_COUNTRIES = setOf(
            "BE", "LU", "DE", "ES", "PT", "CH", "FR", "IT", "NL", "AT",
            "PL", "GB", "SE", "NO", "FI", "CZ", "DK", "IE", "HR", "HU",
            "RO", "GR", "SK", "SI"
        )

        fun countryUrl(countryCode: String): String =
            "https://lufop.org/api/radars?country=${countryCode.uppercase()}"

        fun bboxUrl(bbox: BoundingBox): String =
            "https://lufop.org/api/radars?bbox=${bbox.minLat},${bbox.minLng},${bbox.maxLat},${bbox.maxLng}"
    }
}
