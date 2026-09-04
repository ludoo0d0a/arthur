package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Rijksmuseum Remote Source via the public Linked Art Search API (no API key).
 * [httpGet] is injected so unit tests use fixtures.
 */
class RijksmuseumSource(
    private val httpGet: suspend (url: String) -> String,
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Rijksmuseum"

    override suspend fun load(): List<Artwork> = runCatching {
        val searchTargets = listOf(
            SEARCH_URL to ArtworkKind.Painting,
            SEARCH_SCULPTURE_URL to ArtworkKind.Sculpture,
            SEARCH_PHOTO_URL to ArtworkKind.Photo,
        )
        val perTypeLimit = (limit / searchTargets.size).coerceAtLeast(2)
        val results = mutableListOf<Artwork>()

        for ((searchUrl, fallbackKind) in searchTargets) {
            val searchJson = runCatching { httpGet(searchUrl) }.getOrNull() ?: continue
            val ids = parseSearchIds(searchJson).take(perTypeLimit)
            for (objectId in ids) {
                val art = runCatching { loadArtwork(objectId, fallbackKind) }.getOrNull()
                if (art != null) {
                    results.add(art)
                }
            }
        }
        results.take(limit)
    }.getOrDefault(emptyList())

    private suspend fun loadArtwork(objectId: String, defaultKind: ArtworkKind = ArtworkKind.Painting): Artwork {
        val root = json.parseToJsonElement(httpGet(objectId)).jsonObject
        val objectNumber = identifierContent(root, OBJECT_NUMBER_TYPE)
            ?: objectId.substringAfterLast('/')
        val title = firstName(root) ?: objectNumber
        val attribution = producerName(root) ?: "Rijksmuseum"
        val imageUrl = imageUrl(root)
        val kind = parseKind(root, defaultKind)
        return Artwork(
            id = "rijks-$objectNumber",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = kind,
            remoteUrl = imageUrl,
        )
    }

    private fun parseKind(root: JsonObject, defaultKind: ArtworkKind): ArtworkKind {
        val classified = root["classified_as"]?.jsonArray?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }.orEmpty()
        for (entry in classified) {
            val id = entry["id"]?.jsonPrimitive?.contentOrNull
            if (id != null) {
                if (id.contains("220297") || id.contains("2201048")) return ArtworkKind.Sculpture
                if (id.contains("220951")) return ArtworkKind.Photo
                if (id.contains("2208")) return ArtworkKind.Painting
            }
        }
        return defaultKind
    }

    private suspend fun imageUrl(root: JsonObject): String? {
        root["representation"]?.jsonArray?.forEach { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
            val directId = obj["id"]?.jsonPrimitive?.contentOrNull
            if (directId != null && (directId.startsWith("http://") || directId.startsWith("https://"))) {
                return directId
            }
            obj["digitally_shown_by"]?.jsonArray?.forEach { digitalObj ->
                val dObj = runCatching { digitalObj.jsonObject }.getOrNull() ?: return@forEach
                dObj["access_point"]?.jsonArray?.forEach { accessPoint ->
                    val apId = runCatching { accessPoint.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull()
                    if (apId != null && (apId.startsWith("http://") || apId.startsWith("https://"))) {
                        return apId
                    }
                }
            }
        }
        root["shows"]?.jsonArray?.forEach { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
            val showId = obj["id"]?.jsonPrimitive?.contentOrNull
            if (showId != null && (showId.startsWith("http://") || showId.startsWith("https://"))) {
                runCatching {
                    val vitemJson = httpGet(showId)
                    val vitem = json.parseToJsonElement(vitemJson).jsonObject
                    vitem["digitally_shown_by"]?.jsonArray?.forEach { digitalObj ->
                        val dObj = runCatching { digitalObj.jsonObject }.getOrNull() ?: return@forEach
                        val dobjId = dObj["id"]?.jsonPrimitive?.contentOrNull
                        if (dobjId != null && (dobjId.startsWith("http://") || dobjId.startsWith("https://"))) {
                            val dobjJson = httpGet(dobjId)
                            val dobj = json.parseToJsonElement(dobjJson).jsonObject
                            dobj["access_point"]?.jsonArray?.forEach { accessPoint ->
                                val apId = runCatching { accessPoint.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull()
                                if (apId != null && (apId.startsWith("http://") || apId.startsWith("https://"))) {
                                    return apId
                                }
                            }
                        }
                    }
                }
            }
        }
        root["subject_of"]?.jsonArray?.forEach { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
            obj["digitally_shown_by"]?.jsonArray?.forEach { digitalObj ->
                val dObj = runCatching { digitalObj.jsonObject }.getOrNull() ?: return@forEach
                dObj["access_point"]?.jsonArray?.forEach { accessPoint ->
                    val apId = runCatching { accessPoint.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull()
                    if (apId != null && (apId.startsWith("http://") || apId.startsWith("https://"))) {
                        return apId
                    }
                }
            }
        }
        return null
    }

    companion object {
        const val ID = "rijksmuseum"
        const val DEFAULT_LIMIT = 8
        const val SEARCH_URL =
            "https://data.rijksmuseum.nl/search/collection?type=painting&imageAvailable=true"
        const val SEARCH_SCULPTURE_URL =
            "https://data.rijksmuseum.nl/search/collection?type=sculpture&imageAvailable=true"
        const val SEARCH_PHOTO_URL =
            "https://data.rijksmuseum.nl/search/collection?type=photograph&imageAvailable=true"

        private const val OBJECT_NUMBER_TYPE = "https://id.rijksmuseum.nl/22015218"
        private val json = Json { ignoreUnknownKeys = true }

        internal fun parseSearchIds(payload: String): List<String> {
            val page = json.decodeFromString<LinkedArtSearchPage>(payload)
            return page.orderedItems.mapNotNull { it.id }
        }

        private fun firstName(root: JsonObject): String? =
            root["identified_by"]?.jsonArray
                ?.mapNotNull { it.jsonObject }
                ?.firstOrNull { it["type"]?.jsonPrimitive?.contentOrNull == "Name" }
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull

        private fun identifierContent(root: JsonObject, typeId: String): String? =
            root["identified_by"]?.jsonArray
                ?.mapNotNull { it.jsonObject }
                ?.firstOrNull { entry ->
                    val isIdentifier = entry["type"]?.jsonPrimitive?.contentOrNull == "Identifier"
                    val hasType = entry["classified_as"]?.jsonArray.orEmpty().any { c ->
                        c.jsonObject["id"]?.jsonPrimitive?.contentOrNull == typeId
                    }
                    isIdentifier && hasType
                }
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull

        private fun producerName(root: JsonObject): String? =
            root["produced_by"]?.jsonObject
                ?.get("referred_to_by")
                ?.jsonArray
                ?.mapNotNull { it.jsonObject }
                ?.firstOrNull { it["content"] != null }
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
    }
}

@Serializable
internal data class LinkedArtSearchPage(
    val orderedItems: List<LinkedArtRef> = emptyList(),
)

@Serializable
internal data class LinkedArtRef(
    val id: String? = null,
    val type: String? = null,
)
