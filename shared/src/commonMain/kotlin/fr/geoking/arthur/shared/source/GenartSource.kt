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
        const val TADPOLE_NEBULA = "genart.tadpolenebula"

        fun defaultCatalog(): List<Artwork> = listOf(
            entry(PARTICLES, "#1 - Particules dérivantes"),
            entry(PSEUDO3D, "#2 - Treillis de fils"),
            entry(SOFT_SHADOWS, "#3 - Ombres douces"),
            entry(TUNNEL, "#4 - Tunnel fuyant"),
            entry(TONAL_GEOMETRY, "#5 - Géométrie tonale"),
            entry(SPHERE, "#6 - Sphère en orbite"),
            entry(WAVES, "#7 - Vagues en couches"),
            entry(MICRO, "#8 - Rayons volumétriques"),
            entry(SNOW, "#9 - Neige qui tombe"),
            entry(GRASS, "#10 - Herbe au vent"),
            entry(BIRD_FLOCK, "#11 - Vol d'oiseaux"),
            entry(MOUNTAINS, "#12 - Montagnes en couches"),
            entry(AURORA, "#13 - Rubans d'aurore"),
            entry(POND_RIPPLES, "#14 - Rides d'étang"),
            entry(FALLING_LEAVES, "#15 - Feuilles qui tombent"),
            entry(BREATH_CIRCLES, "#16 - Cercles de souffle"),
            entry(FIRE_EMBERS, "#17 - Braises de cheminée"),
            entry(DUNES, "#18 - Dunes au vent"),
            entry(CONSTELLATION, "#19 - Constellation scintillante"),
            entry(CLOUDS, "#20 - Nuages dérivants"),
            entry(RAIN, "#21 - Pluie douce"),
            entry(FOG, "#22 - Brouillard doux"),
            entry(FISH_SCHOOL, "#23 - Banc de poissons"),
            entry(FIREFLIES, "#24 - Lucioles"),
            entry(SUNBEAMS, "#25 - Rayons dans la brume"),
            entry(METEORS, "#26 - Météores clairsemés"),
            entry(BUBBLES, "#27 - Bulles montantes"),
            entry(CHERRY_BLOSSOMS, "#28 - Pétales de cerisier"),
            entry(RIBBONS, "#29 - Rubans doux"),
            entry(NEBULA, "#30 - Nébuleuse dérivante"),
            entry(BLOBS, "#31 - Blobs morphing"),
            entry(NOISE_FIELD, "#32 - Champ de bruit doux"),
            entry(VORONOI, "#33 - Lavis Voronoi"),
            entry(SILK, "#34 - Plis de soie"),
            entry(GRADIENT_MESH, "#35 - Maillage dégradé"),
            entry(ARC_MOSAIC, "#36 - Mosaïque d'arcs"),
            entry(STORM, "#37 - Orage doux"),
            entry(STAR_FIELD, "#38 - Champ d'étoiles parallaxe"),
            entry(SOLAR_SYSTEM, "#39 - Système solaire"),
            entry(CANDLE_EMBER, "#40 - Braise de bougie"),
            entry(RAINBOW, "#41 - Arc-en-ciel doux"),
            entry(SMOG, "#42 - Brume urbaine"),
            entry(SMOKE, "#43 - Fumée montante"),
            entry(HEAT_HAZE, "#44 - Mirage de chaleur"),
            entry(SUNSHINE, "#45 - Soleil doux"),
            entry(LIGHT_DRIZZLE, "#46 - Bruine légère"),
            entry(STEAM_CURL, "#47 - Vapeur enroulée"),
            entry(DRIFTING_POLLEN, "#48 - Pollen dérivant"),
            entry(LANDSLIDE_DUST, "#49 - Poussière d'éboulement"),
            entry(PEBBLE_SHORE_WASH, "#50 - Lavis de galets"),
            entry(FROST_CRYSTALS, "#51 - Premiers cristaux de givre"),
            entry(ION_TRAIL, "#52 - Traînée d'ions"),
            entry(ANT_TRAILS, "#53 - Pistes de fourmis"),
            entry(SLEEPING_PET, "#54 - Silhouette d'animal endormi"),
            entry(WARP_STREAK, "#55 - Traînées warp"),
            entry(SPACE_STATION_DRIFT, "#56 - Station spatiale dérivante"),
            entry(MOONLIGHT_RIPPLES, "#57 - Rides au clair de lune"),
            entry(ECLIPSE_CORONA, "#58 - Couronne d'éclipse"),
            entry(INK_IN_WATER, "#59 - Encre dans l'eau"),
            entry(WIND_CHIME, "#60 - Silhouette de carillon"),
            entry(DAY_NIGHT_WASH, "#61 - Lavis jour-nuit"),
            entry(TUMBLEWEED_DRIFT, "#62 - Virevoltant dérivant"),
            entry(CITY_LIGHTS, "#64 - Lumières de ville la nuit"),
            entry(MOSS_GROWTH, "#65 - Mousses qui poussent"),
            entry(TERRARIUM_DRIP, "#66 - Goutte de terrarium"),
            entry(AQUARIUM, "#67 - Aquarium"),
            entry(FIELDS, "#68 - Champs doux"),
            entry(RAIN_ON_GLASS, "#69 - Pluie sur vitre"),
            entry(REEDS, "#70 - Roseaux"),
            entry(CANYON_DUNES, "#71 - Dunes de canyon"),
            entry(CONTINENTS, "#72 - Continents"),
            entry(ROADS, "#73 - Routes"),
            entry(RIVERS, "#74 - Rivières"),
            entry(TREE, "#75 - Arbre au vent"),
            entry(FLOWER, "#76 - Éclosion de fleur"),
            entry(LAKE, "#77 - Surface de lac"),
            entry(ASTEROIDS, "#78 - Astéroïdes"),
            entry(WATERFALL_MIST, "#79 - Brume de cascade"),
            entry(SOFT_WIND_STREAKS, "#80 - Traînées de vent doux"),
            entry(GERSTNER_OCEAN, "#81 - Houle océanique"),
            entry(SPIRAL_GALAXY, "#82 - Galaxie spirale dérivante"),
            entry(DATA_HORIZON, "#83 - Horizon de données"),
            entry(SOFT_CAUSTICS, "#84 - Caustiques douces"),
            entry(LOW_FREQ_NOISE_FIELD, "#85 - Champ de bruit basse fréquence"),
            entry(CHROMATIC_BLOBS, "#87 - Blobs chromatiques"),
            entry(PRISMATIC_SHADOWS, "#88 - Ombres prismatiques"),
            entry(AURORA_WASH, "#89 - Lavis d'aurore"),
            entry(VORTEX_GLOW, "#90 - Lueur de vortex"),
            entry(SPECTRAL_FOLDS, "#91 - Plis spectraux"),
            entry(HALO_ECLIPSE, "#92 - Halo d'éclipse"),
            entry(FIRE, "#86 - Feu de forêt"),
            entry(PAPER_CUT_PACK, "#87 - Découpes de papier"),
            entry(DIAMOND_WEAVE, "#88 - Tissage diamant"),
            entry(MATRIX, "#93 - Pluie Matrix"),
            entry(DRIFTING_HALOS, "#94 - Halos mobiles"),
            entry(SUPERDRIVE, "#95 - Vibes Superdrive"),
            entry(FIREWORKS, "#96 - Feux d'artifice doux"),
            entry(ATMOSPHERIC_ASTEROID, "#97 - Astéroïde atmosphérique"),
            entry(SATURN_RINGS, "#98 - Saturne et ses anneaux"),
            entry(LAVA_SUN, "#99 - Soleil de lave"),
            entry(MOON, "#100 - Pleine lune"),
            entry(SPIRAL_MANDALA, "#101 - Mandala spirale"),
            entry(FLOW_RIBBONS, "#102 - Rubans fluides"),
            entry(CLIFFORD_WASH, "#103 - Lavis Clifford"),
            entry(PLASMA_NOVA, "#104 - Nova plasma"),
            entry(ENERGY_TENDRILS, "#105 - Vrilles d'énergie"),
            entry(INTERFERENCE_WASH, "#106 - Lavis d'interférence"),
            entry(NESTED_PACK, "#107 - Pack imbriqué"),
            entry(SILK_BLOOM, "#108 - Éclosion de soie"),
            entry(CROSSING_SPOTLIGHTS, "#109 - Projecteurs croisés"),
            entry(LAMP_IN_DARKNESS, "#110 - Lampe dans l'obscurité"),
            entry(BILLOWING_CLOTH, "#111 - Tissu ondulant"),
            entry(WIND_CURTAINS, "#112 - Rideaux au vent"),
            entry(GLASS_MARBLES, "#113 - Billes de verre"),
            entry(PANE_REFLECTIONS, "#114 - Reflets de vitre"),
            entry(MARBLE_CAUSTICS, "#115 - Caustiques de marbre"),
            entry(MARBLE_DRIFT, "#116 - Marbre dérivant"),
            entry(CANYON_LIGHT, "#117 - Coup de lumière en canyon"),
            entry(DEPTH_SHAFTS, "#118 - Puits de lumière sous-marins"),
            entry(GLASS_ORB, "#119 - Orbe de verre sur bureau"),
            entry(SOFT_RAY_ORBS, "#120 - Orbes raymarch doux"),
            entry(PRISM_CAVE, "#121 - Grotte de prismes"),
            entry(SOAP_FILM, "#122 - Film de savon irisé"),
            entry(LIT_LATTICE, "#123 - Treillis filaire éclairé"),
            entry(RETRO_WAVE, "#124 - Démo rétro-futuriste"),
            entry(TERRAIN_MAKER, "#125 - Générateur de terrain"),
            entry(MULTI_FLAMES, "#126 - Feux multiples"),
            entry(AIR_BUBBLES, "#127 - Bulles d'air sous-marines"),
            entry(COURT_BOUNCE, "#128 - Rebond de court"),
            entry(COPPER_BARS, "#129 - Barres de cuivre"),
            entry(NEON_HIGHWAY, "#130 - Autoroute néon"),
            entry(SOFT_DROP, "#131 - Goutte douce"),
            entry(VECTOR_ROCKS, "#132 - Rochers vectoriels"),
            entry(BRICK_CASCADE, "#133 - Cascade de briques"),
            entry(PIXEL_FORMATION, "#134 - Formation de pixels"),
            entry(CRT_BOOT, "#135 - Démarrage CRT"),
            entry(LWP_PLASMA, "#136 - Plasma harmonique LWP"),
            entry(LWP_STARFIELD, "#137 - Champ d'étoiles warp LWP"),
            entry(LWP_VORONOI, "#138 - Caustiques cellulaires Voronoi LWP"),
            entry(SHADERLAY_NEON_GRID, "#139 - Grille néon synthwave Shaderlay"),
            entry(SHADERLAY_VORTEX_TUNNEL, "#140 - Tunnel vortex warp Shaderlay"),
            entry(PPSSPP_CRT_SCANLINES, "#141 - Scanlines CRT PPSSPP"),
            entry(PPSSPP_CEL_SHADER, "#142 - Cel-shader PPSSPP"),
            entry(PPSSPP_BLOOM_VIGNETTE, "#143 - Bloom vignette PPSSPP"),
            entry(PAPER_SIMPLEX_NOISE, "#144 - Flux bruit simplex papier"),
            entry(PAPER_NEURO_NOISE, "#145 - Vagues neuro-bruit papier"),
            entry(PAPER_GRAIN_GRADIENT, "#146 - Dégradé grain papier"),
            entry(SHADY_NOODLE_ZOOM, "#147 - Tunnel noodle zoom Shady"),
            entry(SHADY_WARP_SPEED, "#148 - Vitesse warp Shady"),
            entry(SHADY_MARBLED_PAPER, "#149 - Papier marbré Shady"),
            entry(MILKY_WAY, "#150 - Voie lactée"),
            entry(PLASMA_FLOW, "#151 - Lignes de flux plasma"),
            entry(ROBOTIC_EYE, "#152 - Œil robotique cybernétique"),
            entry(PENROSE_TRIANGLE, "#153 - Triangle de Penrose"),
            entry(PENROSE_STAIRS, "#154 - Escalier de Penrose"),
            entry(IMPOSSIBLE_CUBE, "#155 - Cube impossible"),
            entry(NECKER_CUBE, "#156 - Cube de Necker"),
            entry(CAFE_WALL, "#157 - Mur du café"),
            entry(KANIZSA_TRIANGLE, "#158 - Triangle de Kanizsa"),
            entry(CHECKER_SHADOW, "#159 - Ombre en damier"),
            entry(CAT_WINDOW, "#160 - Chat à la fenêtre"),
            entry(CAT_SLEEP, "#161 - Chat endormi"),
            entry(DOG_NAP, "#162 - Sieste de chien"),
            entry(DOG_DISTANT_WALK, "#163 - Chien au loin"),
            entry(PANDA_SIT, "#164 - Panda assis"),
            entry(PANDA_BAMBOO, "#165 - Panda et bambou"),
            entry(CAT_TAIL, "#166 - Queue de chat qui balance"),
            entry(DOG_STRETCH, "#167 - Étirement de chien"),
            entry(PANDA_ROLL, "#168 - Panda qui roule"),
            entry(SEASON_SPRING_WASH, "#169 - Lavis de printemps"),
            entry(SEASON_AUTUMN_WASH, "#170 - Lavis d'automne"),
            entry(SEASON_WINTER_WASH, "#171 - Lavis d'hiver"),
            entry(FROSTED_WINDOW_DAWN, "#172 - Fenêtre givrée à l'aube"),
            entry(PAPER_LANTERN, "#173 - Lueur de lanterne en papier"),
            entry(HEARTH_GLOW, "#174 - Lueur d'âtre"),
            entry(VOLUMETRIC_STAIRWELL, "#175 - Cage d'escalier volumétrique"),
            entry(WINDOW_DUST_MOTES, "#176 - Poussières dans la lumière"),
            entry(BOOKSHELF_GLOW, "#177 - Lueur d'étagère"),
            entry(FLOOR_LAMP_POOL, "#178 - Mare de lampe sur pied"),
            entry(CRYSTAL_CAVERN_GLOW, "#179 - Lueur de caverne de cristal"),
            entry(INK_BLOOM, "#180 - Floraison d'encre"),
            entry(SUMI_WASH, "#181 - Lavis sumi"),
            entry(INK_TENDRIL, "#182 - Vrille d'encre"),
            entry(KALEIDO_PETAL, "#183 - Pétale kaléido"),
            entry(KALEIDO_MIRROR, "#184 - Miroir kaléido"),
            entry(KALEIDO_ORBIT, "#185 - Orbite kaléido"),
            entry(ZEN_EN_SO, "#186 - Enso zen"),
            entry(ZEN_STONE_RIPPLES, "#187 - Rides de pierre zen"),
            entry(ZEN_INCENSE, "#188 - Encens zen"),
            entry(OBSIDIAN_SHORE, "#189 - Rivage d'obsidienne"),
            entry(TIDE_FOAM, "#190 - Écume de marée"),
            entry(COAST_CLIFF_HAZE, "#191 - Brume de falaise"),
            entry(DREAM_VEIL, "#192 - Voile de rêve"),
            entry(LUCID_ORBS, "#193 - Orbes lucides"),
            entry(SLEEP_PARALLAX, "#194 - Parallaxe de sommeil"),
            entry(MICRO_DEW, "#195 - Micro-rosée"),
            entry(MICRO_FIBER, "#196 - Micro-fibre"),
            entry(MICRO_DUST, "#197 - Micro-poussière"),
            entry(MICRO_POLLEN_CLOSE, "#198 - Micro-pollen de près"),
            entry(MICRO_BUBBLE_SKIN, "#199 - Peau de micro-bulle"),
            entry(AUTO_DASH_SOFT, "#200 - Voyage auto — tableau de bord"),
            entry(AUTO_RAIN_GLASS, "#201 - Voyage auto — pluie sur vitre"),
            entry(AUTO_DISTANT_HEADLIGHTS, "#202 - Voyage auto — phares au loin"),
            entry(AUTO_DASHBOARD_GLOW, "#203 - Voyage auto — lueur du tableau"),
            entry(AUTO_TUNNEL_LIGHT, "#204 - Voyage auto — rythme du tunnel"),
            entry(AUTO_HORIZON_DRIVE, "#205 - Voyage auto — horizon"),
            entry(MOTO_HEADLIGHT, "#206 - Voyage moto — phare unique"),
            entry(MOTO_WIND_STREAKS, "#207 - Voyage moto — traînées de vent"),
            entry(MOTO_ROAD_BLUR, "#208 - Voyage moto — flou de route"),
            entry(MOTO_NIGHT_LEAN, "#209 - Voyage moto — inclinaison nocturne"),
            entry(MOTO_TAIL_GLOW, "#210 - Voyage moto — lueur arrière"),
            entry(MOTO_DAWN_RIDE, "#211 - Voyage moto — aube"),
            entry(AVION_HORIZON_CLOUDS, "#212 - Voyage avion — nuages d'horizon"),
            entry(AVION_CONTRAIL, "#213 - Voyage avion — traînée de condensation"),
            entry(AVION_WING_GLOW, "#214 - Voyage avion — lueur d'aile"),
            entry(AVION_CABIN_WINDOW, "#215 - Voyage avion — hublot"),
            entry(AVION_SUNRISE_BAND, "#216 - Voyage avion — bande de lever de soleil"),
            entry(AVION_NIGHT_CITY, "#217 - Voyage avion — ville la nuit"),
            entry(VELO_PATH_FIELDS, "#218 - Voyage vélo — chemin et champs"),
            entry(VELO_PARALLAX_TREES, "#219 - Voyage vélo — arbres en parallaxe"),
            entry(VELO_DAWN_LANE, "#220 - Voyage vélo — voie à l'aube"),
            entry(VELO_RAIN_ASPHALT, "#221 - Voyage vélo — pluie sur asphalte"),
            entry(VELO_GOLDEN_HOUR, "#222 - Voyage vélo — heure dorée"),
            entry(VELO_FOREST_CANOPY, "#223 - Voyage vélo — canopée"),
            entry(TRAIN_WINDOW_BLUR, "#224 - Voyage train — flou de fenêtre"),
            entry(TRAIN_CATENARY, "#225 - Voyage train — caténaire douce"),
            entry(TRAIN_PLATFORM_GLOW, "#226 - Voyage train — lueur de quai"),
            entry(TRAIN_COUNTRYSIDE, "#227 - Voyage train — campagne"),
            entry(TRAIN_NIGHT_CARRIAGE, "#228 - Voyage train — wagon de nuit"),
            entry(TRAIN_TUNNEL_PULSE, "#229 - Voyage train — pulsation de tunnel"),
            entry(US_DINER_NEON, "#230 - Diner US néon"),
            entry(US_MOTEL_SIGN, "#231 - Enseigne de motel US"),
            entry(US_ROUTE66_WASH, "#232 - Route 66 — lavis désert"),
            entry(US_CRT_ARCADE, "#233 - Arcade CRT US"),
            entry(US_RETROWAVE, "#234 - Horizon retrowave US"),
            entry(US_DRIVE_IN, "#235 - Lueur de drive-in US"),
            entry(STORM_STROBE_FLASH, "#236 - Orage strobo — flash"),
            entry(STORM_STROBE_SHEET, "#237 - Orage strobo — éclair en nappe"),
            entry(STORM_STROBE_PULSE, "#238 - Orage strobo — pulsation"),
            entry(STORM_STROBE_FORK, "#239 - Orage strobo — fourche"),
            entry(STORM_STROBE_HORIZON, "#240 - Orage strobo — horizon"),
            entry(STORM_STROBE_CLOUD, "#241 - Orage strobo — éclat de nuage"),
            entry(PHYSARUM_VEINS, "#242 - Veines de physarum"),
            entry(PHYSARUM_LATTICE, "#243 - Treillis de physarum"),
            entry(PHYSARUM_BLOOM, "#244 - Floraison de physarum"),
            entry(PHYSARUM_DRIFT, "#245 - Physarum dérivant"),
            entry(PHYSARUM_NODE, "#246 - Nid de nœuds physarum"),
            entry(PHYSARUM_FILAMENT, "#247 - Filament de physarum"),
            entry(REACTION_SPOTS, "#248 - Réaction-diffusion — taches"),
            entry(REACTION_CORALS, "#249 - Réaction-diffusion — coraux"),
            entry(REACTION_WAVES, "#250 - Réaction-diffusion — vagues"),
            entry(REACTION_MITOSIS, "#251 - Réaction-diffusion — mitose"),
            entry(REACTION_MAZE, "#252 - Réaction-diffusion — labyrinthe"),
            entry(REACTION_BLOBS, "#253 - Réaction — blobs doux"),
            entry(SOFT_TIDE_RIPPLES, "#254 - Rides de marée douce"),
            entry(HARBOR_MIST, "#255 - Brume de port"),
            entry(TADPOLE_NEBULA, "#256 - Nébuleuse têtard"),
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
