package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Overlapping translucent glowing bokeh discs with ambient drop shadows. */
@Composable
internal fun PrismBokehEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 12, medium = 20, high = 32)
    val discs = remember(count) {
        List(count) { i ->
            BokehDisc(
                x0 = seededUnit(i * 13 + 3),
                y0 = seededUnit(i * 23 + 7),
                speedX = seededRange(i * 37 + 11, -0.15f, 0.15f),
                speedY = seededRange(i * 47 + 13, -0.15f, 0.15f),
                radiusFrac = seededRange(i * 59 + 17, 0.08f, 0.22f),
                colorIndex = i,
                phase = seededRange(i * 71 + 19, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "prism_bokeh")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "prism_bokeh_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF030509))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        // Draw Ambient Drop Shadows
        discs.forEach { disc ->
            val currX = (((disc.x0 + disc.speedX * time) % 1f) + 1f) % 1f * w
            val currY = (((disc.y0 + disc.speedY * time) % 1f) + 1f) % 1f * h
            val pulse = sin01(time * 2f * PI.toFloat() * 2f + disc.phase)
            val r = disc.radiusFrac * minDim * (0.85f + 0.3f * pulse)

            val shadowCenter = Offset(currX + r * 0.12f, currY + r * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.4f * dim),
                        Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = r * 1.3f
                ),
                radius = r * 1.3f,
                center = shadowCenter
            )
        }

        // Draw Glowing Bokeh Discs
        discs.forEach { disc ->
            val currX = (((disc.x0 + disc.speedX * time) % 1f) + 1f) % 1f * w
            val currY = (((disc.y0 + disc.speedY * time) % 1f) + 1f) % 1f * h
            val pulse = sin01(time * 2f * PI.toFloat() * 2f + disc.phase)
            val r = disc.radiusFrac * minDim * (0.85f + 0.3f * pulse)

            val baseColor = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, disc.colorIndex),
                brightness
            )
            val center = Offset(currX, currY)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(baseColor, 0.65f * dim),
                        TonalPalette.withAlpha(baseColor, 0.35f * dim),
                        TonalPalette.withAlpha(baseColor, 0.08f * dim),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }
    }
}

private data class BokehDisc(
    val x0: Float,
    val y0: Float,
    val speedX: Float,
    val speedY: Float,
    val radiusFrac: Float,
    val colorIndex: Int,
    val phase: Float,
)
