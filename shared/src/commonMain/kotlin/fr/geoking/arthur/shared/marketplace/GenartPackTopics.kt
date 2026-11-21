package fr.geoking.arthur.shared.marketplace

import fr.geoking.arthur.shared.source.GenartSource

/**
 * Genart topic → engine artwork ids (shared by Content Engine gates and Control Plane filters).
 * Topic suffixes match Android [GenartTopic.testTagSuffix] values.
 *
 * Category packs are **exclusive**: each engine id appears in exactly one monetized
 * category set (not Fractal / Custom, which are kinds). Assignment priority when
 * partitioning overlaps: Vintage → Tapet → Life → Nature → Weather → Water → Earth →
 * Planets → SciFi → Geometry → Abstract.
 */
object GenartPackTopics {
    const val TAPET = "tapet"
    const val NATURE = "nature"
    const val WEATHER = "weather"
    const val WATER = "water"
    const val LIFE = "life"
    const val EARTH = "earth"
    const val PLANETS = "planets"
    const val SCIFI = "scifi"
    const val VINTAGE = "vintage"
    const val ABSTRACT = "abstract"
    const val GEOMETRY = "geometry"
    const val FRACTAL = "fractal"
    const val CUSTOM = "custom"

    /** Monetized topic suffixes (excludes All / Random). */
    val monetizedTopicSuffixes: List<String> = listOf(
        TAPET, NATURE, WEATHER, WATER, LIFE, EARTH, PLANETS, SCIFI, VINTAGE, ABSTRACT, GEOMETRY, FRACTAL, CUSTOM,
    )

    /** Category packs that own engine id sets (excludes Fractal / Custom kinds). */
    val categoryTopicSuffixes: List<String> = listOf(
        TAPET, NATURE, WEATHER, WATER, LIFE, EARTH, PLANETS, SCIFI, VINTAGE, ABSTRACT, GEOMETRY,
    )

    val TAPET_IDS: Set<String> = setOf(
        GenartSource.GRADIENT_MESH,
        GenartSource.BLOBS,
        GenartSource.VORONOI,
        GenartSource.SILK,
        GenartSource.ARC_MOSAIC,
        GenartSource.RIBBONS,
        GenartSource.NOISE_FIELD,
        GenartSource.LOW_FREQ_NOISE_FIELD,
        GenartSource.TONAL_GEOMETRY,
        GenartSource.PAPER_CUT_PACK,
        GenartSource.DIAMOND_WEAVE,
        GenartSource.NESTED_PACK,
        GenartSource.CHROMATIC_BLOBS,
        GenartSource.BILLOWING_CLOTH,
        GenartSource.WIND_CURTAINS,
        GenartSource.PAPER_SIMPLEX_NOISE,
        GenartSource.PAPER_GRAIN_GRADIENT,
        GenartSource.SHADY_MARBLED_PAPER,
        GenartSource.LWP_VORONOI,
    )

    val WEATHER_IDS: Set<String> = setOf(
        GenartSource.SNOW,
        GenartSource.AURORA,
        GenartSource.CLOUDS,
        GenartSource.RAIN,
        GenartSource.FOG,
        GenartSource.SUNBEAMS,
        GenartSource.STORM,
        GenartSource.LIGHT_DRIZZLE,
        GenartSource.RAINBOW,
        GenartSource.SMOG,
        GenartSource.SMOKE,
        GenartSource.HEAT_HAZE,
        GenartSource.SUNSHINE,
        GenartSource.STEAM_CURL,
        GenartSource.RAIN_ON_GLASS,
        GenartSource.PANE_REFLECTIONS,
        GenartSource.WATERFALL_MIST,
        GenartSource.SOFT_WIND_STREAKS,
        GenartSource.DAY_NIGHT_WASH,
        GenartSource.FROST_CRYSTALS,
        GenartSource.ECLIPSE_CORONA,
        GenartSource.FIRE,
        GenartSource.AURORA_WASH,
        GenartSource.FIREWORKS,
    )

    val NATURE_IDS: Set<String> = setOf(
        GenartSource.GRASS,
        GenartSource.POND_RIPPLES,
        GenartSource.FALLING_LEAVES,
        GenartSource.BUBBLES,
        GenartSource.WAVES,
        GenartSource.LAKE,
        GenartSource.FIELDS,
        GenartSource.REEDS,
        GenartSource.RIVERS,
        GenartSource.FLOW_RIBBONS,
        GenartSource.MULTI_FLAMES,
        GenartSource.AIR_BUBBLES,
    )

    val WATER_IDS: Set<String> = setOf(
        GenartSource.PEBBLE_SHORE_WASH,
        GenartSource.MOONLIGHT_RIPPLES,
        GenartSource.INK_IN_WATER,
        GenartSource.GERSTNER_OCEAN,
        GenartSource.SOFT_CAUSTICS,
        GenartSource.GLASS_MARBLES,
        GenartSource.MARBLE_CAUSTICS,
        GenartSource.DEPTH_SHAFTS,
        GenartSource.SOAP_FILM,
        GenartSource.GLASS_ORB,
    )

