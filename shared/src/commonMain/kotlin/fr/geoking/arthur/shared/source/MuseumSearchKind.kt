package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind

/**
 * Unified Control Plane museum facet (Painting / Sculpture chips).
 * Wire tokens for each Remote Source come from [RemoteCategoryMapping.museumParams].
 */
enum class MuseumSearchKind {
    All,
    Painting,
    Sculpture,
    ;

    val artworkKind: ArtworkKind?
        get() = when (this) {
            All -> null
            Painting -> ArtworkKind.Painting
            Sculpture -> ArtworkKind.Sculpture
        }

    companion object {
        fun fromArtworkKind(kind: ArtworkKind?): MuseumSearchKind = when (kind) {
            ArtworkKind.Sculpture -> Sculpture
            ArtworkKind.Painting -> Painting
            else -> All
        }
    }
}
