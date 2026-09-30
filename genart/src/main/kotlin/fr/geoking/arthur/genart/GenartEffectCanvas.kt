package fr.geoking.arthur.genart

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Dispatches to a procedural Canvas engine. Pauses visual intensity when [isActive] is false.
 *
 * Live path targets a near-4K look: high contrast, screen-filling scale, and denser High quality.
 */
@Composable
fun GenartEffectCanvas(
    engine: GenartEngineId,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    paletteColors: List<Color> = TonalPalette.default(),
    quality: GenartQuality = GenartQuality.High,
) {
    val palette = paletteColors.ifEmpty { TonalPalette.default() }
    val brightness by animateFloatAsState(
        targetValue = if (isActive) 1.28f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "genart_brightness",
    )
    val speed by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.35f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "genart_speed",
    )
    val fillScale = GenartStillFx.FILL_SCALE
    val contrastWash = if (isActive) 0.10f else 0.04f
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = fillScale
                scaleY = fillScale
            }
            .drawWithContent {
                drawContent()
                // High-contrast polish: soft center lift + edge vignette (no pixelation).
                val minDim = size.minDimension
                if (minDim <= 0f) return@drawWithContent
                val cx = size.width * 0.5f
                val cy = size.height * 0.48f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = contrastWash),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = (minDim * 0.55f).positiveRadius(),
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isActive) 0.28f else 0.18f),
                        ),
                        center = Offset(cx, cy),
                        radius = (minDim * 0.78f).positiveRadius(),
                    ),
                )
            },
    ) {
        GenartRegistry.descriptorFor(engine).render(
            isActive,
            palette,
            quality,
            brightness,
            speed,
            Modifier.fillMaxSize(),
        )
    }
}
