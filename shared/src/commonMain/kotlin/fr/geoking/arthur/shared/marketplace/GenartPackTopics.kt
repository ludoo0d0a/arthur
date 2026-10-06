package fr.geoking.arthur.shared.marketplace

import fr.geoking.arthur.shared.source.GenartSource

/**
 * Genart topic → engine artwork ids (shared by Content Engine gates and Control Plane filters).
 * Topic suffixes match Android [GenartTopic.testTagSuffix] values.
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
    const val ABSTRACT = "abstract"
    const val GEOMETRY = "geometry"
    const val FRACTAL = "fractal"
    const val CUSTOM = "custom"

    /** Monetized topic suffixes (excludes All / Random). */
    val monetizedTopicSuffixes: List<String> = listOf(
        TAPET, NATURE, WEATHER, WATER, LIFE, EARTH, PLANETS, SCIFI, ABSTRACT, GEOMETRY, FRACTAL, CUSTOM,
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
        GenartSource.BILLOWING_CLOTH,
        GenartSource.WIND_CURTAINS,
        GenartSource.DAY_NIGHT_WASH,
        GenartSource.FROST_CRYSTALS,
        GenartSource.ECLIPSE_CORONA,
        GenartSource.FIRE,
        GenartSource.AURORA_WASH,
        GenartSource.FIREWORKS,
        GenartSource.MULTI_FLAMES,
    )

    val NATURE_IDS: Set<String> = setOf(
        GenartSource.GRASS,
        GenartSource.BIRD_FLOCK,
        GenartSource.MOUNTAINS,
        GenartSource.POND_RIPPLES,
        GenartSource.FALLING_LEAVES,
        GenartSource.FIRE_EMBERS,
        GenartSource.DUNES,
        GenartSource.FISH_SCHOOL,
        GenartSource.FIREFLIES,
        GenartSource.BUBBLES,
        GenartSource.CHERRY_BLOSSOMS,
        GenartSource.WAVES,
        GenartSource.TREE,
        GenartSource.FLOWER,
        GenartSource.SILK_BLOOM,
        GenartSource.LAKE,
        GenartSource.FIELDS,
        GenartSource.REEDS,
        GenartSource.MOSS_GROWTH,
        GenartSource.RIVERS,
        GenartSource.CANYON_DUNES,
        GenartSource.DRIFTING_POLLEN,
        GenartSource.WIND_CHIME,
        GenartSource.FLOW_RIBBONS,
        GenartSource.CANYON_LIGHT,
        GenartSource.TERRAIN_MAKER,
        GenartSource.MULTI_FLAMES,
        GenartSource.AIR_BUBBLES,
    )

    val WATER_IDS: Set<String> = setOf(
        GenartSource.WAVES,
        GenartSource.POND_RIPPLES,
        GenartSource.RAIN,
        GenartSource.FISH_SCHOOL,
        GenartSource.BUBBLES,
        GenartSource.LIGHT_DRIZZLE,
        GenartSource.PEBBLE_SHORE_WASH,
        GenartSource.FROST_CRYSTALS,
        GenartSource.MOONLIGHT_RIPPLES,
        GenartSource.INK_IN_WATER,
        GenartSource.TERRARIUM_DRIP,
        GenartSource.AQUARIUM,
        GenartSource.RAIN_ON_GLASS,
        GenartSource.REEDS,
        GenartSource.RIVERS,
        GenartSource.LAKE,
        GenartSource.WATERFALL_MIST,
        GenartSource.GERSTNER_OCEAN,
        GenartSource.SOFT_CAUSTICS,
        GenartSource.GLASS_MARBLES,
        GenartSource.PANE_REFLECTIONS,
        GenartSource.MARBLE_CAUSTICS,
        GenartSource.DEPTH_SHAFTS,
        GenartSource.SOAP_FILM,
        GenartSource.AIR_BUBBLES,
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
        GenartSource.GRASS,
        GenartSource.MOUNTAINS,
        GenartSource.FIRE_EMBERS,
        GenartSource.DUNES,
        GenartSource.HEAT_HAZE,
        GenartSource.LANDSLIDE_DUST,
        GenartSource.PEBBLE_SHORE_WASH,
        GenartSource.TUMBLEWEED_DRIFT,
        GenartSource.MOSS_GROWTH,
        GenartSource.FIELDS,
        GenartSource.CANYON_DUNES,
        GenartSource.CONTINENTS,
        GenartSource.FIRE,
        GenartSource.CANYON_LIGHT,
        GenartSource.TERRAIN_MAKER,
        GenartSource.MULTI_FLAMES,
    )

    val PLANETS_IDS: Set<String> = setOf(
        GenartSource.SPHERE,
        GenartSource.CONSTELLATION,
        GenartSource.METEORS,
        GenartSource.NEBULA,
        GenartSource.STAR_FIELD,
        GenartSource.SOLAR_SYSTEM,
        GenartSource.ECLIPSE_CORONA,
        GenartSource.SPACE_STATION_DRIFT,
        GenartSource.SPIRAL_GALAXY,
        GenartSource.ASTEROIDS,
        GenartSource.CONTINENTS,
        GenartSource.HALO_ECLIPSE,
        GenartSource.ATMOSPHERIC_ASTEROID,
        GenartSource.SATURN_RINGS,
        GenartSource.LAVA_SUN,
        GenartSource.MOON,
    )

    val SCIFI_IDS: Set<String> = setOf(
        GenartSource.PSEUDO3D,
        GenartSource.TUNNEL,
        GenartSource.STAR_FIELD,
        GenartSource.SOLAR_SYSTEM,
        GenartSource.ION_TRAIL,
        GenartSource.WARP_STREAK,
        GenartSource.SPACE_STATION_DRIFT,
        GenartSource.CITY_LIGHTS,
        GenartSource.ROADS,
        GenartSource.ASTEROIDS,
        GenartSource.SPIRAL_GALAXY,
        GenartSource.DATA_HORIZON,
        GenartSource.VORTEX_GLOW,
        GenartSource.MATRIX,
        GenartSource.SUPERDRIVE,
        GenartSource.FIREWORKS,
        GenartSource.ATMOSPHERIC_ASTEROID,
        GenartSource.SATURN_RINGS,
        GenartSource.LAVA_SUN,
        GenartSource.SPIRAL_MANDALA,
        GenartSource.CLIFFORD_WASH,
        GenartSource.PLASMA_NOVA,
        GenartSource.ENERGY_TENDRILS,
        GenartSource.NEBULA,
        GenartSource.ECLIPSE_CORONA,
        GenartSource.SOFT_RAY_ORBS,
        GenartSource.PRISM_CAVE,
        GenartSource.LIT_LATTICE,
        GenartSource.RETRO_WAVE,
    )

    val ABSTRACT_IDS: Set<String> = setOf(
        GenartSource.BREATH_CIRCLES,
        GenartSource.RIBBONS,
        GenartSource.BLOBS,
        GenartSource.NOISE_FIELD,
        GenartSource.VORONOI,
        GenartSource.SILK,
        GenartSource.SILK_BLOOM,
        GenartSource.GRADIENT_MESH,
        GenartSource.ARC_MOSAIC,
        GenartSource.LOW_FREQ_NOISE_FIELD,
        GenartSource.INK_IN_WATER,
        GenartSource.SOFT_SHADOWS,
        GenartSource.PARTICLES,
        GenartSource.SOFT_CAUSTICS,
        GenartSource.PAPER_CUT_PACK,
        GenartSource.DIAMOND_WEAVE,
        GenartSource.CHROMATIC_BLOBS,
        GenartSource.PRISMATIC_SHADOWS,
        GenartSource.AURORA_WASH,
        GenartSource.VORTEX_GLOW,
        GenartSource.SPECTRAL_FOLDS,
        GenartSource.HALO_ECLIPSE,
        GenartSource.MATRIX,
        GenartSource.DRIFTING_HALOS,
        GenartSource.SUPERDRIVE,
        GenartSource.SPIRAL_MANDALA,
        GenartSource.FLOW_RIBBONS,
        GenartSource.CLIFFORD_WASH,
        GenartSource.INTERFERENCE_WASH,
        GenartSource.NESTED_PACK,
        GenartSource.CROSSING_SPOTLIGHTS,
        GenartSource.LAMP_IN_DARKNESS,
        GenartSource.BILLOWING_CLOTH,
        GenartSource.WIND_CURTAINS,
        GenartSource.GLASS_MARBLES,
        GenartSource.PANE_REFLECTIONS,
        GenartSource.MARBLE_CAUSTICS,
        GenartSource.MARBLE_DRIFT,
        GenartSource.SOFT_RAY_ORBS,
        GenartSource.PRISM_CAVE,
        GenartSource.SOAP_FILM,
        GenartSource.GLASS_ORB,
        GenartSource.LIT_LATTICE,
    )

    val GEOMETRY_IDS: Set<String> = setOf(
        GenartSource.PARTICLES,
        GenartSource.PSEUDO3D,
        GenartSource.SOFT_SHADOWS,
        GenartSource.TUNNEL,
        GenartSource.TONAL_GEOMETRY,
        GenartSource.MICRO,
        GenartSource.ROADS,
        GenartSource.ARC_MOSAIC,
        GenartSource.SPHERE,
        GenartSource.CITY_LIGHTS,
        GenartSource.DATA_HORIZON,
        GenartSource.WARP_STREAK,
        GenartSource.DIAMOND_WEAVE,
        GenartSource.NESTED_PACK,
        GenartSource.PRISMATIC_SHADOWS,
        GenartSource.DRIFTING_HALOS,
        GenartSource.SPIRAL_MANDALA,
        GenartSource.CROSSING_SPOTLIGHTS,
        GenartSource.LAMP_IN_DARKNESS,
        GenartSource.GLASS_MARBLES,
        GenartSource.MARBLE_CAUSTICS,
        GenartSource.PRISM_CAVE,
        GenartSource.LIT_LATTICE,
        GenartSource.GLASS_ORB,
        GenartSource.RETRO_WAVE,
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
