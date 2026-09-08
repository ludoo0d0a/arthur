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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit

/** Thin translucent wash-lines lapping up a pebbled shore, one-directional and staggered. */
@Composable
internal fun PebbleShoreWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val washCount = qualityCount(quality, low = 2, medium = 3, high = 3)
    val pebbleCount = qualityCount(quality, low = 12, medium = 20, high = 30)

    val washes = remember(washCount) {
        List(washCount) { i ->
            WashSeed(
                phaseOffset = seededUnit(i * 53 + 5),
                cyclesPerLoop = seededRange(i * 67 + 9, 0.55f, 0.85f),
                maxHeightFrac = seededRange(i * 83 + 13, 0.34f, 0.58f),
                baseAlpha = seededRange(i * 101 + 17, 0.16f, 0.3f),
                centerFrac = seededRange(i * 113 + 19, 0.32f, 0.68f),
                spanFrac = seededRange(i * 127 + 23, 0.78f, 1.05f),
                curveFrac = seededRange(i * 139 + 29, -0.06f, 0.06f),
                colorIndex = i,
            )
        }
    }
    val pebbles = remember(pebbleCount) {
        List(pebbleCount) { i ->
            PebbleSeed(
                xFrac = seededRange(i * 151 + 31, 0.02f, 0.98f),
                yFrac = seededRange(i * 163 + 37, 0.78f, 0.99f),
                radiusFrac = seededRange(i * 179 + 41, 0.008f, 0.022f),
                squash = seededRange(i * 191 + 43, 0.5f, 0.85f),
                warmth = seededUnit(i * 199 + 47),
                shade = seededRange(i * 211 + 53, 0.25f, 0.55f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "pebble_shore_wash")
    val effectiveSpeed = (if (isActive) speed else speed * 0.4f).coerceAtLeast(0.05f)
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / effectiveSpeed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pebble_shore_wash_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF070B0E), Color(0xFF14181C)),
            ),
        )

        val dimming = if (isActive) 1f else 0.55f

        pebbles.forEach { pebble ->
            val cx = pebble.xFrac * w
            val cy = pebble.yFrac * h
            val rx = (pebble.radiusFrac * w).coerceAtLeast(1.2f)
            val ry = rx * pebble.squash
            val grey = TonalPalette.mix(Color(0xFF3A3630), Color(0xFF8A8378), pebble.warmth)
            val color = TonalPalette.brightness(grey, brightness.coerceAtMost(1f) * dimming)
            drawOval(
                color = TonalPalette.withAlpha(color, pebble.shade * dimming),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
            )
        }

        washes.forEach { wash ->
            val life = phase01(t * wash.cyclesPerLoop + wash.phaseOffset)
            val y = h - life * wash.maxHeightFrac * h
            val alpha = (1f - life) * wash.baseAlpha * brightness.coerceAtMost(1.2f) * dimming
            if (alpha <= 0.01f) return@forEach
            val halfSpan = wash.spanFrac * w * 0.5f
            val cx = wash.centerFrac * w
            val startX = cx - halfSpan
            val endX = cx + halfSpan
            val curveY = y - wash.curveFrac * h - life * h * 0.04f
            val path = Path().apply {
                moveTo(startX, y + h * 0.015f)
                quadraticTo((startX + endX) * 0.5f, curveY, endX, y + h * 0.015f)
            }
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, wash.colorIndex), brightness)
            val strokeWidth = (3.2f - life * 1.6f).coerceAtLeast(0.8f)
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(color, alpha.coerceIn(0f, 1f)),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private data class WashSeed(
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val maxHeightFrac: Float,
    val baseAlpha: Float,
    val centerFrac: Float,
    val spanFrac: Float,
    val curveFrac: Float,
    val colorIndex: Int,
)

private data class PebbleSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val squash: Float,
    val warmth: Float,
    val shade: Float,
)
