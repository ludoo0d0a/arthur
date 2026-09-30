package fr.geoking.arthur.genart

import androidx.compose.ui.graphics.Color

/**
 * Named ARGB palettes shared by Compose engines and android.graphics still bakers.
 *
 * Themes stay visually coherent: blues, amber/orange, soft triads — never rainbow flash.
 * [fromGeneration] / [paletteFor] pick a random theme per bake / artwork.
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
            name = "Ocean",
            colors = listOf(
                0xFF0EA5E9.toInt(),
                0xFF38BDF8.toInt(),
                0xFF06B6D4.toInt(),
                0xFF14B8A6.toInt(),
                0xFF7DD3FC.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Deep Blue",
            colors = listOf(
                0xFF1E3A8A.toInt(),
                0xFF2563EB.toInt(),
                0xFF3B82F6.toInt(),
                0xFF60A5FA.toInt(),
                0xFF93C5FD.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Ice",
            colors = listOf(
                0xFF0C4A6E.toInt(),
                0xFF0284C7.toInt(),
                0xFF38BDF8.toInt(),
                0xFFBAE6FD.toInt(),
                0xFFE0F2FE.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Teal Mist",
            colors = listOf(
                0xFF115E59.toInt(),
                0xFF0D9488.toInt(),
                0xFF2DD4BF.toInt(),
                0xFF99F6E4.toInt(),
                0xFFCCFBF1.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Amber Ember",
            colors = listOf(
                0xFF9A3412.toInt(),
                0xFFEA580C.toInt(),
                0xFFF59E0B.toInt(),
                0xFFFBBF24.toInt(),
                0xFFFEF3C7.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Golden Dusk",
            colors = listOf(
                0xFFB45309.toInt(),
                0xFFD97706.toInt(),
                0xFFFBBF24.toInt(),
                0xFFFDE68A.toInt(),
                0xFFFFFBEB.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Copper Glow",
            colors = listOf(
                0xFF7C2D12.toInt(),
                0xFFC2410C.toInt(),
                0xFFFB923C.toInt(),
                0xFFFDBA74.toInt(),
                0xFFFFEDD5.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Forest",
            colors = listOf(
                0xFF166534.toInt(),
                0xFF16A34A.toInt(),
                0xFF22C55E.toInt(),
                0xFF86EFAC.toInt(),
                0xFFDCFCE7.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Indigo Night",
            colors = listOf(
                0xFF312E81.toInt(),
                0xFF4338CA.toInt(),
                0xFF6366F1.toInt(),
                0xFFA5B4FC.toInt(),
                0xFFE0E7FF.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Soft Violet",
            colors = listOf(
                0xFF5B21B6.toInt(),
                0xFF7C3AED.toInt(),
                0xFFA78BFA.toInt(),
                0xFFC4B5FD.toInt(),
                0xFFEDE9FE.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Rose Dusk",
            colors = listOf(
                0xFF9F1239.toInt(),
                0xFFBE123C.toInt(),
                0xFFFB7185.toInt(),
                0xFFFDA4AF.toInt(),
                0xFFFFE4E6.toInt(),
            ),
        ),
        AnimationPalette(
            name = "Slate",
            colors = listOf(
                0xFF334155.toInt(),
                0xFF475569.toInt(),
                0xFF64748B.toInt(),
                0xFF94A3B8.toInt(),
                0xFFE2E8F0.toInt(),
            ),
        ),
        // Legacy Micro index kept for callers that pin the last palette.
        AnimationPalette(
            name = "Micro",
            colors = listOf(
                0xFF4C1D95.toInt(),
                0xFF6D28D9.toInt(),
                0xFF8B5CF6.toInt(),
                0xFFC4B5FD.toInt(),
                0xFFEDE9FE.toInt(),
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
