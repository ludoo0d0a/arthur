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
    )

    fun engineForId(id: String): GenartEngineId? =
        entries().firstOrNull { it.id == id }?.engine
}
