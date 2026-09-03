package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.FastOutSlowInEasing
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
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

private data class BreathGroup(
    val offsetXFrac: Float,
    val offsetYFrac: Float,
    val baseRadiusFrac: Float,
    val phaseOffset: Float,
    val driftFreq: Float,
    val colorIndex: Int,
)

/** The calmest engine of the set: slow, layered breathing circles on a near-black canvas. */
@Composable
internal fun BreathCirclesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 1, medium = 2, high = 3)
    val groups = remember(count) {
        List(count) { i ->
            BreathGroup(
                offsetXFrac = seededRange(i * 13 + 5, -0.10f, 0.10f),
                offsetYFrac = seededRange(i * 19 + 7, -0.08f, 0.08f),
                baseRadiusFrac = seededRange(i * 23 + 11, 0.16f, 0.30f),
                phaseOffset = seededRange(i * 31 + 19, 0f, 2f * PI.toFloat()),
                driftFreq = seededRange(i * 37 + 23, 0.15f, 0.25f),
                colorIndex = i,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breath_circles_loop")
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween((1_000_000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )

    val restingScale = if (isActive) 1f else 0.4f
    val restingAlpha = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val minDim = size.width.coerceAtMost(size.height)

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0B1220), Color(0xFF05070D), Color(0xFF000000)),
                center = Offset(centerX, centerY),
                radius = size.width.coerceAtLeast(size.height) * 0.75f,
            ),
        )

        val signed = (breathAnim - 0.5f) * 2f

        groups.forEach { group ->
            val desync = sin(driftAnim * group.driftFreq + group.phaseOffset)
            val breathFactor = signed * 0.8f + desync * 0.2f
            val amplitude = 0.3f * restingScale
            val radiusFactor = 1f + amplitude * breathFactor
            val baseRadius = minDim * group.baseRadiusFrac
            val radius = (baseRadius * radiusFactor).coerceAtLeast(1f)
            val center = Offset(
                centerX + group.offsetXFrac * size.width,
                centerY + group.offsetYFrac * size.height,
            )
            val color = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, group.colorIndex),
                brightness,
            )
            val coreAlpha = (0.22f + 0.10f * (breathFactor * 0.5f + 0.5f)) * restingAlpha
            val haloAlpha = coreAlpha * 0.45f
            val haloRadius = radius * 1.6f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = haloAlpha), Color.Transparent),
                    center = center,
                    radius = haloRadius,
                ),
                radius = haloRadius,
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = coreAlpha), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}
