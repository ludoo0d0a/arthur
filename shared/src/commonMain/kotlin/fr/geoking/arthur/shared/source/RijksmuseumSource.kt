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
 *
 * Image URLs often live behind `shows` → VisualItem → DigitalObject → `access_point`
 * rather than inline `representation`.
 */
class RijksmuseumSource(
    private val httpGet: suspend (url: String) -> String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.All },
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Rijksmuseum"

    override suspend fun load(): List<Artwork> = runCatching {
        val searchTargets = searchTargetsFor(kind())
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

    private suspend fun loadArtwork(
        objectId: String,
        defaultKind: ArtworkKind = ArtworkKind.Painting,
    ): Artwork? {
        val root = json.parseToJsonElement(httpGet(objectId)).jsonObject
        val objectNumber = identifierContent(root, OBJECT_NUMBER_TYPE)
            ?: objectId.substringAfterLast('/')
        val title = firstName(root) ?: objectNumber
        val attribution = producerName(root) ?: "Rijksmuseum"
        val imageUrl = imageUrl(root)?.takeIf { it.isNotBlank() } ?: return null
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
        val classified = root["classified_as"]?.jsonArray
            ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .orEmpty()
        for (entry in classified) {
            val id = entry["id"]?.jsonPrimitive?.contentOrNull ?: continue
            when {
                id.contains("220297") || id.contains("2201048") -> return ArtworkKind.Sculpture
                id.contains("220951") -> return ArtworkKind.Photo
                id.contains("2208") -> return ArtworkKind.Painting
            }
        }
        return defaultKind
    }

    private suspend fun imageUrl(root: JsonObject): String? {
        root["representation"]?.jsonArray?.forEach { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
            httpUrl(obj["id"]?.jsonPrimitive?.contentOrNull)?.let { return it }
            accessPointUrl(obj)?.let { return it }
        }
        root["shows"]?.jsonArray?.forEach { element ->
            val showId = httpUrl(
                runCatching { element.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull(),
            ) ?: return@forEach
            runCatching {
                val vitem = json.parseToJsonElement(httpGet(showId)).jsonObject
                vitem["digitally_shown_by"]?.jsonArray?.forEach { digitalRef ->
                    val dObj = runCatching { digitalRef.jsonObject }.getOrNull() ?: return@forEach
                    accessPointUrl(dObj)?.let { return it }
                    val dobjId = httpUrl(dObj["id"]?.jsonPrimitive?.contentOrNull) ?: return@forEach
                    val dobj = json.parseToJsonElement(httpGet(dobjId)).jsonObject
                    accessPointUrl(dobj)?.let { return it }
                }
            }
        }
        root["subject_of"]?.jsonArray?.forEach { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
            accessPointUrl(obj)?.let { return it }
        }
        return null
    }

    private fun accessPointUrl(obj: JsonObject): String? {
        obj["digitally_shown_by"]?.jsonArray?.forEach { digitalObj ->
            val dObj = runCatching { digitalObj.jsonObject }.getOrNull() ?: return@forEach
            dObj["access_point"]?.jsonArray?.forEach { accessPoint ->
                httpUrl(
                    runCatching { accessPoint.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull(),
                )?.let { return it }
            }
        }
        obj["access_point"]?.jsonArray?.forEach { accessPoint ->
            httpUrl(
                runCatching { accessPoint.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull(),
            )?.let { return it }
        }
        return null
    }

    companion object {
        const val ID = "rijksmuseum"
        const val DEFAULT_LIMIT = 20
        const val SEARCH_URL =
            "https://data.rijksmuseum.nl/search/collection?type=painting&imageAvailable=true"
        const val SEARCH_SCULPTURE_URL =
            "https://data.rijksmuseum.nl/search/collection?type=sculpture&imageAvailable=true"
        const val SEARCH_PHOTO_URL =
            "https://data.rijksmuseum.nl/search/collection?type=photograph&imageAvailable=true"

        private const val OBJECT_NUMBER_TYPE = "https://id.rijksmuseum.nl/22015218"
        private val json = Json { ignoreUnknownKeys = true }

        /** Linked Art Search: `type=painting|sculpture|photograph&imageAvailable=true`. */
        fun searchUrl(kind: MuseumSearchKind): String {
            val type = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Rijksmuseum).type
                ?: "painting"
            return "https://data.rijksmuseum.nl/search/collection?type=$type&imageAvailable=true"
        }

        private fun searchTargetsFor(
            kind: MuseumSearchKind,
        ): List<Pair<String, ArtworkKind>> = when (kind) {
            MuseumSearchKind.All -> listOf(
                SEARCH_URL to ArtworkKind.Painting,
                SEARCH_SCULPTURE_URL to ArtworkKind.Sculpture,
                SEARCH_PHOTO_URL to ArtworkKind.Photo,
            )
            MuseumSearchKind.Painting -> listOf(
                searchUrl(MuseumSearchKind.Painting) to ArtworkKind.Painting,
            )
            MuseumSearchKind.Sculpture -> listOf(
                searchUrl(MuseumSearchKind.Sculpture) to ArtworkKind.Sculpture,
            )
            MuseumSearchKind.Photo -> listOf(
                searchUrl(MuseumSearchKind.Photo) to ArtworkKind.Photo,
            )
        }

        internal fun parseSearchIds(payload: String): List<String> {
            val page = json.decodeFromString<LinkedArtSearchPage>(payload)
            return page.orderedItems.mapNotNull { it.id }
        }

        private fun httpUrl(value: String?): String? =
            value?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

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
