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
    )

    fun engineForId(id: String): GenartEngineId? =
        entries().firstOrNull { it.id == id }?.engine
}
