package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source

/**
 * Procedural genart engines as Content Engine Artwork.
 * Ids map 1:1 to `:genart` [GenartCatalog] / [GenartEngineId] names.
 */
class GenartSource(
    private val items: List<Artwork> = defaultCatalog(),
) : Source {
    override val id: String = ID
    override val displayName: String = "Genart"

    override suspend fun load(): List<Artwork> = items

    companion object {
        const val ID = "genart"

        const val PARTICLES = "genart.particles"
        const val PSEUDO3D = "genart.pseudo3d"
        const val SOFT_SHADOWS = "genart.softshadows"
        const val TUNNEL = "genart.tunnel"
        const val TONAL_GEOMETRY = "genart.tonalgeometry"

        fun defaultCatalog(): List<Artwork> = listOf(
            Artwork(
                id = PARTICLES,
                title = "Drifting Particles",
                attribution = "Arthur Genart",
                sourceId = ID,
                kind = ArtworkKind.Genart,
            ),
            Artwork(
                id = PSEUDO3D,
                title = "Wire Lattice",
                attribution = "Arthur Genart",
                sourceId = ID,
                kind = ArtworkKind.Genart,
            ),
            Artwork(
                id = SOFT_SHADOWS,
                title = "Soft Shadows",
                attribution = "Arthur Genart",
                sourceId = ID,
                kind = ArtworkKind.Genart,
            ),
            Artwork(
                id = TUNNEL,
                title = "Vanishing Tunnel",
                attribution = "Arthur Genart",
                sourceId = ID,
                kind = ArtworkKind.Genart,
            ),
            Artwork(
                id = TONAL_GEOMETRY,
                title = "Tonal Geometry",
                attribution = "Arthur Genart",
                sourceId = ID,
                kind = ArtworkKind.Genart,
            ),
        )
    }
}
