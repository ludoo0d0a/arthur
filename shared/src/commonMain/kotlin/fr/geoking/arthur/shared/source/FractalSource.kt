package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source

/**
 * Fractal presets as Content Engine Artwork.
 * Ids map 1:1 to `:fractal` FractalPresetType / FractalType names (lowercase).
 */
class FractalSource(
    private val items: List<Artwork> = defaultCatalog(),
) : Source {
    override val id: String = ID
    override val displayName: String = "Fractal Presets"

    override suspend fun load(): List<Artwork> = items

    companion object {
        const val ID = "fractal"

        const val MANDELBROT = "fractal.mandelbrot"
        const val JULIA = "fractal.julia"
        const val BURNING_SHIP = "fractal.burningship"
        const val TRICORN = "fractal.tricorn"
        const val MULTIBROT = "fractal.multibrot"
        const val CELTIC = "fractal.celtic"
        const val BUFFALO = "fractal.buffalo"
        const val PHOENIX = "fractal.phoenix"
        const val NOVA = "fractal.nova"
        const val NEWTON = "fractal.newton"
        const val MANDELBROT_GLOW = "fractal.mandelbrotglow"
        const val JULIA_TOUCH = "fractal.juliatouch"

        fun defaultCatalog(): List<Artwork> = listOf(
            Artwork(
                id = MANDELBROT,
                title = "#96 - Mandelbrot",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = JULIA,
                title = "#97 - Julia",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = BURNING_SHIP,
                title = "#98 - Burning Ship",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = TRICORN,
                title = "#99 - Tricorn",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = MULTIBROT,
                title = "#100 - Multibrot",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = CELTIC,
                title = "#101 - Celtic",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = BUFFALO,
                title = "#102 - Buffalo",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = PHOENIX,
                title = "#103 - Phoenix",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = NOVA,
                title = "#104 - Nova",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = NEWTON,
                title = "#105 - Newton",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = MANDELBROT_GLOW,
                title = "#106 - Mandelbrot Glow",
                attribution = "AGSL · tbahlai/agsl",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = JULIA_TOUCH,
                title = "#107 - Julia Touch",
                attribution = "AGSL · tbahlai/agsl",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
        )

        /** Returns the preset type name (`mandelbrot`, …) or null if not a fractal id. */
        fun typeKey(artworkId: String): String? =
            artworkId.removePrefix("fractal.").takeIf { artworkId.startsWith("fractal.") && it.isNotEmpty() }
    }
}
