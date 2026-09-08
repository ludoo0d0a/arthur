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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit

/** Condensation beads growing and dripping down terrarium glass — calm, car-safe loop. */
@Composable
internal fun TerrariumDripEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 1, medium = 2, high = 3)
    val drops = remember(count) {
        List(count) { i ->
            DropSeed(
                x0 = seededRange(i * 17 + 3, 0.18f, 0.82f),
                y0 = seededRange(i * 29 + 7, 0.15f, 0.75f),
                phaseOffset = seededUnit(i * 41 + 11),
                cycleSeconds = seededRange(i * 53 + 13, 9f, 16f),
                maxRadiusFrac = seededRange(i * 67 + 19, 0.014f, 0.026f),
                dripFrac = seededRange(i * 79 + 23, 0.06f, 0.11f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "terrarium_drip")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "terrarium_drip_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF16261C), Color(0xFF0A140E)),
            ),
        )
        val time = phase01(t) * 20f
        val dim = if (isActive) 1f else 0.55f
        drops.forEach { drop ->
            val cycle = phase01(drop.phaseOffset + time / drop.cycleSeconds)
            val growEnd = 0.78f
            val minDim = w.coerceAtMost(h)
            val maxRadius = drop.maxRadiusFrac * minDim
            val baseX = drop.x0 * w
            val baseY = drop.y0 * h
            val radius: Float
            val yOffset: Float
            val stretch: Float
            val alphaShape: Float
            if (cycle < growEnd) {
                val growT = cycle / growEnd
                radius = (0.08f + 0.92f * growT) * maxRadius
                yOffset = 0f
                stretch = 1f
                alphaShape = growT.coerceIn(0.15f, 1f)
            } else {
                val dripT = (cycle - growEnd) / (1f - growEnd)
                radius = maxRadius * (1f - 0.5f * dripT)
                yOffset = dripT * drop.dripFrac * h
                stretch = 1f + 0.6f * dripT
                alphaShape = 1f - dripT
            }
            val alpha = alphaShape * dim
            val cx = baseX
            val cy = baseY + yOffset
            val base = TonalPalette.mix(Color(0xFFBFE6C6), TonalPalette.pick(paletteColors, drop.colorIndex), 0.3f)
            val bodyColor = TonalPalette.brightness(base, brightness * 0.85f)
            val rimColor = TonalPalette.brightness(TonalPalette.mix(base, Color(0xFF0A1A0F), 0.55f), brightness)
            val rh = radius * stretch
            drawOval(
                color = TonalPalette.withAlpha(rimColor, alpha * 0.6f),
                topLeft = Offset(cx - radius, cy - rh),
                size = Size(radius * 2f, rh * 2f),
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(bodyColor, alpha * 0.85f),
                        TonalPalette.withAlpha(bodyColor, alpha * 0.15f),
                    ),
                    center = Offset(cx, cy),
                    radius = (radius * 0.92f).coerceAtLeast(1f),
                ),
                topLeft = Offset(cx - radius * 0.92f, cy - rh * 0.92f),
                size = Size(radius * 1.84f, rh * 1.84f),
            )
            val highlightRadius = radius * 0.32f
            val highlightOffset = radius * 0.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color(0xFFF4FFF6), alpha * 0.9f),
                        Color.Transparent,
                    ),
                    center = Offset(cx - highlightOffset, cy - rh * 0.4f),
                    radius = highlightRadius.coerceAtLeast(1f),
                ),
                radius = highlightRadius.coerceAtLeast(1f),
                center = Offset(cx - highlightOffset, cy - rh * 0.4f),
            )
        }
    }
}

private data class DropSeed(
    val x0: Float,
    val y0: Float,
    val phaseOffset: Float,
    val cycleSeconds: Float,
    val maxRadiusFrac: Float,
    val dripFrac: Float,
    val colorIndex: Int,
)