    val LIFE_IDS: Set<String> = setOf(
        GenartSource.BIRD_FLOCK,
        GenartSource.FISH_SCHOOL,
        GenartSource.FIREFLIES,
        GenartSource.ANT_TRAILS,
        GenartSource.SLEEPING_PET,
        GenartSource.MOSS_GROWTH,
        GenartSource.TERRARIUM_DRIP,
        GenartSource.AQUARIUM,
        GenartSource.TREE,
        GenartSource.FLOWER,
        GenartSource.SILK_BLOOM,
        GenartSource.CHERRY_BLOSSOMS,
        GenartSource.DRIFTING_POLLEN,
        GenartSource.CANDLE_EMBER,
        GenartSource.WIND_CHIME,
        GenartSource.LAMP_IN_DARKNESS,
    )

    val EARTH_IDS: Set<String> = setOf(
        GenartSource.MOUNTAINS,
        GenartSource.FIRE_EMBERS,
        GenartSource.DUNES,
        GenartSource.LANDSLIDE_DUST,
        GenartSource.TUMBLEWEED_DRIFT,
        GenartSource.CANYON_DUNES,
        GenartSource.CONTINENTS,
        GenartSource.CANYON_LIGHT,
        GenartSource.TERRAIN_MAKER,
    )

    val PLANETS_IDS: Set<String> = setOf(
        GenartSource.SPHERE,
        GenartSource.CONSTELLATION,
        GenartSource.METEORS,
        GenartSource.NEBULA,
        GenartSource.STAR_FIELD,
        GenartSource.SOLAR_SYSTEM,
        GenartSource.SPACE_STATION_DRIFT,
        GenartSource.SPIRAL_GALAXY,
        GenartSource.HALO_ECLIPSE,
        GenartSource.ATMOSPHERIC_ASTEROID,
        GenartSource.SATURN_RINGS,
        GenartSource.LAVA_SUN,
        GenartSource.MOON,
        GenartSource.LWP_STARFIELD,
        GenartSource.SHADY_WARP_SPEED,
        GenartSource.MILKY_WAY,
    )

    val SCIFI_IDS: Set<String> = setOf(
        GenartSource.ION_TRAIL,
        GenartSource.CITY_LIGHTS,
        GenartSource.VORTEX_GLOW,
        GenartSource.SUPERDRIVE,
        GenartSource.SPIRAL_MANDALA,
        GenartSource.CLIFFORD_WASH,
        GenartSource.PLASMA_NOVA,
        GenartSource.ENERGY_TENDRILS,
        GenartSource.SOFT_RAY_ORBS,
        GenartSource.PRISM_CAVE,
        GenartSource.LIT_LATTICE,
        GenartSource.LWP_PLASMA,
        GenartSource.SHADERLAY_VORTEX_TUNNEL,
        GenartSource.SHADY_NOODLE_ZOOM,
        GenartSource.PLASMA_FLOW,
        GenartSource.ROBOTIC_EYE,
    )

    val VINTAGE_IDS: Set<String> = setOf(
        GenartSource.COURT_BOUNCE,
        GenartSource.COPPER_BARS,
        GenartSource.NEON_HIGHWAY,
        GenartSource.SOFT_DROP,
        GenartSource.VECTOR_ROCKS,
        GenartSource.BRICK_CASCADE,
        GenartSource.PIXEL_FORMATION,
        GenartSource.CRT_BOOT,
        GenartSource.RETRO_WAVE,
        GenartSource.TUNNEL,
        GenartSource.ROADS,
        GenartSource.PSEUDO3D,
        GenartSource.MATRIX,
        GenartSource.ASTEROIDS,
        GenartSource.DATA_HORIZON,
        GenartSource.WARP_STREAK,
        GenartSource.SHADERLAY_NEON_GRID,
        GenartSource.PPSSPP_CRT_SCANLINES,
        GenartSource.PPSSPP_CEL_SHADER,
        GenartSource.PPSSPP_BLOOM_VIGNETTE,
    )

    val ABSTRACT_IDS: Set<String> = setOf(
        GenartSource.BREATH_CIRCLES,
        GenartSource.SPECTRAL_FOLDS,
        GenartSource.INTERFERENCE_WASH,
        GenartSource.MARBLE_DRIFT,
        GenartSource.PAPER_NEURO_NOISE,
    )

    val GEOMETRY_IDS: Set<String> = setOf(
        GenartSource.PARTICLES,
        GenartSource.SOFT_SHADOWS,
        GenartSource.MICRO,
        GenartSource.PRISMATIC_SHADOWS,
        GenartSource.DRIFTING_HALOS,
        GenartSource.CROSSING_SPOTLIGHTS,
    )

    fun engineIdsForTopic(topicSuffix: String): Set<String> = when (topicSuffix) {
        TAPET -> TAPET_IDS
        NATURE -> NATURE_IDS
        WEATHER -> WEATHER_IDS
        WATER -> WATER_IDS
        LIFE -> LIFE_IDS
        EARTH -> EARTH_IDS
        PLANETS -> PLANETS_IDS
        SCIFI -> SCIFI_IDS
        VINTAGE -> VINTAGE_IDS
        ABSTRACT -> ABSTRACT_IDS
        GEOMETRY -> GEOMETRY_IDS
        else -> emptySet()
    }

    /** Topic suffixes whose engine sets contain [engineId] (monetized genart topics only). */
    fun topicsCoveringEngine(engineId: String): List<String> =
        monetizedTopicSuffixes.filter { suffix ->
            suffix != FRACTAL && suffix != CUSTOM && engineId in engineIdsForTopic(suffix)
        }
}
