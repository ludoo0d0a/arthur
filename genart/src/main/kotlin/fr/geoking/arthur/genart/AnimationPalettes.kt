package fr.geoking.arthur.genart

import androidx.compose.ui.graphics.Color

/**
 * Named ARGB palettes shared by Compose engines and android.graphics still bakers.
 * Ported from Julius AnimationPalettes (copy-extract, no Julius dependency).
 */
data class AnimationPalette(
    val name: String,
    val colors: List<Int>,
) {
    /** Cached Compose Color list to avoid repeated mapping in engines. */
    val colorsAsComposeColor: List<Color> by lazy { colors.map { Color(it) } }

    val primary: Int get() = colorAt(0)
    val secondary: Int get() = colorAt(1)
    val tertiary: Int get() = colorAt(2)
    val quaternary: Int get() = colorAt(3)
    val quinary: Int get() = colorAt(4)

    fun colorAt(index: Int): Int {
        if (colors.isEmpty()) return 0
        val safeIndex = ((index % colors.size) + colors.size) % colors.size
        return colors[safeIndex]
    }
}

object AnimationPalettes {
    private val palettes = listOf(
        AnimationPalette(
            name = "Aurora",
            colors = listOf(
                0xFF6366F1.toInt(),
                0xFF8B5CF6.toInt(),
                0xFFEC4899.toInt(),
                0xFF06B6D4.toInt(),
                0xFF10B981.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Sunset",
            colors = listOf(
                0xFFF97316.toInt(),
                0xFFF59E0B.toInt(),
                0xFFFB7185.toInt(),
                0xFFEF4444.toInt(),
                0xFFA855F7.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Ocean",
            colors = listOf(
                0xFF0EA5E9.toInt(),
                0xFF38BDF8.toInt(),
                0xFF06B6D4.toInt(),
                0xFF14B8A6.toInt(),
                0xFF22D3EE.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Forest",
            colors = listOf(
                0xFF22C55E.toInt(),
                0xFF16A34A.toInt(),
                0xFF10B981.toInt(),
                0xFF84CC16.toInt(),
                0xFF4ADE80.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Ember",
            colors = listOf(
                0xFFEF4444.toInt(),
                0xFFF97316.toInt(),
                0xFFFBBF24.toInt(),
                0xFFDC2626.toInt(),
                0xFFFB7185.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Cosmic",
            colors = listOf(
                0xFF7C3AED.toInt(),
                0xFF6366F1.toInt(),
                0xFF0EA5E9.toInt(),
                0xFFF472B6.toInt(),
                0xFFA855F7.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Micro",
            colors = listOf(
                0xFFA732FF.toInt(),
                0xFFB388FF.toInt(),
                0xFF7C4DFF.toInt(),
                0xFFD1C4E9.toInt(),
                0xFF9575CD.toInt(),
            ),
        ),
    )

    val size: Int
        get() = palettes.size

    /** Index of the Micro palette (last in list). */
    val microPaletteIndex: Int
        get() = (palettes.size - 1).coerceAtLeast(0)

    fun paletteFor(index: Int): AnimationPalette {
        if (palettes.isEmpty()) return AnimationPalette("Default", emptyList())
        val safeIndex = ((index % palettes.size) + palettes.size) % palettes.size
        return palettes[safeIndex]
    }

    fun fromGeneration(generation: Long): AnimationPalette =
        paletteFor(generation.toInt())
}
