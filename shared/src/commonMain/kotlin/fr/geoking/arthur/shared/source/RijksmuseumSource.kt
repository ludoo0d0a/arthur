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
        val searchJson = httpGet(SEARCH_URL)
        val ids = parseSearchIds(searchJson).take(limit)
        ids.mapNotNull { objectId ->
            runCatching { loadArtwork(objectId) }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private suspend fun loadArtwork(objectId: String): Artwork {
        val root = json.parseToJsonElement(httpGet(objectId)).jsonObject
        val objectNumber = identifierContent(root, OBJECT_NUMBER_TYPE)
            ?: objectId.substringAfterLast('/')
        val title = firstName(root) ?: objectNumber
        val attribution = producerName(root) ?: "Rijksmuseum"
        return Artwork(
            id = "rijks-$objectNumber",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = ArtworkKind.Painting,
        )
    }

    companion object {
        const val ID = "rijksmuseum"
        const val DEFAULT_LIMIT = 8
        const val SEARCH_URL =
            "https://data.rijksmuseum.nl/search/collection?type=painting&imageAvailable=true"

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
