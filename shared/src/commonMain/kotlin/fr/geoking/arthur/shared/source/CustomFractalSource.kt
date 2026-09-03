package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source

/**
 * Premium Custom Fractal artworks authored on the Control Plane.
 * Ids are self-describing (`customfractal.…`) so Auto/TV can bake/render without a blob.
 */
class CustomFractalSource(
    private val loadArtworks: suspend () -> List<Artwork> = { emptyList() },
) : Source {
    override val id: String = ID
    override val displayName: String = "Custom Fractal"

    override suspend fun load(): List<Artwork> = loadArtworks()

    companion object {
        const val ID = "customfractal"

        fun artwork(id: String, title: String): Artwork = Artwork(
            id = id,
            title = title,
            attribution = "Arthur Custom Fractal",
            sourceId = ID,
            kind = ArtworkKind.CustomFractal,
        )

        fun isCustomId(artworkId: String): Boolean = artworkId.startsWith("customfractal.")
    }
}
