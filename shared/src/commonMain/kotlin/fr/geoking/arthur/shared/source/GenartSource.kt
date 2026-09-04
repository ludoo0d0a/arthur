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
        const val SPHERE = "genart.sphere"
        const val WAVES = "genart.waves"
        const val MICRO = "genart.micro"
        const val SNOW = "genart.snow"
        const val GRASS = "genart.grass"
        const val BIRD_FLOCK = "genart.birdflock"
        const val MOUNTAINS = "genart.mountains"
        const val AURORA = "genart.aurora"
        const val POND_RIPPLES = "genart.pondripples"
        const val FALLING_LEAVES = "genart.fallingleaves"
        const val BREATH_CIRCLES = "genart.breathcircles"
        const val FIRE_EMBERS = "genart.fireembers"
        const val DUNES = "genart.dunes"
        const val CONSTELLATION = "genart.constellation"
        const val CLOUDS = "genart.clouds"
        const val RAIN = "genart.rain"
        const val FOG = "genart.fog"
        const val FISH_SCHOOL = "genart.fishschool"
        const val FIREFLIES = "genart.fireflies"
        const val SUNBEAMS = "genart.sunbeams"
        const val METEORS = "genart.meteors"
        const val BUBBLES = "genart.bubbles"
        const val CHERRY_BLOSSOMS = "genart.cherryblossoms"
        const val RIBBONS = "genart.ribbons"
        const val NEBULA = "genart.nebula"

        fun defaultCatalog(): List<Artwork> = listOf(
            entry(PARTICLES, "Drifting Particles"),
            entry(PSEUDO3D, "Wire Lattice"),
            entry(SOFT_SHADOWS, "Soft Shadows"),
            entry(TUNNEL, "Vanishing Tunnel"),
            entry(TONAL_GEOMETRY, "Tonal Geometry"),
            entry(SPHERE, "Orbiting Sphere"),
            entry(WAVES, "Layered Waves"),
            entry(MICRO, "Volumetric Rays"),
            entry(SNOW, "Falling Snow"),
            entry(GRASS, "Grass in Wind"),
            entry(BIRD_FLOCK, "Bird Flock"),
            entry(MOUNTAINS, "Layered Mountains"),
            entry(AURORA, "Aurora Ribbons"),
            entry(POND_RIPPLES, "Pond Ripples"),
            entry(FALLING_LEAVES, "Falling Leaves"),
            entry(BREATH_CIRCLES, "Breath Circles"),
            entry(FIRE_EMBERS, "Fireplace Embers"),
            entry(DUNES, "Wind-Blown Dunes"),
            entry(CONSTELLATION, "Constellation Twinkle"),
            entry(CLOUDS, "Drifting Clouds"),
            entry(RAIN, "Soft Rain"),
            entry(FOG, "Soft Fog"),
            entry(FISH_SCHOOL, "School of Fish"),
            entry(FIREFLIES, "Fireflies"),
            entry(SUNBEAMS, "Sunbeams Through Haze"),
            entry(METEORS, "Sparse Meteors"),
            entry(BUBBLES, "Rising Bubbles"),
            entry(CHERRY_BLOSSOMS, "Cherry Blossom Petals"),
            entry(RIBBONS, "Soft Ribbons"),
            entry(NEBULA, "Nebula Drift"),
        )

        private fun entry(id: String, title: String) = Artwork(
            id = id,
            title = title,
            attribution = "Arthur Genart",
            sourceId = ID,
            kind = ArtworkKind.Genart,
        )
    }
}
