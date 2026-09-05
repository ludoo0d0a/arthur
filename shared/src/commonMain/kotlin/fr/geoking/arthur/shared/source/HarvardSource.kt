package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Harvard Art Museums Remote Source (API key required).
 * Objects with images only; blank [apiKey] yields an empty catalog.
 *
 * Search: `GET /object?classification=…&hasimage=1&apikey=…`
 */
class HarvardSource(
    private val httpGet: suspend (url: String) -> String,
    private val apiKey: String,
    private val kind: () -> MuseumSearchKind = { MuseumSearchKind.Painting },
    private val limit: Int = DEFAULT_LIMIT,
) : Source {
    override val id: String = ID
    override val displayName: String = "Harvard Art Museums"

    override suspend fun load(): List<Artwork> {
        if (apiKey.isBlank()) return emptyList()
        return runCatching {
            val targets = RemoteCategoryMapping.museumTargets(kind())
            val perKind = (limit / targets.size).coerceAtLeast(1)
            val results = mutableListOf<Artwork>()
            for (target in targets) {
                val payload = httpGet(searchUrl(apiKey, perKind, target))
                val page = json.decodeFromString<HarvardObjectPage>(payload)
                page.records
                    .asSequence()
                    .mapNotNull { toArtwork(it, target) }
                    .take(perKind)
                    .forEach { results.add(it) }
            }
            results.take(limit)
        }.getOrDefault(emptyList())
    }

    private fun toArtwork(item: HarvardObject, searchKind: MuseumSearchKind): Artwork? {
        val objectId = item.id ?: return null
        val imageUrl = item.primaryImageUrl?.takeIf { it.isNotBlank() } ?: return null
        val title = item.title?.takeIf { it.isNotBlank() } ?: "Object $objectId"
        val artist = item.people
            ?.firstOrNull { it.role.equals("Artist", ignoreCase = true) || it.displayname != null }
            ?.displayname
            ?.takeIf { it.isNotBlank() }
        val attribution = listOfNotNull(artist, "Harvard Art Museums").joinToString(" / ")
        return Artwork(
            id = "harvard-$objectId",
            title = title,
            attribution = attribution,
            sourceId = ID,
            kind = searchKind.artworkKind ?: ArtworkKind.Painting,
            remoteUrl = imageUrl,
        )
    }

    companion object {
        const val ID = "harvard"
        const val DEFAULT_LIMIT = 20

        private val json = Json { ignoreUnknownKeys = true }

        fun searchUrl(
            apiKey: String,
            limit: Int = DEFAULT_LIMIT,
            kind: MuseumSearchKind = MuseumSearchKind.Painting,
        ): String {
            val classification = RemoteCategoryMapping.museumParams(kind, RemoteProvider.Harvard)
                .type
                .orEmpty()
            return "https://api.harvardartmuseums.org/object" +
                "?apikey=$apiKey" +
                "&classification=$classification" +
                "&hasimage=1" +
                "&size=$limit" +
                "&fields=id,title,primaryimageurl,people,classification"
        }
    }
}

@Serializable
internal data class HarvardObjectPage(
    val records: List<HarvardObject> = emptyList(),
)

@Serializable
internal data class HarvardObject(
    val id: Int? = null,
    val title: String? = null,
    @SerialName("primaryimageurl") val primaryImageUrl: String? = null,
    val people: List<HarvardPerson>? = null,
    val classification: String? = null,
)

@Serializable
internal data class HarvardPerson(
    val role: String? = null,
    val displayname: String? = null,
)
