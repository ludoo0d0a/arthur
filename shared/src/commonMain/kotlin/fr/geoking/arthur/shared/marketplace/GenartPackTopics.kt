package fr.geoking.arthur.shared.marketplace

import fr.geoking.arthur.shared.source.GenartSource

/**
 * Genart topic → engine artwork ids (shared by Content Engine gates and Control Plane filters).
 * Topic suffixes match Android [GenartTopic.testTagSuffix] values.
 *
 * Category packs are **exclusive**: each engine id appears in exactly one monetized
 * category set (not Fractal / Custom, which are kinds). Assignment priority when
 * partitioning overlaps: Vintage → Animals → Life → Nature → Weather → Water →
 * Earth → Planets → SciFi → Geometry → Illusions → Seasons → Light → Night →
 * Material → Interior → Crystal → Ink → Botanical → Fire → Kaleido → Zen →
 * Coast → Dream → Micro → Auto → Moto → Avion → Velo → Train → UsVintage →
 * StormStrobe → Physarum → Reactions → Abstract.
 */
object GenartPackTopics {
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
    const val ILLUSIONS = "illusions"
    const val ANIMALS = "animals"
    const val SEASONS = "seasons"
    const val LIGHT = "light"
    const val NIGHT = "night"
    const val MATERIAL = "material"
    const val INTERIOR = "interior"
    const val CRYSTAL = "crystal"
    const val INK = "ink"
    const val BOTANICAL = "botanical"
    const val FIRE = "fire"
    const val KALEIDO = "kaleido"
    const val ZEN = "zen"
    const val COAST = "coast"
    const val DREAM = "dream"
    const val MICRO = "micro"
    const val AUTO = "auto"
    const val MOTO = "moto"
    const val AVION = "avion"
    const val VELO = "velo"
    const val TRAIN = "train"
    const val US_VINTAGE = "us_vintage"
    const val STORM_STROBE = "storm_strobe"
    const val PHYSARUM = "physarum"
    const val REACTIONS = "reactions"
    const val FRACTAL = "fractal"
    const val CUSTOM = "custom"

    /** Monetized topic suffixes (excludes All / Random). */
    val monetizedTopicSuffixes: List<String> = listOf(
        NATURE, WEATHER, WATER, LIFE, EARTH, PLANETS, SCIFI, VINTAGE, ABSTRACT, GEOMETRY,
        ILLUSIONS,
        ANIMALS, SEASONS, LIGHT, NIGHT, MATERIAL, INTERIOR, CRYSTAL, INK, BOTANICAL, FIRE,
        KALEIDO, ZEN, COAST, DREAM, MICRO, AUTO, MOTO, AVION, VELO, TRAIN, US_VINTAGE,
        STORM_STROBE, PHYSARUM, REACTIONS,
        FRACTAL, CUSTOM,
    )

    /** Category packs that own engine id sets (excludes Fractal / Custom kinds). */
    val categoryTopicSuffixes: List<String> = listOf(
        NATURE, WEATHER, WATER, LIFE, EARTH, PLANETS, SCIFI, VINTAGE, ABSTRACT, GEOMETRY,
        ILLUSIONS,
        ANIMALS, SEASONS, LIGHT, NIGHT, MATERIAL, INTERIOR, CRYSTAL, INK, BOTANICAL, FIRE,
        KALEIDO, ZEN, COAST, DREAM, MICRO, AUTO, MOTO, AVION, VELO, TRAIN, US_VINTAGE,
        STORM_STROBE, PHYSARUM, REACTIONS,
    )

    val WEATHER_IDS: Set<String> = setOf(
        GenartSource.AURORA,
        GenartSource.CLOUDS,
        GenartSource.RAIN,
        GenartSource.FOG,
        GenartSource.STORM,
        GenartSource.RAINBOW,
        GenartSource.LIGHT_DRIZZLE,
        GenartSource.STEAM_CURL,
        GenartSource.ECLIPSE_CORONA,
        GenartSource.DAY_NIGHT_WASH,
        GenartSource.RAIN_ON_GLASS,
        GenartSource.WATERFALL_MIST,
        GenartSource.SOFT_WIND_STREAKS,
        GenartSource.FIREWORKS,
    )

