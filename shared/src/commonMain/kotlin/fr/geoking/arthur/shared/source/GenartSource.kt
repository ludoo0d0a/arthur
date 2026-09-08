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
        const val BLOBS = "genart.blobs"
        const val NOISE_FIELD = "genart.noisefield"
        const val VORONOI = "genart.voronoi"
        const val SILK = "genart.silk"
        const val GRADIENT_MESH = "genart.gradientmesh"
        const val ARC_MOSAIC = "genart.arcmosaic"
        const val STORM = "genart.storm"
        const val STAR_FIELD = "genart.starfield"
        const val SOLAR_SYSTEM = "genart.solarsystem"
        const val CANDLE_EMBER = "genart.candleember"
        const val RAINBOW = "genart.rainbow"
        const val SMOG = "genart.smog"
        const val SMOKE = "genart.smoke"
        const val HEAT_HAZE = "genart.heathaze"
        const val SUNSHINE = "genart.sunshine"
        const val LIGHT_DRIZZLE = "genart.lightdrizzle"
        const val STEAM_CURL = "genart.steamcurl"
        const val DRIFTING_POLLEN = "genart.pollen"
        const val LANDSLIDE_DUST = "genart.landslidedust"
        const val PEBBLE_SHORE_WASH = "genart.pebbleshore"
        const val FROST_CRYSTALS = "genart.frostcrystals"
        const val ION_TRAIL = "genart.iontrail"
        const val ANT_TRAILS = "genart.anttrails"
        const val SLEEPING_PET = "genart.sleepingpet"
        const val WARP_STREAK = "genart.warpstreak"
        const val SPACE_STATION_DRIFT = "genart.spacestation"
        const val MOONLIGHT_RIPPLES = "genart.moonlightripples"
        const val ECLIPSE_CORONA = "genart.eclipsecorona"
        const val INK_IN_WATER = "genart.inkinwater"
        const val WIND_CHIME = "genart.windchime"
        const val DAY_NIGHT_WASH = "genart.daynightwash"
        const val TUMBLEWEED_DRIFT = "genart.tumbleweed"
        const val DISTANT_DINOSAURS = "genart.dinosaurs"
        const val CITY_LIGHTS = "genart.citylights"
        const val MOSS_GROWTH = "genart.moss"
        const val TERRARIUM_DRIP = "genart.terrariumdrip"
        const val AQUARIUM = "genart.aquarium"
        const val FIELDS = "genart.fields"
        const val RAIN_ON_GLASS = "genart.rainonglass"
        const val REEDS = "genart.reeds"
        const val CANYON_DUNES = "genart.canyondunes"
        const val CONTINENTS = "genart.continents"
        const val ROADS = "genart.roads"
        const val RIVERS = "genart.rivers"
        const val TREE = "genart.tree"
        const val FLOWER = "genart.flower"
        const val LAKE = "genart.lake"
        const val ASTEROIDS = "genart.asteroids"
        const val WATERFALL_MIST = "genart.waterfallmist"
        const val SOFT_WIND_STREAKS = "genart.windstreaks"
        const val GERSTNER_OCEAN = "genart.oceanswell"
        const val SPIRAL_GALAXY = "genart.galaxy"
        const val DATA_HORIZON = "genart.datahorizon"
        const val SOFT_CAUSTICS = "genart.caustics"
        const val LOW_FREQ_NOISE_FIELD = "genart.lowfreqnoise"

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
            entry(BLOBS, "Morphing Blobs"),
            entry(NOISE_FIELD, "Soft Noise Field"),
            entry(VORONOI, "Voronoi Wash"),
            entry(SILK, "Silk Folds"),
            entry(GRADIENT_MESH, "Gradient Mesh"),
            entry(ARC_MOSAIC, "Arc Mosaic"),
            entry(STORM, "Soft Storm"),
            entry(STAR_FIELD, "Star Field Parallax"),
            entry(SOLAR_SYSTEM, "Solar System"),
            entry(CANDLE_EMBER, "Candle Ember"),
            entry(RAINBOW, "Soft Rainbow"),
            entry(SMOG, "Soft Smog"),
            entry(SMOKE, "Rising Smoke"),
            entry(HEAT_HAZE, "Heat Haze"),
            entry(SUNSHINE, "Soft Sunshine"),
            entry(LIGHT_DRIZZLE, "Light Drizzle"),
            entry(STEAM_CURL, "Steam Curl"),
            entry(DRIFTING_POLLEN, "Drifting Pollen"),
            entry(LANDSLIDE_DUST, "Soft Landslide Dust"),
            entry(PEBBLE_SHORE_WASH, "Pebble Shore Wash"),
            entry(FROST_CRYSTALS, "First Frost Crystals"),
            entry(ION_TRAIL, "Ion Trail"),
            entry(ANT_TRAILS, "Ant Trails"),
            entry(SLEEPING_PET, "Sleeping Pet Outline"),
            entry(WARP_STREAK, "Warp Streak"),
            entry(SPACE_STATION_DRIFT, "Space Station Drift"),
            entry(MOONLIGHT_RIPPLES, "Moonlight Ripples"),
            entry(ECLIPSE_CORONA, "Eclipse Corona"),
            entry(INK_IN_WATER, "Ink in Water"),
            entry(WIND_CHIME, "Wind Chime Silhouette"),
            entry(DAY_NIGHT_WASH, "Soft Day-Night Wash"),
            entry(TUMBLEWEED_DRIFT, "Tumbleweed Drift"),
            entry(DISTANT_DINOSAURS, "Distant Dinosaur Silhouettes"),
            entry(CITY_LIGHTS, "City Night Lights"),
            entry(MOSS_GROWTH, "Moss Growth"),
            entry(TERRARIUM_DRIP, "Terrarium Drip"),
            entry(AQUARIUM, "Aquarium"),
            entry(FIELDS, "Soft Fields"),
            entry(RAIN_ON_GLASS, "Rain on Glass"),
            entry(REEDS, "Reeds"),
            entry(CANYON_DUNES, "Canyon Dunes"),
            entry(CONTINENTS, "Continents"),
            entry(ROADS, "Roads"),
            entry(RIVERS, "Rivers"),
            entry(TREE, "Tree in Wind"),
            entry(FLOWER, "Flower Bloom"),
            entry(LAKE, "Lake Surface"),
            entry(ASTEROIDS, "Asteroids"),
            entry(WATERFALL_MIST, "Waterfall Mist"),
            entry(SOFT_WIND_STREAKS, "Soft Wind Streaks"),
            entry(GERSTNER_OCEAN, "Ocean Swell"),
            entry(SPIRAL_GALAXY, "Spiral Galaxy Drift"),
            entry(DATA_HORIZON, "Data Horizon"),
            entry(SOFT_CAUSTICS, "Soft Caustics"),
            entry(LOW_FREQ_NOISE_FIELD, "Low-Frequency Noise Field"),
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
