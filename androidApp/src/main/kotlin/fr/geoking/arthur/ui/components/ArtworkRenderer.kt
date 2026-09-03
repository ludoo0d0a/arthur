package fr.geoking.arthur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.fractal.FractalEffectCanvas
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.fractal.FractalType
import fr.geoking.arthur.genart.GenartCatalog
import fr.geoking.arthur.genart.GenartEffectCanvas
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.FractalSource

/**
 * Renders Artwork for Control Plane hero and phone Ambient.
 * Generative kinds use live Canvas engines; stills use a tonal placeholder.
 */
@Composable
fun ArtworkRenderer(
    artwork: Artwork,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: GenartQuality = GenartQuality.Medium,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (artwork.kind) {
            ArtworkKind.Genart -> {
                val engine = GenartCatalog.engineForId(artwork.id)
                if (engine != null) {
                    GenartEffectCanvas(
                        engine = engine,
                        isActive = isActive,
                        quality = quality,
                    )
                } else {
                    StillArtworkPlaceholder(kind = artwork.kind)
                }
            }
            ArtworkKind.FractalPreset, ArtworkKind.CustomFractal -> {
                FractalEffectCanvas(
                    isActive = isActive,
                    quality = quality.toFractalQuality(),
                    forceType = fractalTypeForArtworkId(artwork.id),
                )
            }
            else -> StillArtworkPlaceholder(kind = artwork.kind)
        }
    }
}

/** Gradient field with the artwork-kind glyph — used for bundled / museum stills. */
@Composable
private fun StillArtworkPlaceholder(
    kind: ArtworkKind,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val visual = kind.visual()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(scheme.surfaceVariant, scheme.background),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(visual.iconRes),
            contentDescription = null,
            tint = visual.onContainer.copy(alpha = 0.42f),
            modifier = Modifier.size(88.dp),
        )
    }
}

internal fun fractalTypeForArtworkId(artworkId: String): FractalType? =
    when (FractalSource.typeKey(artworkId)?.lowercase()) {
        "mandelbrot" -> FractalType.Mandelbrot
        "julia" -> FractalType.Julia
        "burningship" -> FractalType.BurningShip
        "tricorn" -> FractalType.Tricorn
        else -> null
    }

internal fun GenartQuality.toFractalQuality(): FractalQuality = when (this) {
    GenartQuality.Low -> FractalQuality.Low
    GenartQuality.Medium -> FractalQuality.Medium
    GenartQuality.High -> FractalQuality.High
}
