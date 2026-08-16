package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source

/** Small offline set shipped with the app. */
class BundledPackSource(
    private val items: List<Artwork> = defaultPack(),
) : Source {
    override val id: String = ID
    override val displayName: String = "Bundled pack"

    override suspend fun load(): List<Artwork> = items

    companion object {
        const val ID = "bundled"

        fun defaultPack(): List<Artwork> = listOf(
            Artwork(
                id = "bundled-1",
                title = "Study in Blue",
                attribution = "Arthur Bundled Pack",
                sourceId = ID,
                kind = ArtworkKind.Painting,
            ),
            Artwork(
                id = "bundled-2",
                title = "Marble Light",
                attribution = "Arthur Bundled Pack",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
            ),
            Artwork(
                id = "bundled-3",
                title = "Harbor Grain",
                attribution = "Arthur Bundled Pack",
                sourceId = ID,
                kind = ArtworkKind.Photo,
            ),
        )
    }
}
