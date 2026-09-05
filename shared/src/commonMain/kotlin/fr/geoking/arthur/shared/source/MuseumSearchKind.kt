package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind

/**
 * Unified Control Plane museum facet (Painting / Sculpture / Photo).
 * Wire tokens for each Remote Source come from [RemoteCategoryMapping.museumParams].
 */
enum class MuseumSearchKind {
    All,
    Painting,
    Sculpture,
    Photo,
    ;

    val artworkKind: ArtworkKind?
        get() = when (this) {
            All -> null
            Painting -> ArtworkKind.Painting
            Sculpture -> ArtworkKind.Sculpture
            Photo -> ArtworkKind.Photo
        }

    companion object {
        fun fromArtworkKind(kind: ArtworkKind?): MuseumSearchKind = when (kind) {
            ArtworkKind.Sculpture -> Sculpture
            ArtworkKind.Painting -> Painting
            ArtworkKind.Photo -> Photo
            else -> All
        }
    }
}