    val NATURE_IDS: Set<String> = setOf(
        GenartSource.GRASS,
        GenartSource.POND_RIPPLES,
        GenartSource.FIELDS,
        GenartSource.RIVERS,
        GenartSource.LAKE,
        GenartSource.FLOW_RIBBONS,
    )

    val WATER_IDS: Set<String> = setOf(
        GenartSource.BUBBLES,
        GenartSource.GERSTNER_OCEAN,
        GenartSource.SOFT_CAUSTICS,
        GenartSource.MARBLE_CAUSTICS,
        GenartSource.AIR_BUBBLES,
        GenartSource.SOFT_TIDE_RIPPLES,
        GenartSource.HARBOR_MIST,
    )

    val LIFE_IDS: Set<String> = setOf(
        GenartSource.BIRD_FLOCK,
        GenartSource.FISH_SCHOOL,
        GenartSource.ANT_TRAILS,
        GenartSource.WIND_CHIME,
        GenartSource.TERRARIUM_DRIP,
        GenartSource.AQUARIUM,
    )

    val EARTH_IDS: Set<String> = setOf(
        GenartSource.MOUNTAINS,
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
        GenartSource.SOLAR_SYSTEM,
        GenartSource.SPACE_STATION_DRIFT,
        GenartSource.SPIRAL_GALAXY,
        GenartSource.HALO_ECLIPSE,
        GenartSource.ATMOSPHERIC_ASTEROID,
        GenartSource.SATURN_RINGS,
        GenartSource.LWP_STARFIELD,
        GenartSource.SHADY_WARP_SPEED,
        GenartSource.MILKY_WAY,
    )

    val SCIFI_IDS: Set<String> = setOf(
        GenartSource.ION_TRAIL,
        GenartSource.VORTEX_GLOW,
        GenartSource.SUPERDRIVE,
        GenartSource.CLIFFORD_WASH,
        GenartSource.PLASMA_NOVA,
        GenartSource.ENERGY_TENDRILS,
        GenartSource.LIT_LATTICE,
        GenartSource.LWP_PLASMA,
        GenartSource.SHADERLAY_VORTEX_TUNNEL,
        GenartSource.SHADY_NOODLE_ZOOM,
        GenartSource.PLASMA_FLOW,
        GenartSource.ROBOTIC_EYE,
    )

    val VINTAGE_IDS: Set<String> = setOf(
        GenartSource.PSEUDO3D,
        GenartSource.TUNNEL,
        GenartSource.WARP_STREAK,
        GenartSource.ROADS,
        GenartSource.ASTEROIDS,
        GenartSource.DATA_HORIZON,
        GenartSource.MATRIX,
        GenartSource.RETRO_WAVE,
        GenartSource.COURT_BOUNCE,
        GenartSource.COPPER_BARS,
        GenartSource.NEON_HIGHWAY,
        GenartSource.VECTOR_ROCKS,
        GenartSource.BRICK_CASCADE,
        GenartSource.PIXEL_FORMATION,
        GenartSource.CRT_BOOT,
        GenartSource.SHADERLAY_NEON_GRID,
        GenartSource.PPSSPP_CRT_SCANLINES,
        GenartSource.PPSSPP_CEL_SHADER,
        GenartSource.PPSSPP_BLOOM_VIGNETTE,
    )

    val ABSTRACT_IDS: Set<String> = setOf(
        GenartSource.RIBBONS,
        GenartSource.BLOBS,
        GenartSource.NOISE_FIELD,
        GenartSource.SILK,
        GenartSource.GRADIENT_MESH,
        GenartSource.CHROMATIC_BLOBS,
        GenartSource.INTERFERENCE_WASH,
        GenartSource.LWP_VORONOI,
        GenartSource.PAPER_SIMPLEX_NOISE,
        GenartSource.PAPER_NEURO_NOISE,
    )

