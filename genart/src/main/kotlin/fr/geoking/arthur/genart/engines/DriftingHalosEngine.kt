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
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Multiple soft halos covering the screen, drifting slowly in all directions with
 * varying diameters, stroke thickness, light intensity, and colors.
 */
@Composable
internal fun DriftingHalosEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 6, medium = 10, high = 15)
    val halos = remember(count) {
        List(count) { i ->
            DriftingHaloNode(
                xCenterStart = seededRange(i * 13 + 3, -0.2f, 1.2f),
                yCenterStart = seededRange(i * 17 + 5, -0.2f, 1.2f),
                xCenterEnd = seededRange(i * 23 + 7, -0.2f, 1.2f),
                yCenterEnd = seededRange(i * 29 + 11, -0.2f, 1.2f),
                radiusFrac = seededRange(i * 31 + 13, 0.15f, 0.55f),
                ringThicknessFrac = seededRange(i * 37 + 17, 0.015f, 0.08f),
                phaseOffset = seededRange(i * 41 + 19, 0f, 2f * PI.toFloat()),
                speedMult = seededRange(i * 43 + 23, 0.5f, 1.5f),
                baseIntensity = seededRange(i * 47 + 29, 0.35f, 0.8f),
                colorIdx = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "drifting_halos")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drifting_halos_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        drawRect(color = Color(0xFF030308))

        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        halos.forEach { halo ->
            val localTime = (time * halo.speedMult + halo.phaseOffset / (2f * PI.toFloat())) % 1f
            val motionPhase = sin01(localTime * 2f * PI.toFloat())
            val intensityPulse = sin01(localTime * 4f * PI.toFloat() + halo.phaseOffset)

            val cx = lerp(halo.xCenterStart, halo.xCenterEnd, motionPhase) * w
            val cy = lerp(halo.yCenterStart, halo.yCenterEnd, 1f - motionPhase) * h
            val center = Offset(cx, cy)

            val radius = halo.radiusFrac * minDim * (0.88f + 0.24f * intensityPulse)
            val strokeWidth = halo.ringThicknessFrac * minDim * (0.9f + 0.2f * motionPhase)
            val intensity = halo.baseIntensity * (0.65f + 0.35f * intensityPulse) * dim

            val color = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, halo.colorIdx),
                brightness,
            )

            // Outer soft glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(color, intensity * 0.45f),
                        TonalPalette.withAlpha(color, intensity * 0.15f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius + strokeWidth * 2.5f,
                ),
                radius = radius + strokeWidth * 2.5f,
                center = center,
            )

            // Ring halo core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(color, intensity * 0.85f),
                        TonalPalette.withAlpha(color, intensity * 0.3f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius + strokeWidth,
                ),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private data class DriftingHaloNode(
    val xCenterStart: Float,
    val yCenterStart: Float,
    val xCenterEnd: Float,
    val yCenterEnd: Float,
    val radiusFrac: Float,
    val ringThicknessFrac: Float,
    val phaseOffset: Float,
    val speedMult: Float,
    val baseIntensity: Float,
    val colorIdx: Int,
)
