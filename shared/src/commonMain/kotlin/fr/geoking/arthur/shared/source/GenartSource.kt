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
        const val CHROMATIC_BLOBS = "genart.chromaticblobs"
        const val PRISMATIC_SHADOWS = "genart.prismaticshadows"
        const val AURORA_WASH = "genart.aurorawash"
        const val VORTEX_GLOW = "genart.vortexglow"
        const val SPECTRAL_FOLDS = "genart.spectralfolds"
        const val HALO_ECLIPSE = "genart.haloeclipse"
        const val FIRE = "genart.fire"
        const val PAPER_CUT_PACK = "genart.papercut"
        const val DIAMOND_WEAVE = "genart.diamondweave"
        const val MATRIX = "genart.matrix"
        const val DRIFTING_HALOS = "genart.driftinghalos"
        const val SUPERDRIVE = "genart.superdrive"
        const val FIREWORKS = "genart.fireworks"
        const val ATMOSPHERIC_ASTEROID = "genart.atmosphericasteroid"
        const val SATURN_RINGS = "genart.saturnrings"
        const val LAVA_SUN = "genart.lavasun"
        const val MOON = "genart.moon"
        const val SPIRAL_MANDALA = "genart.spiralmandala"
        const val FLOW_RIBBONS = "genart.flowribbons"
        const val CLIFFORD_WASH = "genart.cliffordwash"
        const val PLASMA_NOVA = "genart.plasmanova"
        const val ENERGY_TENDRILS = "genart.energytendrils"
        const val INTERFERENCE_WASH = "genart.interferencewash"
        const val NESTED_PACK = "genart.nestedpack"
        const val SILK_BLOOM = "genart.silkbloom"
        const val CROSSING_SPOTLIGHTS = "genart.crossingspots"
        const val LAMP_IN_DARKNESS = "genart.lampdark"

        fun defaultCatalog(): List<Artwork> = listOf(
            entry(PARTICLES, "#1 - Drifting Particles"),
            entry(PSEUDO3D, "#2 - Wire Lattice"),
            entry(SOFT_SHADOWS, "#3 - Soft Shadows"),
            entry(TUNNEL, "#4 - Vanishing Tunnel"),
            entry(TONAL_GEOMETRY, "#5 - Tonal Geometry"),
            entry(SPHERE, "#6 - Orbiting Sphere"),
            entry(WAVES, "#7 - Layered Waves"),
            entry(MICRO, "#8 - Volumetric Rays"),
            entry(SNOW, "#9 - Falling Snow"),
            entry(GRASS, "#10 - Grass in Wind"),
            entry(BIRD_FLOCK, "#11 - Bird Flock"),
            entry(MOUNTAINS, "#12 - Layered Mountains"),
            entry(AURORA, "#13 - Aurora Ribbons"),
            entry(POND_RIPPLES, "#14 - Pond Ripples"),
            entry(FALLING_LEAVES, "#15 - Falling Leaves"),
            entry(BREATH_CIRCLES, "#16 - Breath Circles"),
            entry(FIRE_EMBERS, "#17 - Fireplace Embers"),
            entry(DUNES, "#18 - Wind-Blown Dunes"),
            entry(CONSTELLATION, "#19 - Constellation Twinkle"),
            entry(CLOUDS, "#20 - Drifting Clouds"),
            entry(RAIN, "#21 - Soft Rain"),
            entry(FOG, "#22 - Soft Fog"),
            entry(FISH_SCHOOL, "#23 - School of Fish"),
            entry(FIREFLIES, "#24 - Fireflies"),
            entry(SUNBEAMS, "#25 - Sunbeams Through Haze"),
            entry(METEORS, "#26 - Sparse Meteors"),
            entry(BUBBLES, "#27 - Rising Bubbles"),
            entry(CHERRY_BLOSSOMS, "#28 - Cherry Blossom Petals"),
            entry(RIBBONS, "#29 - Soft Ribbons"),
            entry(NEBULA, "#30 - Nebula Drift"),
            entry(BLOBS, "#31 - Morphing Blobs"),
            entry(NOISE_FIELD, "#32 - Soft Noise Field"),
            entry(VORONOI, "#33 - Voronoi Wash"),
            entry(SILK, "#34 - Silk Folds"),
            entry(GRADIENT_MESH, "#35 - Gradient Mesh"),
            entry(ARC_MOSAIC, "#36 - Arc Mosaic"),
            entry(STORM, "#37 - Soft Storm"),
            entry(STAR_FIELD, "#38 - Star Field Parallax"),
            entry(SOLAR_SYSTEM, "#39 - Solar System"),
            entry(CANDLE_EMBER, "#40 - Candle Ember"),
            entry(RAINBOW, "#41 - Soft Rainbow"),
            entry(SMOG, "#42 - Soft Smog"),
            entry(SMOKE, "#43 - Rising Smoke"),
            entry(HEAT_HAZE, "#44 - Heat Haze"),
            entry(SUNSHINE, "#45 - Soft Sunshine"),
            entry(LIGHT_DRIZZLE, "#46 - Light Drizzle"),
            entry(STEAM_CURL, "#47 - Steam Curl"),
            entry(DRIFTING_POLLEN, "#48 - Drifting Pollen"),
            entry(LANDSLIDE_DUST, "#49 - Soft Landslide Dust"),
            entry(PEBBLE_SHORE_WASH, "#50 - Pebble Shore Wash"),
            entry(FROST_CRYSTALS, "#51 - First Frost Crystals"),
            entry(ION_TRAIL, "#52 - Ion Trail"),
            entry(ANT_TRAILS, "#53 - Ant Trails"),
            entry(SLEEPING_PET, "#54 - Sleeping Pet Outline"),
            entry(WARP_STREAK, "#55 - Warp Streak"),
            entry(SPACE_STATION_DRIFT, "#56 - Space Station Drift"),
            entry(MOONLIGHT_RIPPLES, "#57 - Moonlight Ripples"),
            entry(ECLIPSE_CORONA, "#58 - Eclipse Corona"),
            entry(INK_IN_WATER, "#59 - Ink in Water"),
            entry(WIND_CHIME, "#60 - Wind Chime Silhouette"),
            entry(DAY_NIGHT_WASH, "#61 - Soft Day-Night Wash"),
            entry(TUMBLEWEED_DRIFT, "#62 - Tumbleweed Drift"),
            entry(CITY_LIGHTS, "#64 - City Night Lights"),
            entry(MOSS_GROWTH, "#65 - Moss Growth"),
            entry(TERRARIUM_DRIP, "#66 - Terrarium Drip"),
            entry(AQUARIUM, "#67 - Aquarium"),
            entry(FIELDS, "#68 - Soft Fields"),
            entry(RAIN_ON_GLASS, "#69 - Rain on Glass"),
            entry(REEDS, "#70 - Reeds"),
            entry(CANYON_DUNES, "#71 - Canyon Dunes"),
            entry(CONTINENTS, "#72 - Continents"),
            entry(ROADS, "#73 - Roads"),
            entry(RIVERS, "#74 - Rivers"),
            entry(TREE, "#75 - Tree in Wind"),
            entry(FLOWER, "#76 - Flower Bloom"),
            entry(LAKE, "#77 - Lake Surface"),
            entry(ASTEROIDS, "#78 - Asteroids"),
            entry(WATERFALL_MIST, "#79 - Waterfall Mist"),
            entry(SOFT_WIND_STREAKS, "#80 - Soft Wind Streaks"),
            entry(GERSTNER_OCEAN, "#81 - Ocean Swell"),
            entry(SPIRAL_GALAXY, "#82 - Spiral Galaxy Drift"),
            entry(DATA_HORIZON, "#83 - Data Horizon"),
            entry(SOFT_CAUSTICS, "#84 - Soft Caustics"),
            entry(LOW_FREQ_NOISE_FIELD, "#85 - Low-Frequency Noise Field"),
            entry(CHROMATIC_BLOBS, "#87 - Chromatic Blobs"),
            entry(PRISMATIC_SHADOWS, "#88 - Prismatic Shadows"),
            entry(AURORA_WASH, "#89 - Aurora Wash"),
            entry(VORTEX_GLOW, "#90 - Vortex Glow"),
            entry(SPECTRAL_FOLDS, "#91 - Spectral Folds"),
            entry(HALO_ECLIPSE, "#92 - Halo Eclipse"),
            entry(FIRE, "#86 - Wildfire"),
            entry(PAPER_CUT_PACK, "#87 - Paper-Cut Pack"),
            entry(DIAMOND_WEAVE, "#88 - Diamond Weave"),
            entry(MATRIX, "#93 - Matrix Rain"),
            entry(DRIFTING_HALOS, "#94 - Moving Halos"),
            entry(SUPERDRIVE, "#95 - Superdrive Vibes"),
            entry(FIREWORKS, "#96 - Soft Fireworks"),
            entry(ATMOSPHERIC_ASTEROID, "#97 - Atmospheric Asteroid"),
            entry(SATURN_RINGS, "#98 - Saturn and Rings"),
            entry(LAVA_SUN, "#99 - Lava Sun"),
            entry(MOON, "#100 - Full Moon"),
            entry(SPIRAL_MANDALA, "#101 - Spiral Mandala"),
            entry(FLOW_RIBBONS, "#102 - Flow Ribbons"),
            entry(CLIFFORD_WASH, "#103 - Clifford Wash"),
            entry(PLASMA_NOVA, "#104 - Plasma Nova"),
            entry(ENERGY_TENDRILS, "#105 - Energy Tendrils"),
            entry(INTERFERENCE_WASH, "#106 - Interference Wash"),
            entry(NESTED_PACK, "#107 - Nested Pack"),
            entry(SILK_BLOOM, "#108 - Silk Bloom"),
            entry(CROSSING_SPOTLIGHTS, "#109 - Crossing Spotlights"),
            entry(LAMP_IN_DARKNESS, "#110 - Lamp in Darkness"),
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
