package fr.geoking.arthur.genart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/** Seed → harmonious Compose colors shared by all genart engines. */
object TonalPalette {
    fun default(): List<Color> = fromAnimationPalette(AnimationPalettes.paletteFor(0))

    fun fromAnimationPalette(palette: AnimationPalette): List<Color> =
        palette.colorsAsComposeColor.ifEmpty { fromSeed(Color(0xFF6366F1)) }

    fun toAnimationPalette(colors: List<Color>, name: String = "Custom"): AnimationPalette =
        AnimationPalette(
            name = name,
            colors = colors.map { it.toArgb() },
        )

    fun fromSeed(seed: Color): List<Color> {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed.toArgb(), hsl)
        val hue = hsl[0]
        val sat = hsl[1].coerceIn(0.35f, 0.85f)
        val light = hsl[2].coerceIn(0.35f, 0.7f)
        return listOf(
            hslColor(hue, sat, light),
            hslColor((hue + 30f) % 360f, sat * 0.9f, (light + 0.08f).coerceAtMost(0.78f)),
            hslColor((hue + 330f) % 360f, sat * 0.95f, (light - 0.05f).coerceAtLeast(0.28f)),
            hslColor((hue + 160f) % 360f, (sat * 0.75f).coerceAtLeast(0.3f), light),
            hslColor((hue + 200f) % 360f, sat * 0.7f, (light + 0.12f).coerceAtMost(0.8f)),
        )
    }

    private fun hslColor(h: Float, s: Float, l: Float): Color {
        val argb = ColorUtils.HSLToColor(floatArrayOf(h, s, l))
        return Color(argb)
    }

    fun pick(colors: List<Color>, index: Int): Color {
        if (colors.isEmpty()) return Color(0xFF6366F1)
        return colors[index.floorMod(colors.size)]
    }

    private fun Int.floorMod(m: Int): Int {
        val r = this % m
        return if (r >= 0) r else r + m
    }

    fun mix(a: Color, b: Color, t: Float): Color {
        val u = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * u,
            green = a.green + (b.green - a.green) * u,
            blue = a.blue + (b.blue - a.blue) * u,
            alpha = a.alpha + (b.alpha - a.alpha) * u,
        )
    }

    fun withAlpha(color: Color, alpha: Float): Color =
        color.copy(alpha = alpha.coerceIn(0f, 1f))

    fun brightness(color: Color, factor: Float): Color = Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

internal fun qualityCount(quality: GenartQuality, low: Int, medium: Int, high: Int): Int =
    when (quality) {
        GenartQuality.Low -> low
        GenartQuality.Medium -> ((medium + high) * 0.55f).toInt().coerceAtLeast(medium)
        // Near-4K density: High lands denser than the authored high so scenes fill the frame.
        GenartQuality.High -> (high * 1.55f).toInt().coerceAtLeast(high + 2)
    }

internal fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

internal fun seededUnit(seed: Int): Float {
    var x = seed * 1103515245 + 12345
    x = (x ushr 16) xor x
    return ((x and 0x7fff).toFloat() / 0x7fff.toFloat()).coerceIn(0f, 1f)
}

internal fun seededRange(seed: Int, min: Float, max: Float): Float =
    min + seededUnit(seed) * (max - min)

internal fun phase01(t: Float): Float = ((t % 1f) + 1f) % 1f

internal fun sin01(t: Float): Float = ((kotlin.math.sin(t.toDouble()) + 1.0) / 2.0).toFloat()

/**
 * Android [android.graphics.RadialGradient] requires radius > 0; Compose brushes crash
 * the process with IllegalArgumentException otherwise (no ErrorLogger hook in draw).
 */
internal fun Float.positiveRadius(min: Float = 1f): Float = coerceAtLeast(min)
