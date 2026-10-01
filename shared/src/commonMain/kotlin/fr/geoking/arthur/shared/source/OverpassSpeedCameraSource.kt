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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Speed camera provider querying OpenStreetMap via Overpass API.
 */
class OverpassSpeedCameraSource(
    private val httpPostOrGet: suspend (url: String, query: String?) -> String,
    private val errorLogger: ErrorLogger? = null,
) : SpeedCameraSource {

    override val id: String = ID
    override val displayName: String = "OpenStreetMap / Overpass"

    override suspend fun getSpeedCamerasByCountry(countryCode: String): List<SpeedCamera> {
        val normalizedCountry = countryCode.uppercase().trim()
        if (normalizedCountry.isBlank()) return emptyList()

        return runCatching {
            val query = buildCountryQuery(normalizedCountry)
            val jsonText = httpPostOrGet(OVERPASS_URL, query)
            parseOverpassJson(jsonText, defaultCountry = normalizedCountry)
        }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            errorLogger?.log(
                sourceId = id,
                category = ErrorClassifier.classify(null, e),
                message = "Failed to fetch Overpass radars for country $normalizedCountry",
                details = e.stackTraceToString().take(300),
                throwable = e,
            )
        }.getOrDefault(emptyList())
    }

    override suspend fun getSpeedCamerasByBoundingBox(boundingBox: BoundingBox): List<SpeedCamera> {
        return runCatching {
            val query = buildBboxQuery(boundingBox)
            val jsonText = httpPostOrGet(OVERPASS_URL, query)
            parseOverpassJson(jsonText, defaultCountry = "")
        }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            errorLogger?.log(
                sourceId = id,
                category = ErrorClassifier.classify(null, e),
                message = "Failed to fetch Overpass radars for bbox $boundingBox",
                details = e.stackTraceToString().take(300),
                throwable = e,
            )
        }.getOrDefault(emptyList())
    }

    internal fun parseOverpassJson(jsonText: String, defaultCountry: String): List<SpeedCamera> {
        if (jsonText.isBlank()) return emptyList()

        return runCatching {
            val rootElement = json.parseToJsonElement(jsonText).jsonObject
            val elements = rootElement["elements"]?.jsonArray ?: return emptyList()
            elements.mapNotNull { parseElement(it.jsonObject, defaultCountry) }
        }.getOrDefault(emptyList())
    }

    private fun parseElement(elementObj: JsonObject, defaultCountry: String): SpeedCamera? {
        val lat = elementObj["lat"]?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = elementObj["lon"]?.jsonPrimitive?.doubleOrNull ?: return null
        val nodeId = elementObj["id"]?.jsonPrimitive?.content ?: "${lat}_${lon}"

        val tags = elementObj["tags"]?.jsonObject ?: JsonObject(emptyMap())
        val rawMaxSpeed = tags["maxspeed"]?.jsonPrimitive?.content
        val speedLimit = rawMaxSpeed?.filter { it.isDigit() }?.toIntOrNull()

        val cameraTypeTag = tags["camera:type"]?.jsonPrimitive?.content
            ?: tags["enforcement"]?.jsonPrimitive?.content
            ?: tags["highway"]?.jsonPrimitive?.content
            ?: "speed_camera"

        val road = tags["ref"]?.jsonPrimitive?.content
            ?: tags["name"]?.jsonPrimitive?.content
            ?: tags["description"]?.jsonPrimitive?.content
            ?: ""

        val direction = tags["direction"]?.jsonPrimitive?.content
            ?: tags["camera:direction"]?.jsonPrimitive?.content
            ?: ""

        val country = tags["addr:country"]?.jsonPrimitive?.content
            ?: defaultCountry

        return SpeedCamera(
            id = "osm-$nodeId",
            latitude = lat,
            longitude = lon,
            countryCode = country.uppercase(),
            maxSpeed = speedLimit,
            type = parseType(cameraTypeTag),
            road = road,
            direction = direction,
            provider = displayName,
        )
    }

    private fun parseType(raw: String): SpeedCameraType {
        val lower = raw.lowercase()
        return when {
            lower.contains("section") || lower.contains("distance") || lower.contains("average") -> SpeedCameraType.Section
            lower.contains("red") || lower.contains("signal") || lower.contains("light") -> SpeedCameraType.RedLight
            lower.contains("construction") || lower.contains("work") -> SpeedCameraType.Construction
            else -> SpeedCameraType.Fixed
        }
    }

    companion object {
        const val ID = "overpass"
        const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun buildCountryQuery(countryCode: String): String =
            """[out:json][timeout:25];area["ISO3166-1"="${countryCode.uppercase()}"]["admin_level"="2"]->.searchArea;node["highway"="speed_camera"](area.searchArea);out body;"""

        fun buildBboxQuery(bbox: BoundingBox): String =
            """[out:json][timeout:25];node["highway"="speed_camera"](${bbox.minLat},${bbox.minLng},${bbox.maxLat},${bbox.maxLng});out body;"""
    }
}