    val GEOMETRY_IDS: Set<String> = setOf(
        GenartSource.PARTICLES,
        GenartSource.TONAL_GEOMETRY,
        GenartSource.VORONOI,
        GenartSource.ARC_MOSAIC,
        GenartSource.PAPER_CUT_PACK,
        GenartSource.DIAMOND_WEAVE,
    )

    val ILLUSIONS_IDS: Set<String> = setOf(
        GenartSource.PENROSE_TRIANGLE,
        GenartSource.PENROSE_STAIRS,
        GenartSource.IMPOSSIBLE_CUBE,
        GenartSource.NECKER_CUBE,
        GenartSource.CAFE_WALL,
        GenartSource.KANIZSA_TRIANGLE,
        GenartSource.CHECKER_SHADOW,
    )

    val ANIMALS_IDS: Set<String> = setOf(
        GenartSource.SLEEPING_PET,
        GenartSource.CAT_WINDOW,
        GenartSource.CAT_SLEEP,
        GenartSource.DOG_NAP,
        GenartSource.DOG_DISTANT_WALK,
        GenartSource.PANDA_SIT,
        GenartSource.PANDA_BAMBOO,
        GenartSource.CAT_TAIL,
        GenartSource.DOG_STRETCH,
        GenartSource.PANDA_ROLL,
    )

    val SEASONS_IDS: Set<String> = setOf(
        GenartSource.SNOW,
        GenartSource.FALLING_LEAVES,
        GenartSource.CHERRY_BLOSSOMS,
        GenartSource.SEASON_SPRING_WASH,
        GenartSource.SEASON_AUTUMN_WASH,
        GenartSource.SEASON_WINTER_WASH,
    )

    val LIGHT_IDS: Set<String> = setOf(
        GenartSource.SUNBEAMS,
        GenartSource.CANDLE_EMBER,
        GenartSource.SUNSHINE,
        GenartSource.CROSSING_SPOTLIGHTS,
        GenartSource.DEPTH_SHAFTS,
        GenartSource.FROSTED_WINDOW_DAWN,
        GenartSource.PAPER_LANTERN,
    )

    val NIGHT_IDS: Set<String> = setOf(
        GenartSource.FIREFLIES,
        GenartSource.STAR_FIELD,
        GenartSource.MOONLIGHT_RIPPLES,
        GenartSource.CITY_LIGHTS,
        GenartSource.MOON,
        GenartSource.LAMP_IN_DARKNESS,
    )

    val MATERIAL_IDS: Set<String> = setOf(
        GenartSource.BILLOWING_CLOTH,
        GenartSource.WIND_CURTAINS,
        GenartSource.GLASS_ORB,
        GenartSource.SOAP_FILM,
        GenartSource.PAPER_GRAIN_GRADIENT,
        GenartSource.SHADY_MARBLED_PAPER,
    )

    val INTERIOR_IDS: Set<String> = setOf(
        GenartSource.SOFT_SHADOWS,
        GenartSource.PANE_REFLECTIONS,
        GenartSource.VOLUMETRIC_STAIRWELL,
        GenartSource.WINDOW_DUST_MOTES,
        GenartSource.BOOKSHELF_GLOW,
        GenartSource.FLOOR_LAMP_POOL,
    )

    val CRYSTAL_IDS: Set<String> = setOf(
        GenartSource.FROST_CRYSTALS,
        GenartSource.PRISMATIC_SHADOWS,
        GenartSource.GLASS_MARBLES,
        GenartSource.SOFT_RAY_ORBS,
        GenartSource.PRISM_CAVE,
        GenartSource.CRYSTAL_CAVERN_GLOW,
    )

