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
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.min

/** Rotating spiral ambient gradients with central shadow core and radial glowing palettes. */
@Composable
internal fun VortexGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 6, medium = 9, high = 14)
    val arms = remember(count) {
        List(count) { i ->
            VortexArm(
                angleOffsetDeg = (360f / count) * i,
                radiusScale = seededRange(i * 23 + 11, 0.45f, 0.95f),
                colorIdx = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "vortex_glow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((35000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vortex_glow_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val maxRadius = min(w, h) * 0.7f
        drawRect(color = Color(0xFF030308))

        val time = phase01(t)
        val rotationDeg = time * 360f
        val dim = if (isActive) 1f else 0.55f

        // Draw spiral arms
        arms.forEach { arm ->
            val c = TonalPalette.brightness(TonalPalette.pick(paletteColors, arm.colorIdx), brightness)
            val armRadius = maxRadius * arm.radiusScale

            rotate(rotationDeg + arm.angleOffsetDeg, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(c, 0.45f * dim),
                            TonalPalette.withAlpha(c, 0.15f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(center.x + armRadius * 0.4f, center.y),
                        radius = armRadius * 0.6f,
                    ),
                    radius = armRadius * 0.6f,
                    center = Offset(center.x + armRadius * 0.4f, center.y),
                )
            }
        }

        // Central dark shadow core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.85f * dim),
                    Color.Black.copy(alpha = 0.45f * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = maxRadius * 0.35f,
            ),
            radius = maxRadius * 0.35f,
            center = center,
        )
    }
}

private data class VortexArm(
    val angleOffsetDeg: Float,
    val radiusScale: Float,
    val colorIdx: Int,
)
