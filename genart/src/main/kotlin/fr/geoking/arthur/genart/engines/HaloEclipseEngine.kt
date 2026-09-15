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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.min

/** Concentric shifting color rings with deep inner and outer soft shadow masking and color pulses. */
@Composable
internal fun HaloEclipseEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 6, high = 9)
    val rings = remember(count) {
        List(count) { i ->
            HaloRing(
                radiusFrac = 0.15f + 0.08f * i,
                strokeFrac = seededRange(i * 17 + 5, 0.02f, 0.05f),
                phaseOffset = seededRange(i * 29 + 11, 0f, 2f * PI.toFloat()),
                colorIdx = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "halo_eclipse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "halo_eclipse_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val minDim = min(w, h)
        drawRect(color = Color(0xFF020205))

        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        // Draw rings from outside in
        rings.reversed().forEach { ring ->
            val pulse = sin01(time * 2f * PI.toFloat() + ring.phaseOffset)
            val radius = (ring.radiusFrac + pulse * 0.04f) * minDim
            val strokeWidth = ring.strokeFrac * minDim
            val c = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIdx), brightness)

            // Outer soft shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f * dim),
                radius = radius + strokeWidth * 1.2f,
                center = center,
                style = Stroke(width = strokeWidth * 1.5f),
            )

            // Halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(c, 0.65f * dim),
                        TonalPalette.withAlpha(c, 0.2f * dim),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius + strokeWidth * 2f,
                ),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }

        // Deep central eclipse shadow disk
        val coreRadius = 0.12f * minDim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black,
                    Color.Black.copy(alpha = 0.85f * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = coreRadius * 1.5f,
            ),
            radius = coreRadius * 1.5f,
            center = center,
        )
    }
}

private data class HaloRing(
    val radiusFrac: Float,
    val strokeFrac: Float,
    val phaseOffset: Float,
    val colorIdx: Int,
)