    val INK_IDS: Set<String> = setOf(
        GenartSource.INK_IN_WATER,
        GenartSource.SPECTRAL_FOLDS,
        GenartSource.MARBLE_DRIFT,
        GenartSource.INK_BLOOM,
        GenartSource.SUMI_WASH,
        GenartSource.INK_TENDRIL,
    )

    val BOTANICAL_IDS: Set<String> = setOf(
        GenartSource.DRIFTING_POLLEN,
        GenartSource.MOSS_GROWTH,
        GenartSource.REEDS,
        GenartSource.TREE,
        GenartSource.FLOWER,
        GenartSource.SILK_BLOOM,
    )

    val FIRE_IDS: Set<String> = setOf(
        GenartSource.FIRE_EMBERS,
        GenartSource.SMOKE,
        GenartSource.FIRE,
        GenartSource.LAVA_SUN,
        GenartSource.MULTI_FLAMES,
        GenartSource.HEARTH_GLOW,
    )

    val KALEIDO_IDS: Set<String> = setOf(
        GenartSource.BREATH_CIRCLES,
        GenartSource.SPIRAL_MANDALA,
        GenartSource.NESTED_PACK,
        GenartSource.KALEIDO_PETAL,
        GenartSource.KALEIDO_MIRROR,
        GenartSource.KALEIDO_ORBIT,
    )

    val ZEN_IDS: Set<String> = setOf(
        GenartSource.LOW_FREQ_NOISE_FIELD,
        GenartSource.DRIFTING_HALOS,
        GenartSource.SOFT_DROP,
        GenartSource.ZEN_EN_SO,
        GenartSource.ZEN_STONE_RIPPLES,
        GenartSource.ZEN_INCENSE,
    )

    val COAST_IDS: Set<String> = setOf(
        GenartSource.WAVES,
        GenartSource.DUNES,
        GenartSource.PEBBLE_SHORE_WASH,
        GenartSource.OBSIDIAN_SHORE,
        GenartSource.TIDE_FOAM,
        GenartSource.COAST_CLIFF_HAZE,
    )

    val DREAM_IDS: Set<String> = setOf(
        GenartSource.SMOG,
        GenartSource.HEAT_HAZE,
        GenartSource.AURORA_WASH,
        GenartSource.DREAM_VEIL,
        GenartSource.LUCID_ORBS,
        GenartSource.SLEEP_PARALLAX,
    )

    val MICRO_IDS: Set<String> = setOf(
        GenartSource.MICRO,
        GenartSource.MICRO_DEW,
        GenartSource.MICRO_FIBER,
        GenartSource.MICRO_DUST,
        GenartSource.MICRO_POLLEN_CLOSE,
        GenartSource.MICRO_BUBBLE_SKIN,
    )

    val AUTO_IDS: Set<String> = setOf(
        GenartSource.AUTO_DASH_SOFT,
        GenartSource.AUTO_RAIN_GLASS,
        GenartSource.AUTO_DISTANT_HEADLIGHTS,
        GenartSource.AUTO_DASHBOARD_GLOW,
        GenartSource.AUTO_TUNNEL_LIGHT,
        GenartSource.AUTO_HORIZON_DRIVE,
    )

    val MOTO_IDS: Set<String> = setOf(
        GenartSource.MOTO_HEADLIGHT,
        GenartSource.MOTO_WIND_STREAKS,
        GenartSource.MOTO_ROAD_BLUR,
        GenartSource.MOTO_NIGHT_LEAN,
        GenartSource.MOTO_TAIL_GLOW,
        GenartSource.MOTO_DAWN_RIDE,
    )

    val AVION_IDS: Set<String> = setOf(
        GenartSource.AVION_HORIZON_CLOUDS,
        GenartSource.AVION_CONTRAIL,
        GenartSource.AVION_WING_GLOW,
        GenartSource.AVION_CABIN_WINDOW,
        GenartSource.AVION_SUNRISE_BAND,
        GenartSource.AVION_NIGHT_CITY,
    )

