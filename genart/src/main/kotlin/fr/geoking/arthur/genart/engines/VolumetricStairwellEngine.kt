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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class StairwellDust(
    val x: Float,
    val y: Float,
    val size: Float,
    val phase: Float,
    val drift: Float,
)

/** A quiet, warmly lit stairwell with slow, seamless volumetric light. */
@Composable
internal fun VolumetricStairwellEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val dust = remember(count) {
        List(count) { i ->
            StairwellDust(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                size = seededRange(i * 41 + 11, 0.0015f, 0.004f),
                phase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                drift = seededRange(i * 67 + 19, 0.008f, 0.025f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "volumetricstairwell")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "volumetricstairwell_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val haze = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val sway = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = 37) - 0.5f
        val warm = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8D8B0), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        val wall = TonalPalette.brightness(Color(0xFF2A2E38), brightness * dim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF12141A), brightness * dim),
                0.58f to wall,
                1f to Color(0xFF17191F),
            ),
        )

        // Enclosing walls converge on a small, softly lit landing.
        val openingX = w * (0.52f + sway * 0.035f)
        val openingY = h * 0.19f
        val openingW = w * 0.28f
        val openingH = h * 0.35f
        val opening = Path().apply {
            moveTo(openingX - openingW / 2f, openingY + openingH)
            lineTo(openingX - openingW / 2f, openingY + openingH * 0.32f)
            quadraticTo(openingX - openingW / 2f, openingY, openingX, openingY)
            quadraticTo(openingX + openingW / 2f, openingY, openingX + openingW / 2f, openingY + openingH * 0.32f)
            lineTo(openingX + openingW / 2f, openingY + openingH)
            close()
        }
        drawPath(
            path = opening,
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(warm, 0.46f * dim),
                0.52f to TonalPalette.withAlpha(warm, 0.13f * dim),
                1f to Color(0xFF17191F),
                startY = openingY,
                endY = openingY + openingH,
            ),
        )
        drawPath(path = opening, color = Color(0xFF080A10).copy(alpha = 0.72f * dim), style = Stroke(width = unit * 0.012f))

        // Broad transparent cones suggest dusty shafts without sharp light edges.
        for (beam in 0..2) {
            val centerX = openingX + (beam - 1) * openingW * 0.18f + sway * unit * 0.06f
            val beamPath = Path().apply {
                moveTo(centerX - openingW * 0.12f, openingY + openingH * 0.38f)
                lineTo(centerX + openingW * 0.12f, openingY + openingH * 0.38f)
                lineTo(centerX + openingW * (0.34f + beam * 0.04f), h * 0.78f)
                lineTo(centerX - openingW * (0.34f + beam * 0.04f), h * 0.78f)
                close()
            }
            drawPath(
                path = beamPath,
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to TonalPalette.withAlpha(warm, (0.035f + 0.012f * haze) * dim),
                    1f to Color.Transparent,
                    startX = centerX - openingW * 0.4f,
                    endX = centerX + openingW * 0.4f,
                ),
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(warm, (0.22f + haze * 0.06f) * dim),
                0.55f to TonalPalette.withAlpha(warm, 0.055f * dim),
                1f to Color.Transparent,
                center = Offset(openingX, openingY + openingH * 0.45f),
                radius = unit * 0.72f,
            ),
            radius = unit * 0.72f,
            center = Offset(openingX, openingY + openingH * 0.45f),
        )

        // Receding stair treads form a clear central flight.
        val steps = 9
        for (i in 0 until steps) {
            val depth = i / (steps - 1f)
            val y = h * (0.48f + depth * 0.49f)
            val halfWidth = w * (0.10f + depth * 0.32f)
            val topY = y - h * (0.018f + depth * 0.038f)
            val left = openingX - halfWidth
            val right = openingX + halfWidth
            val step = Path().apply {
                moveTo(left, topY)
                lineTo(right, topY)
                lineTo(right + w * 0.025f * depth, y)
                lineTo(left - w * 0.025f * depth, y)
                close()
            }
            val shade = (0.18f + depth * 0.38f) * dim
            drawPath(path = step, color = TonalPalette.withAlpha(Color(0xFF080A10), shade))
            drawLine(
                color = TonalPalette.withAlpha(warm, (0.07f + 0.11f * (1f - depth) * (0.85f + haze * 0.15f)) * dim),
                start = Offset(left, topY),
                end = Offset(right, topY),
                strokeWidth = unit * (0.002f + depth * 0.003f),
            )
        }

        // Quiet wall mouldings add depth and frame the stair opening.
        drawLine(color = Color(0xFF080A10).copy(alpha = 0.38f * dim), start = Offset(w * 0.12f, h * 0.14f), end = Offset(w * 0.29f, h), strokeWidth = unit * 0.009f)
        drawLine(color = Color(0xFF080A10).copy(alpha = 0.38f * dim), start = Offset(w * 0.88f, h * 0.14f), end = Offset(w * 0.71f, h), strokeWidth = unit * 0.009f)

        dust.forEachIndexed { index, mote ->
            val noise = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = index * 97 + 5)
            val x = (mote.x + sway * mote.drift + (noise - 0.5f) * mote.drift * 0.45f) * w
            val y = ((mote.y - time * mote.drift + mote.drift * sin(time * 2f * PI.toFloat() + mote.phase) * 0.18f + 1f) % 1f) * h
            val alpha = (0.16f + 0.22f * noise) * dim
            drawCircle(
                color = TonalPalette.withAlpha(warm, alpha),
                radius = unit * mote.size * (0.7f + noise * 0.7f),
                center = Offset(x, y),
            )
        }

        drawRect(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.62f to Color.Transparent,
                1f to Color(0xFF080A10).copy(alpha = 0.45f * dim),
                center = Offset(w * 0.5f, h * 0.46f),
                radius = unit * 0.82f,
            ),
        )
    }
}
