package fr.geoking.arthur.fractal

/**
 * Fractal Presets (free tier) — Julius-style types.
 * Custom Fractal (tap points) is Premium — see [CustomFractalParams].
 */
enum class FractalPresetType {
    Mandelbrot,
    Julia,
    BurningShip,
    Tricorn,
}

data class FractalPreset(
    val type: FractalPresetType,
    val quality: Int = 1,
)

/** Realme-style tap-authored fractal — Premium only. */
data class CustomFractalParams(
    val points: List<Pair<Float, Float>>,
    val colorSeed: Int = 0,
)

object FractalCatalog {
    fun freePresets(): List<FractalPreset> =
        FractalPresetType.entries.map { FractalPreset(type = it) }

    fun isCustomAllowed(isPremium: Boolean): Boolean = isPremium
}
