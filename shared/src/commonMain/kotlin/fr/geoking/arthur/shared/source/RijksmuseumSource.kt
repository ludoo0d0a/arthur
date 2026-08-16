package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Rijksmuseum Remote Source — parses collection JSON into Artwork.
 * Network I/O is injected so unit tests use fixtures.
 */
class RijksmuseumSource(
    private val fetchCollectionJson: suspend () -> String,
) : Source {
    override val id: String = ID
    override val displayName: String = "Rijksmuseum"

    override suspend fun load(): List<Artwork> {
        val payload = json.decodeFromString<RijksCollectionResponse>(fetchCollectionJson())
        return payload.artObjects.map { obj ->
            Artwork(
                id = "rijks-${obj.objectNumber}",
                title = obj.title,
                attribution = obj.principalOrFirstMaker ?: "Rijksmuseum",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = obj.webImage?.url,
            )
        }
    }

    companion object {
        const val ID = "rijksmuseum"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
data class RijksCollectionResponse(
    val artObjects: List<RijksArtObject> = emptyList(),
)

@Serializable
data class RijksArtObject(
    val objectNumber: String,
    val title: String,
    val principalOrFirstMaker: String? = null,
    val webImage: RijksWebImage? = null,
)

@Serializable
data class RijksWebImage(
    val url: String? = null,
)
