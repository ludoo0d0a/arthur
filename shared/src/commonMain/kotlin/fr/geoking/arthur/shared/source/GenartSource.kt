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
        const val GLASS_MARBLES = "genart.glassmarbles"
        const val PANE_REFLECTIONS = "genart.panereflections"
        const val MARBLE_CAUSTICS = "genart.marblecaustics"
        const val BILLOWING_CLOTH = "genart.billowcloth"
        const val WIND_CURTAINS = "genart.windcurtains"
        const val MARBLE_DRIFT = "genart.marble"
        const val CANYON_LIGHT = "genart.canyonlight"
        const val DEPTH_SHAFTS = "genart.depthshafts"
        const val GLASS_ORB = "genart.glassorb"
        const val SOFT_RAY_ORBS = "genart.rayorbs"
        const val PRISM_CAVE = "genart.prismcave"
        const val SOAP_FILM = "genart.soapfilm"
        const val LIT_LATTICE = "genart.litlattice"
        const val RETRO_WAVE = "genart.retrowave"
        const val TERRAIN_MAKER = "genart.terrainmaker"
        const val MULTI_FLAMES = "genart.multiflames"
        const val AIR_BUBBLES = "genart.airbubbles"
        const val COURT_BOUNCE = "genart.courtbounce"
        const val COPPER_BARS = "genart.copperbars"
        const val NEON_HIGHWAY = "genart.neonhighway"
        const val SOFT_DROP = "genart.softdrop"
        const val VECTOR_ROCKS = "genart.vectorrocks"
        const val BRICK_CASCADE = "genart.brickcascade"
        const val PIXEL_FORMATION = "genart.pixelformation"
        const val CRT_BOOT = "genart.crtboot"
        const val LWP_PLASMA = "genart.lwpplasma"
        const val LWP_STARFIELD = "genart.lwpstarfield"
        const val LWP_VORONOI = "genart.lwpvoronoi"
        const val SHADERLAY_NEON_GRID = "genart.neongrid"
        const val SHADERLAY_VORTEX_TUNNEL = "genart.vortextunnel"
        const val PPSSPP_CRT_SCANLINES = "genart.crtscanlines"
        const val PPSSPP_CEL_SHADER = "genart.celshader"
        const val PPSSPP_BLOOM_VIGNETTE = "genart.bloomvignette"
        const val PAPER_SIMPLEX_NOISE = "genart.simplexnoise"
        const val PAPER_NEURO_NOISE = "genart.neuronnoise"
        const val PAPER_GRAIN_GRADIENT = "genart.graingradient"
        const val SHADY_NOODLE_ZOOM = "genart.noodlezoom"
        const val SHADY_WARP_SPEED = "genart.warpspeed"
        const val SHADY_MARBLED_PAPER = "genart.marbledpaper"
        const val MILKY_WAY = "genart.milkyway"
        const val PLASMA_FLOW = "genart.plasmaflow"
        const val ROBOTIC_EYE = "genart.roboticeye"
        const val PENROSE_TRIANGLE = "genart.penrosetriangle"
        const val PENROSE_STAIRS = "genart.penrosestairs"
        const val IMPOSSIBLE_CUBE = "genart.impossiblecube"
        const val NECKER_CUBE = "genart.neckercube"
        const val CAFE_WALL = "genart.cafewall"
        const val KANIZSA_TRIANGLE = "genart.kanizsa"
        const val CHECKER_SHADOW = "genart.checkershadow"
        const val CAT_WINDOW = "genart.catwindow"
        const val CAT_SLEEP = "genart.catsleep"
        const val DOG_NAP = "genart.dognap"
        const val DOG_DISTANT_WALK = "genart.dogdistantwalk"
        const val PANDA_SIT = "genart.pandasit"
        const val PANDA_BAMBOO = "genart.pandabamboo"
        const val CAT_TAIL = "genart.cattail"
        const val DOG_STRETCH = "genart.dogstretch"
        const val PANDA_ROLL = "genart.pandaroll"
        const val SEASON_SPRING_WASH = "genart.seasonspringwash"
        const val SEASON_AUTUMN_WASH = "genart.seasonautumnwash"
        const val SEASON_WINTER_WASH = "genart.seasonwinterwash"
        const val FROSTED_WINDOW_DAWN = "genart.frostedwindowdawn"
        const val PAPER_LANTERN = "genart.paperlantern"
        const val HEARTH_GLOW = "genart.hearthglow"
        const val VOLUMETRIC_STAIRWELL = "genart.volumetricstairwell"
        const val WINDOW_DUST_MOTES = "genart.windowdustmotes"
        const val BOOKSHELF_GLOW = "genart.bookshelfglow"
        const val FLOOR_LAMP_POOL = "genart.floorlamppool"
        const val CRYSTAL_CAVERN_GLOW = "genart.crystalcavernglow"
        const val INK_BLOOM = "genart.inkbloom"
        const val SUMI_WASH = "genart.sumiwash"
        const val INK_TENDRIL = "genart.inktendril"
        const val KALEIDO_PETAL = "genart.kaleidopetal"
        const val KALEIDO_MIRROR = "genart.kaleidomirror"
        const val KALEIDO_ORBIT = "genart.kaleidoorbit"
        const val ZEN_EN_SO = "genart.zenenso"
        const val ZEN_STONE_RIPPLES = "genart.zenstoneripples"
        const val ZEN_INCENSE = "genart.zenincense"
        const val OBSIDIAN_SHORE = "genart.obsidianshore"
        const val TIDE_FOAM = "genart.tidefoam"
        const val COAST_CLIFF_HAZE = "genart.coastcliffhaze"
        const val DREAM_VEIL = "genart.dreamveil"
        const val LUCID_ORBS = "genart.lucidorbs"
        const val SLEEP_PARALLAX = "genart.sleepparallax"
        const val MICRO_DEW = "genart.microdew"
        const val MICRO_FIBER = "genart.microfiber"
        const val MICRO_DUST = "genart.microdust"
        const val MICRO_POLLEN_CLOSE = "genart.micropollenclose"
        const val MICRO_BUBBLE_SKIN = "genart.microbubbleskin"
        const val AUTO_DASH_SOFT = "genart.autodashsoft"
        const val AUTO_RAIN_GLASS = "genart.autorainonglass"
        const val AUTO_DISTANT_HEADLIGHTS = "genart.autodistantheadlights"
        const val AUTO_DASHBOARD_GLOW = "genart.autodashboardglow"
        const val AUTO_TUNNEL_LIGHT = "genart.autotunnellight"
        const val AUTO_HORIZON_DRIVE = "genart.autohorizondrive"
        const val MOTO_HEADLIGHT = "genart.motoheadlight"
        const val MOTO_WIND_STREAKS = "genart.motowindstreaks"
        const val MOTO_ROAD_BLUR = "genart.motoroadblur"
        const val MOTO_NIGHT_LEAN = "genart.motonightlean"
        const val MOTO_TAIL_GLOW = "genart.mototailglow"
        const val MOTO_DAWN_RIDE = "genart.motodawnride"
        const val AVION_HORIZON_CLOUDS = "genart.avionhorizonclouds"
        const val AVION_CONTRAIL = "genart.avioncontrail"
        const val AVION_WING_GLOW = "genart.avionwingglow"
        const val AVION_CABIN_WINDOW = "genart.avioncabinwindow"
        const val AVION_SUNRISE_BAND = "genart.avionsunriseband"
        const val AVION_NIGHT_CITY = "genart.avionnightcity"
        const val VELO_PATH_FIELDS = "genart.velopathfields"
        const val VELO_PARALLAX_TREES = "genart.veloparallaxtrees"
        const val VELO_DAWN_LANE = "genart.velodawnlane"
        const val VELO_RAIN_ASPHALT = "genart.velorainasphalt"
        const val VELO_GOLDEN_HOUR = "genart.velogoldenhour"
        const val VELO_FOREST_CANOPY = "genart.veloforestcanopy"
        const val TRAIN_WINDOW_BLUR = "genart.trainwindowblur"
        const val TRAIN_CATENARY = "genart.traincatenary"
        const val TRAIN_PLATFORM_GLOW = "genart.trainplatformglow"
        const val TRAIN_COUNTRYSIDE = "genart.traincountryside"
        const val TRAIN_NIGHT_CARRIAGE = "genart.trainnightcarriage"
        const val TRAIN_TUNNEL_PULSE = "genart.traintunnelpulse"
        const val US_DINER_NEON = "genart.usdinerneon"
        const val US_MOTEL_SIGN = "genart.usmotelsign"
        const val US_ROUTE66_WASH = "genart.usroute66wash"
        const val US_CRT_ARCADE = "genart.uscrtarcade"
        const val US_RETROWAVE = "genart.usretrowave"
        const val US_DRIVE_IN = "genart.usdrivein"
        const val STORM_STROBE_FLASH = "genart.stormstrobeflash"
        const val STORM_STROBE_SHEET = "genart.stormstrobesheet"
        const val STORM_STROBE_PULSE = "genart.stormstrobepulse"
        const val STORM_STROBE_FORK = "genart.stormstrobefork"
        const val STORM_STROBE_HORIZON = "genart.stormstrobehorizon"
        const val STORM_STROBE_CLOUD = "genart.stormstrobecloud"
        const val PHYSARUM_VEINS = "genart.physarumveins"
        const val PHYSARUM_LATTICE = "genart.physarumlattice"
        const val PHYSARUM_BLOOM = "genart.physarumbloom"
        const val PHYSARUM_DRIFT = "genart.physarumdrift"
        const val PHYSARUM_NODE = "genart.physarumnode"
        const val PHYSARUM_FILAMENT = "genart.physarumfilament"
        const val REACTION_SPOTS = "genart.reactionspots"
        const val REACTION_CORALS = "genart.reactioncorals"
        const val REACTION_WAVES = "genart.reactionwaves"
        const val REACTION_MITOSIS = "genart.reactionmitosis"
        const val REACTION_MAZE = "genart.reactionmaze"
        const val REACTION_BLOBS = "genart.reactionblobs"
        const val SOFT_TIDE_RIPPLES = "genart.softtideripples"
        const val HARBOR_MIST = "genart.harbormist"

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
            entry(BILLOWING_CLOTH, "#111 - Billowing Cloth"),
            entry(WIND_CURTAINS, "#112 - Wind Curtains"),
            entry(GLASS_MARBLES, "#113 - Glass Marbles"),
            entry(PANE_REFLECTIONS, "#114 - Pane Reflections"),
            entry(MARBLE_CAUSTICS, "#115 - Marble Caustics"),
            entry(MARBLE_DRIFT, "#116 - Marble Drift"),
            entry(CANYON_LIGHT, "#117 - Canyon Light Cut"),
            entry(DEPTH_SHAFTS, "#118 - Underwater Light Shafts"),
            entry(GLASS_ORB, "#119 - Glass Orb on Desk"),
            entry(SOFT_RAY_ORBS, "#120 - Soft Raymarch Orbs"),
            entry(PRISM_CAVE, "#121 - Crystal Prism Cave"),
            entry(SOAP_FILM, "#122 - Iridescent Soap Film"),
            entry(LIT_LATTICE, "#123 - Lit Wire Lattice Depth"),
            entry(RETRO_WAVE, "#124 - Retro-futurist Demo"),
            entry(TERRAIN_MAKER, "#125 - Landscape Terrain Maker"),
            entry(MULTI_FLAMES, "#126 - Multiple Fires"),
            entry(AIR_BUBBLES, "#127 - Underwater Air Bubbles"),
            entry(COURT_BOUNCE, "#128 - Court Bounce"),
            entry(COPPER_BARS, "#129 - Copper Bars"),
            entry(NEON_HIGHWAY, "#130 - Neon Highway"),
            entry(SOFT_DROP, "#131 - Soft Drop"),
            entry(VECTOR_ROCKS, "#132 - Vector Rocks"),
            entry(BRICK_CASCADE, "#133 - Brick Cascade"),
            entry(PIXEL_FORMATION, "#134 - Pixel Formation"),
            entry(CRT_BOOT, "#135 - CRT Boot"),
            entry(LWP_PLASMA, "#136 - LWP Harmonic Plasma"),
            entry(LWP_STARFIELD, "#137 - LWP 3D Warp Starfield"),
            entry(LWP_VORONOI, "#138 - LWP Voronoi Cellular Caustics"),
            entry(SHADERLAY_NEON_GRID, "#139 - Shaderlay Synthwave Neon Grid"),
            entry(SHADERLAY_VORTEX_TUNNEL, "#140 - Shaderlay Vortex Warp Tunnel"),
            entry(PPSSPP_CRT_SCANLINES, "#141 - PPSSPP CRT Scanlines"),
            entry(PPSSPP_CEL_SHADER, "#142 - PPSSPP Cel-Shader"),
            entry(PPSSPP_BLOOM_VIGNETTE, "#143 - PPSSPP Bloom Vignette"),
            entry(PAPER_SIMPLEX_NOISE, "#144 - Paper Simplex Noise Flow"),
            entry(PAPER_NEURO_NOISE, "#145 - Paper Neuro-Noise Waves"),
            entry(PAPER_GRAIN_GRADIENT, "#146 - Paper Grain Gradient"),
            entry(SHADY_NOODLE_ZOOM, "#147 - Shady Noodle Zoom Tunnel"),
            entry(SHADY_WARP_SPEED, "#148 - Shady Warp Speed"),
            entry(SHADY_MARBLED_PAPER, "#149 - Shady Marbled Paper"),
            entry(MILKY_WAY, "#150 - Milky Way Galaxy"),
            entry(PLASMA_FLOW, "#151 - Plasma Flow Streamlines"),
            entry(ROBOTIC_EYE, "#152 - Cybernetic Robotic Eye"),
            entry(PENROSE_TRIANGLE, "#153 - Penrose Triangle"),
            entry(PENROSE_STAIRS, "#154 - Penrose Stairs"),
            entry(IMPOSSIBLE_CUBE, "#155 - Impossible Cube"),
            entry(NECKER_CUBE, "#156 - Necker Cube"),
            entry(CAFE_WALL, "#157 - Café Wall"),
            entry(KANIZSA_TRIANGLE, "#158 - Kanizsa Triangle"),
            entry(CHECKER_SHADOW, "#159 - Checker Shadow"),
            entry(CAT_WINDOW, "#160 - Cat at Window"),
            entry(CAT_SLEEP, "#161 - Sleeping Cat"),
            entry(DOG_NAP, "#162 - Dog Nap"),
            entry(DOG_DISTANT_WALK, "#163 - Distant Dog Walk"),
            entry(PANDA_SIT, "#164 - Sitting Panda"),
            entry(PANDA_BAMBOO, "#165 - Panda and Bamboo"),
            entry(CAT_TAIL, "#166 - Cat Tail Sway"),
            entry(DOG_STRETCH, "#167 - Dog Stretch"),
            entry(PANDA_ROLL, "#168 - Panda Roll"),
            entry(SEASON_SPRING_WASH, "#169 - Season Spring Wash"),
            entry(SEASON_AUTUMN_WASH, "#170 - Season Autumn Wash"),
            entry(SEASON_WINTER_WASH, "#171 - Season Winter Wash"),
            entry(FROSTED_WINDOW_DAWN, "#172 - Frosted Window Dawn"),
            entry(PAPER_LANTERN, "#173 - Paper Lantern Glow"),
            entry(HEARTH_GLOW, "#174 - Hearth Glow"),
            entry(VOLUMETRIC_STAIRWELL, "#175 - Volumetric Stairwell"),
            entry(WINDOW_DUST_MOTES, "#176 - Window Dust Motes"),
            entry(BOOKSHELF_GLOW, "#177 - Bookshelf Glow"),
            entry(FLOOR_LAMP_POOL, "#178 - Floor Lamp Pool"),
            entry(CRYSTAL_CAVERN_GLOW, "#179 - Crystal Cavern Glow"),
            entry(INK_BLOOM, "#180 - Ink Bloom"),
            entry(SUMI_WASH, "#181 - Sumi Wash"),
            entry(INK_TENDRIL, "#182 - Ink Tendril"),
            entry(KALEIDO_PETAL, "#183 - Kaleido Petal"),
            entry(KALEIDO_MIRROR, "#184 - Kaleido Mirror"),
            entry(KALEIDO_ORBIT, "#185 - Kaleido Orbit"),
            entry(ZEN_EN_SO, "#186 - Zen En So"),
            entry(ZEN_STONE_RIPPLES, "#187 - Zen Stone Ripples"),
            entry(ZEN_INCENSE, "#188 - Zen Incense"),
            entry(OBSIDIAN_SHORE, "#189 - Obsidian Shore"),
            entry(TIDE_FOAM, "#190 - Tide Foam"),
            entry(COAST_CLIFF_HAZE, "#191 - Coast Cliff Haze"),
            entry(DREAM_VEIL, "#192 - Dream Veil"),
            entry(LUCID_ORBS, "#193 - Lucid Orbs"),
            entry(SLEEP_PARALLAX, "#194 - Sleep Parallax"),
            entry(MICRO_DEW, "#195 - Micro Dew"),
            entry(MICRO_FIBER, "#196 - Micro Fiber"),
            entry(MICRO_DUST, "#197 - Micro Dust"),
            entry(MICRO_POLLEN_CLOSE, "#198 - Micro Pollen Close"),
            entry(MICRO_BUBBLE_SKIN, "#199 - Micro Bubble Skin"),
            entry(AUTO_DASH_SOFT, "#200 - Auto Dash Soft"),
            entry(AUTO_RAIN_GLASS, "#201 - Auto Rain on Glass"),
            entry(AUTO_DISTANT_HEADLIGHTS, "#202 - Auto Distant Headlights"),
            entry(AUTO_DASHBOARD_GLOW, "#203 - Auto Dashboard Glow"),
            entry(AUTO_TUNNEL_LIGHT, "#204 - Auto Tunnel Light Rhythm"),
            entry(AUTO_HORIZON_DRIVE, "#205 - Auto Horizon Drive"),
            entry(MOTO_HEADLIGHT, "#206 - Moto Single Headlight"),
            entry(MOTO_WIND_STREAKS, "#207 - Moto Wind Streaks"),
            entry(MOTO_ROAD_BLUR, "#208 - Moto Road Blur"),
            entry(MOTO_NIGHT_LEAN, "#209 - Moto Night Lean"),
            entry(MOTO_TAIL_GLOW, "#210 - Moto Tail Glow"),
            entry(MOTO_DAWN_RIDE, "#211 - Moto Dawn Ride"),
            entry(AVION_HORIZON_CLOUDS, "#212 - Avion Horizon Clouds"),
            entry(AVION_CONTRAIL, "#213 - Avion Contrail"),
            entry(AVION_WING_GLOW, "#214 - Avion Wing Glow"),
            entry(AVION_CABIN_WINDOW, "#215 - Avion Cabin Window"),
            entry(AVION_SUNRISE_BAND, "#216 - Avion Sunrise Band"),
            entry(AVION_NIGHT_CITY, "#217 - Avion Night City Below"),
            entry(VELO_PATH_FIELDS, "#218 - Velo Path and Fields"),
            entry(VELO_PARALLAX_TREES, "#219 - Velo Parallax Trees"),
            entry(VELO_DAWN_LANE, "#220 - Velo Dawn Lane"),
            entry(VELO_RAIN_ASPHALT, "#221 - Velo Rain Asphalt"),
            entry(VELO_GOLDEN_HOUR, "#222 - Velo Golden Hour"),
            entry(VELO_FOREST_CANOPY, "#223 - Velo Forest Canopy"),
            entry(TRAIN_WINDOW_BLUR, "#224 - Train Window Blur"),
            entry(TRAIN_CATENARY, "#225 - Train Soft Catenary"),
            entry(TRAIN_PLATFORM_GLOW, "#226 - Train Platform Glow"),
            entry(TRAIN_COUNTRYSIDE, "#227 - Train Countryside Wash"),
            entry(TRAIN_NIGHT_CARRIAGE, "#228 - Train Night Carriage"),
            entry(TRAIN_TUNNEL_PULSE, "#229 - Train Tunnel Pulse"),
            entry(US_DINER_NEON, "#230 - US Diner Neon"),
            entry(US_MOTEL_SIGN, "#231 - US Motel Sign"),
            entry(US_ROUTE66_WASH, "#232 - US Route 66 Desert Wash"),
            entry(US_CRT_ARCADE, "#233 - US CRT Arcade"),
            entry(US_RETROWAVE, "#234 - US Retrowave Horizon"),
            entry(US_DRIVE_IN, "#235 - US Drive-In Glow"),
            entry(STORM_STROBE_FLASH, "#236 - Storm Strobe Flash"),
            entry(STORM_STROBE_SHEET, "#237 - Storm Strobe Sheet Lightning"),
            entry(STORM_STROBE_PULSE, "#238 - Storm Strobe Pulse"),
            entry(STORM_STROBE_FORK, "#239 - Storm Strobe Fork"),
            entry(STORM_STROBE_HORIZON, "#240 - Storm Strobe Horizon"),
            entry(STORM_STROBE_CLOUD, "#241 - Storm Strobe Cloud Burst"),
            entry(PHYSARUM_VEINS, "#242 - Physarum Veins"),
            entry(PHYSARUM_LATTICE, "#243 - Physarum Lattice"),
            entry(PHYSARUM_BLOOM, "#244 - Physarum Bloom"),
            entry(PHYSARUM_DRIFT, "#245 - Physarum Drift"),
            entry(PHYSARUM_NODE, "#246 - Physarum Node Nest"),
            entry(PHYSARUM_FILAMENT, "#247 - Physarum Filament"),
            entry(REACTION_SPOTS, "#248 - Reaction Diffusion Spots"),
            entry(REACTION_CORALS, "#249 - Reaction Diffusion Corals"),
            entry(REACTION_WAVES, "#250 - Reaction Diffusion Waves"),
            entry(REACTION_MITOSIS, "#251 - Reaction Diffusion Mitosis"),
            entry(REACTION_MAZE, "#252 - Reaction Diffusion Maze"),
            entry(REACTION_BLOBS, "#253 - Reaction Soft Blobs"),
            entry(SOFT_TIDE_RIPPLES, "#254 - Soft Tide Ripples"),
            entry(HARBOR_MIST, "#255 - Harbor Mist"),
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
