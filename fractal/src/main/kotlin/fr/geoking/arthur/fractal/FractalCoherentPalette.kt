package fr.geoking.arthur.fractal

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Visually coherent fractal themes — narrow hue families (blues, amber, teal triads).
 * Avoids rainbow / flashy full-spectrum cycling. Same spirit as Mandelbrot Glow / Julia Touch.
 */
object FractalCoherentPalette {

    data class Theme(
        val name: String,
        /** 4 escape-time stops (dark → mid → bright → accent), MandelbrotGlow-style. */
        val stops: List<Color>,
    )

    val themes: List<Theme> = listOf(
        Theme(
            "Deep Ocean",
            listOf(Color(0xFF000000), Color(0xFF0B3A6E), Color(0xFF1E90C8), Color(0xFFB8E8FF)),
        ),
        Theme(
            "Mandelbrot Glow",
            listOf(Color(0xFF000000), Color(0xFF206BCB), Color(0xFFEDFFFF), Color(0xFFFFB000)),
        ),
        Theme(
            "Amber Ember",
            listOf(Color(0xFF0A0602), Color(0xFF8B4510), Color(0xFFE8A020), Color(0xFFFFF0C8)),
        ),
        Theme(
            "Golden Dusk",
            listOf(Color(0xFF120A04), Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFFEF3C7)),
        ),
        Theme(
            "Teal Mist",
            listOf(Color(0xFF020A0C), Color(0xFF0E7490), Color(0xFF22D3EE), Color(0xFFECFEFF)),
        ),
        Theme(
            "Indigo Night",
            listOf(Color(0xFF05040F), Color(0xFF312E81), Color(0xFF6366F1), Color(0xFFC7D2FE)),
        ),
        Theme(
            "Soft Cyan",
            listOf(Color(0xFF000000), Color(0xFF206BCB), Color(0xFF00BCD4), Color(0xFFE0F7FA)),
        ),
        Theme(
            "Rose Dusk",
            listOf(Color(0xFF0A0408), Color(0xFF7C2D4A), Color(0xFFE879A8), Color(0xFFFFE4EE)),
        ),
        Theme(
            "Forest Depth",
            listOf(Color(0xFF030806), Color(0xFF14532D), Color(0xFF34D399), Color(0xFFD1FAE5)),
        ),
        Theme(
            "Copper Glow",
            listOf(Color(0xFF0C0704), Color(0xFF9A3412), Color(0xFFFB923C), Color(0xFFFFF7ED)),
        ),
        Theme(
            "Violet Mist",
            listOf(Color(0xFF08040F), Color(0xFF5B21B6), Color(0xFFA78BFA), Color(0xFFEDE9FE)),
        ),
        Theme(
            "Ice Blue",
            listOf(Color(0xFF02040A), Color(0xFF1E3A5F), Color(0xFF7DD3FC), Color(0xFFF0F9FF)),
        ),
    )

    fun themeFor(seed: Int): Theme {
        val idx = ((seed % themes.size) + themes.size) % themes.size
        return themes[idx]
    }

    /** Four AGSL color stops for Mandelbrot Glow / Julia Touch style shaders. */
    fun fourStops(seed: Int): List<Color> = themeFor(seed).stops

    /**
     * Expanded escape palette for CPU / still bakers — stays inside the theme family
     * (no rainbow wrap). Slight seed jitter on mid stops for variety.
     */
    fun escapeStops(seed: Int, count: Int = 12): List<Color> {
        val base = fourStops(seed)
        if (count <= 4) return base.take(count)
        val rnd = Random(seed.toLong() xor 0xC0FFEEL)
        val out = mutableListOf<Color>()
        for (i in 0 until count) {
            val t = i / (count - 1).coerceAtLeast(1).toFloat()
            val segment = t * 3f
            val i0 = segment.toInt().coerceIn(0, 2)
            val frac = segment - i0
            val c0 = base[i0]
            val c1 = base[i0 + 1]
            val jitter = (rnd.nextFloat() - 0.5f) * 0.04f
            out += Color(
                red = (c0.red + (c1.red - c0.red) * frac + jitter).coerceIn(0f, 1f),
                green = (c0.green + (c1.green - c0.green) * frac + jitter * 0.6f).coerceIn(0f, 1f),
                blue = (c0.blue + (c1.blue - c0.blue) * frac - jitter * 0.4f).coerceIn(0f, 1f),
                alpha = 1f,
            )
        }
        return out
    }

    fun escapeArgb(seed: Int, count: Int = 8): List<Int> =
        escapeStops(seed, count).map { c ->
            val r = (c.red * 255f).toInt().coerceIn(0, 255)
            val g = (c.green * 255f).toInt().coerceIn(0, 255)
            val b = (c.blue * 255f).toInt().coerceIn(0, 255)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

    /** Sample along the 4-stop theme (still baker / AA). */
    fun sampleArgb(seed: Int, t: Float): Int {
        val stops = escapeArgb(seed, 4)
        val u = ((t % 1f) + 1f) % 1f
        val scaled = u * (stops.size - 1)
        val i0 = scaled.toInt().coerceIn(0, stops.size - 2)
        val frac = scaled - i0
        val a = stops[i0]
        val b = stops[i0 + 1]
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val r = (ar + (br - ar) * frac).toInt().coerceIn(0, 255)
        val g = (ag + (bg - ag) * frac).toInt().coerceIn(0, 255)
        val bl = (ab + (bb - ab) * frac).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}
