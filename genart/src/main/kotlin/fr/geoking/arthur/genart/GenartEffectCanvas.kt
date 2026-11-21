package fr.geoking.arthur.genart

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/**
 * Dispatches to a procedural Canvas engine. Pauses visual intensity when [isActive] is false.
 *
 * Live path targets a near-4K look at High; Medium/Low render into a smaller buffer and
 * upscale so blur, particles, and AGSL stay fluid on TV-sized panels.
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
    val contrastWash = when {
        !isActive -> 0.04f
        quality == GenartQuality.High -> 0.10f
        else -> 0.06f
    }
    val vignetteAlpha = when {
        !isActive -> 0.18f
        quality == GenartQuality.High -> 0.28f
        else -> 0.20f
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val boxMaxWidth = maxWidth
        val boxMaxHeight = maxHeight
        val minSidePx = with(density) {
            minOf(boxMaxWidth.toPx(), boxMaxHeight.toPx())
        }
        val renderScale = GenartPerf.liveRenderScale(quality, minSidePx)
        val contentModifier = if (renderScale < 0.999f) {
            Modifier
                .requiredSize(
                    width = boxMaxWidth * renderScale,
                    height = boxMaxHeight * renderScale,
                )
                .graphicsLayer {
                    val up = fillScale / renderScale
                    scaleX = up
                    scaleY = up
                    transformOrigin = TransformOrigin.Center
                }
        } else {
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = fillScale
                    scaleY = fillScale
                }
        }

        Box(
            modifier = contentModifier.drawWithContent {
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
                            Color.Black.copy(alpha = vignetteAlpha),
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
}
