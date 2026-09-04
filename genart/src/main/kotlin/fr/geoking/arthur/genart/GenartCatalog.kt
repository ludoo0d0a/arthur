package fr.geoking.arthur.genart

enum class GenartEngineId {
    Particles,
    Pseudo3D,
    SoftShadows,
    Tunnel,
    TonalGeometry,
    Sphere,
    Waves,
    Micro,
    Snow,
    Grass,
    BirdFlock,
    Mountains,
    Aurora,
    PondRipples,
    FallingLeaves,
    BreathCircles,
    FireEmbers,
    Dunes,
    Constellation,
    Clouds,
    Rain,
    Fog,
    FishSchool,
    Fireflies,
    Sunbeams,
    Meteors,
    Bubbles,
    CherryBlossoms,
    SoftRibbons,
    Nebula,
}

enum class GenartQuality { Low, Medium, High }

data class GenartCatalogEntry(
    val engine: GenartEngineId,
    val id: String,
    val title: String,
)

/** Stable ids/titles for Content Engine Source mapping. */
object GenartCatalog {
    fun entries(): List<GenartCatalogEntry> = listOf(
        GenartCatalogEntry(GenartEngineId.Particles, "genart.particles", "Drifting Particles"),
        GenartCatalogEntry(GenartEngineId.Pseudo3D, "genart.pseudo3d", "Wire Lattice"),
        GenartCatalogEntry(GenartEngineId.SoftShadows, "genart.softshadows", "Soft Shadows"),
        GenartCatalogEntry(GenartEngineId.Tunnel, "genart.tunnel", "Vanishing Tunnel"),
        GenartCatalogEntry(GenartEngineId.TonalGeometry, "genart.tonalgeometry", "Tonal Geometry"),
        GenartCatalogEntry(GenartEngineId.Sphere, "genart.sphere", "Orbiting Sphere"),
        GenartCatalogEntry(GenartEngineId.Waves, "genart.waves", "Layered Waves"),
        GenartCatalogEntry(GenartEngineId.Micro, "genart.micro", "Volumetric Rays"),
        GenartCatalogEntry(GenartEngineId.Snow, "genart.snow", "Falling Snow"),
        GenartCatalogEntry(GenartEngineId.Grass, "genart.grass", "Grass in Wind"),
        GenartCatalogEntry(GenartEngineId.BirdFlock, "genart.birdflock", "Bird Flock"),
        GenartCatalogEntry(GenartEngineId.Mountains, "genart.mountains", "Layered Mountains"),
        GenartCatalogEntry(GenartEngineId.Aurora, "genart.aurora", "Aurora Ribbons"),
        GenartCatalogEntry(GenartEngineId.PondRipples, "genart.pondripples", "Pond Ripples"),
        GenartCatalogEntry(GenartEngineId.FallingLeaves, "genart.fallingleaves", "Falling Leaves"),
        GenartCatalogEntry(GenartEngineId.BreathCircles, "genart.breathcircles", "Breath Circles"),
        GenartCatalogEntry(GenartEngineId.FireEmbers, "genart.fireembers", "Fireplace Embers"),
        GenartCatalogEntry(GenartEngineId.Dunes, "genart.dunes", "Wind-Blown Dunes"),
        GenartCatalogEntry(GenartEngineId.Constellation, "genart.constellation", "Constellation Twinkle"),
        GenartCatalogEntry(GenartEngineId.Clouds, "genart.clouds", "Drifting Clouds"),
        GenartCatalogEntry(GenartEngineId.Rain, "genart.rain", "Soft Rain"),
        GenartCatalogEntry(GenartEngineId.Fog, "genart.fog", "Soft Fog"),
        GenartCatalogEntry(GenartEngineId.FishSchool, "genart.fishschool", "School of Fish"),
        GenartCatalogEntry(GenartEngineId.Fireflies, "genart.fireflies", "Fireflies"),
        GenartCatalogEntry(GenartEngineId.Sunbeams, "genart.sunbeams", "Sunbeams Through Haze"),
        GenartCatalogEntry(GenartEngineId.Meteors, "genart.meteors", "Sparse Meteors"),
        GenartCatalogEntry(GenartEngineId.Bubbles, "genart.bubbles", "Rising Bubbles"),
        GenartCatalogEntry(GenartEngineId.CherryBlossoms, "genart.cherryblossoms", "Cherry Blossom Petals"),
        GenartCatalogEntry(GenartEngineId.SoftRibbons, "genart.ribbons", "Soft Ribbons"),
        GenartCatalogEntry(GenartEngineId.Nebula, "genart.nebula", "Nebula Drift"),
    )

    fun engineForId(id: String): GenartEngineId? =
        entries().firstOrNull { it.id == id }?.engine
}
