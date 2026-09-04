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

        fun defaultCatalog(): List<Artwork> = listOf(
            Artwork(
                id = MANDELBROT,
                title = "Mandelbrot",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = JULIA,
                title = "Julia",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = BURNING_SHIP,
                title = "Burning Ship",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = TRICORN,
                title = "Tricorn",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = MULTIBROT,
                title = "Multibrot",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = CELTIC,
                title = "Celtic",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = BUFFALO,
                title = "Buffalo",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
            Artwork(
                id = PHOENIX,
                title = "Phoenix",
                attribution = "Arthur Fractal Presets",
                sourceId = ID,
                kind = ArtworkKind.FractalPreset,
            ),
        )

        /** Returns the preset type name (`mandelbrot`, …) or null if not a fractal id. */
        fun typeKey(artworkId: String): String? =
            artworkId.removePrefix("fractal.").takeIf { artworkId.startsWith("fractal.") && it.isNotEmpty() }
    }
}
