package fr.geoking.arthur.fractal

/**
 * Fractal Presets (free tier) — Julius-style types, AGSL originals, and
 * [tbahlai/agsl](https://github.com/tbahlai/agsl) Mandelbrot/Julia ports.
 * Custom Fractal (tap points) is Premium — see [CustomFractalParams].
 */
enum class FractalPresetType {
    Mandelbrot,
    Julia,
    BurningShip,
    Tricorn,
    Multibrot,
    Celtic,
    Buffalo,
    Phoenix,
    Nova,
    Newton,
    MandelbrotGlow,
    JuliaTouch,
}

data class FractalPreset(
    val type: FractalPresetType,
    val quality: Int = 1,
)

object FractalCatalog {
    fun freePresets(): List<FractalPreset> =
        FractalPresetType.entries.map { FractalPreset(type = it) }

    fun isCustomAllowed(isPremium: Boolean): Boolean = isPremium
}
