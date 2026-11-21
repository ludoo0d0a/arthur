package fr.geoking.arthur.fractal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

/** AGSL render buffer scale — smaller on large TV panels / lower quality. */
internal fun fractalAgslRenderScale(quality: FractalQuality, minSidePx: Float): Float {
    val large = minSidePx >= 1080f
    return when (quality) {
        FractalQuality.Low -> if (large) 0.45f else 0.60f
        FractalQuality.Medium -> if (large) 0.55f else 0.72f
        FractalQuality.High -> if (large) 0.75f else 1f
    }
}

/**
 * Renders [content] into a quality-scaled buffer, then upscales to fill [modifier].
 * Matches the FractalAgslCanvas pattern so Bahlai / escape AGSL share one code path.
 */
@Composable
internal fun FractalScaledHost(
    quality: FractalQuality,
    modifier: Modifier = Modifier,
    content: @Composable (scaledModifier: Modifier) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val boxMaxWidth = maxWidth
        val boxMaxHeight = maxHeight
        val minSidePx = with(density) {
            minOf(boxMaxWidth.toPx(), boxMaxHeight.toPx())
        }
        val renderScale = fractalAgslRenderScale(quality, minSidePx)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val scaledModifier = if (renderScale < 0.999f) {
                Modifier
                    .requiredSize(
                        width = boxMaxWidth * renderScale,
                        height = boxMaxHeight * renderScale,
                    )
                    .graphicsLayer {
                        scaleX = 1f / renderScale
                        scaleY = 1f / renderScale
                        transformOrigin = TransformOrigin.Center
                    }
            } else {
                Modifier.fillMaxSize()
            }
            content(scaledModifier)
        }
    }
}