    val VELO_IDS: Set<String> = setOf(
        GenartSource.VELO_PATH_FIELDS,
        GenartSource.VELO_PARALLAX_TREES,
        GenartSource.VELO_DAWN_LANE,
        GenartSource.VELO_RAIN_ASPHALT,
        GenartSource.VELO_GOLDEN_HOUR,
        GenartSource.VELO_FOREST_CANOPY,
    )

    val TRAIN_IDS: Set<String> = setOf(
        GenartSource.TRAIN_WINDOW_BLUR,
        GenartSource.TRAIN_CATENARY,
        GenartSource.TRAIN_PLATFORM_GLOW,
        GenartSource.TRAIN_COUNTRYSIDE,
        GenartSource.TRAIN_NIGHT_CARRIAGE,
        GenartSource.TRAIN_TUNNEL_PULSE,
    )

    val US_VINTAGE_IDS: Set<String> = setOf(
        GenartSource.US_DINER_NEON,
        GenartSource.US_MOTEL_SIGN,
        GenartSource.US_ROUTE66_WASH,
        GenartSource.US_CRT_ARCADE,
        GenartSource.US_RETROWAVE,
        GenartSource.US_DRIVE_IN,
    )

    val STORM_STROBE_IDS: Set<String> = setOf(
        GenartSource.STORM_STROBE_FLASH,
        GenartSource.STORM_STROBE_SHEET,
        GenartSource.STORM_STROBE_PULSE,
        GenartSource.STORM_STROBE_FORK,
        GenartSource.STORM_STROBE_HORIZON,
        GenartSource.STORM_STROBE_CLOUD,
    )

    val PHYSARUM_IDS: Set<String> = setOf(
        GenartSource.PHYSARUM_VEINS,
        GenartSource.PHYSARUM_LATTICE,
        GenartSource.PHYSARUM_BLOOM,
        GenartSource.PHYSARUM_DRIFT,
        GenartSource.PHYSARUM_NODE,
        GenartSource.PHYSARUM_FILAMENT,
    )

    val REACTIONS_IDS: Set<String> = setOf(
        GenartSource.REACTION_SPOTS,
        GenartSource.REACTION_CORALS,
        GenartSource.REACTION_WAVES,
        GenartSource.REACTION_MITOSIS,
        GenartSource.REACTION_MAZE,
        GenartSource.REACTION_BLOBS,
    )

    fun engineIdsForTopic(topicSuffix: String): Set<String> = when (topicSuffix) {
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
        ILLUSIONS -> ILLUSIONS_IDS
        ANIMALS -> ANIMALS_IDS
        SEASONS -> SEASONS_IDS
        LIGHT -> LIGHT_IDS
        NIGHT -> NIGHT_IDS
        MATERIAL -> MATERIAL_IDS
        INTERIOR -> INTERIOR_IDS
        CRYSTAL -> CRYSTAL_IDS
        INK -> INK_IDS
        BOTANICAL -> BOTANICAL_IDS
        FIRE -> FIRE_IDS
        KALEIDO -> KALEIDO_IDS
        ZEN -> ZEN_IDS
        COAST -> COAST_IDS
        DREAM -> DREAM_IDS
        MICRO -> MICRO_IDS
        AUTO -> AUTO_IDS
        MOTO -> MOTO_IDS
        AVION -> AVION_IDS
        VELO -> VELO_IDS
        TRAIN -> TRAIN_IDS
        US_VINTAGE -> US_VINTAGE_IDS
        STORM_STROBE -> STORM_STROBE_IDS
        PHYSARUM -> PHYSARUM_IDS
        REACTIONS -> REACTIONS_IDS
        else -> emptySet()
    }

    /** Topic suffixes whose engine sets contain [engineId] (monetized genart topics only). */
    fun topicsCoveringEngine(engineId: String): List<String> =
        monetizedTopicSuffixes.filter { suffix ->
            suffix != FRACTAL && suffix != CUSTOM && engineId in engineIdsForTopic(suffix)
        }
}
