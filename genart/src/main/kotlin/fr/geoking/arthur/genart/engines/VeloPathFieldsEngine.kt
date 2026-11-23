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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV Velo Path and Fields — seamless loop via loopedFbm. */
@Composable
internal fun VeloPathFieldsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 15, high = 24)
    val grass = remember(count) {
        List(count) { i ->
            floatArrayOf(
                seededUnit(i * 17 + 3),
                seededRange(i * 29 + 7, 0.58f, 1.02f),
                seededRange(i * 41 + 11, 0.015f, 0.075f),
                seededRange(i * 53 + 13, 0.25f, 0.85f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "velopathfields")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "velopathfields_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val horizon = h * (0.49f + 0.025f * (wash - 0.5f))
        val sky = TonalPalette.brightness(Color(0xFF6A9A58), brightness * dim)
        val field = TonalPalette.brightness(Color(0xFF3A6040), brightness * dim)
        val sun = TonalPalette.brightness(Color(0xFFE8E0A0), brightness * dim)
        drawRect(brush = Brush.verticalGradient(listOf(sky, Color(0xFF9AAD73), field), endY = h))

        // Soft dawn haze where the open fields meet the sky.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to TonalPalette.withAlpha(sun, 0.23f * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.13f,
                endY = horizon + h * 0.14f,
            ),
            topLeft = Offset(0f, horizon - h * 0.14f),
            size = Size(w, h * 0.28f),
        )

        // Low rolling field bands establish distance without hard geometry.
        for (layer in 0..2) {
            val y = horizon + h * (0.09f + layer * 0.10f)
            val bandColor = when (layer) {
                0 -> Color(0xFF718C55)
                1 -> Color(0xFF557546)
                else -> Color(0xFF345338)
            }
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(w * 0.23f, y - h * 0.055f, w * 0.69f, y + h * 0.04f, w, y - h * 0.02f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, TonalPalette.withAlpha(TonalPalette.brightness(bandColor, brightness), (0.52f + layer * 0.13f) * dim))
        }

        // Narrow path expands toward the rider, centered on the vanishing point.
        val drift = (loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 61) - 0.5f) * w * 0.035f
        val cx = w * 0.5f + drift
        val pathLeft = Path().apply {
            moveTo(cx - w * 0.016f, horizon + h * 0.045f)
            lineTo(w * 0.06f, h)
            lineTo(w * 0.39f, h)
            lineTo(cx - w * 0.016f, horizon + h * 0.045f)
            close()
        }
        val pathRight = Path().apply {
            moveTo(cx + w * 0.016f, horizon + h * 0.045f)
            lineTo(w * 0.61f, h)
            lineTo(w * 0.94f, h)
            lineTo(cx + w * 0.016f, horizon + h * 0.045f)
            close()
        }
        val pathFill = TonalPalette.brightness(Color(0xFF6B6948), brightness * dim)
        drawPath(pathLeft, pathFill)
        drawPath(pathRight, pathFill)

        // Delicate edge lines and broken center dashes emphasize the POV lane.
        val edge = TonalPalette.withAlpha(Color(0xFFD8D39D), 0.30f * dim)
        drawLine(edge, Offset(cx - w * 0.016f, horizon + h * 0.045f), Offset(w * 0.06f, h), minDim * 0.003f)
        drawLine(edge, Offset(cx + w * 0.016f, horizon + h * 0.045f), Offset(w * 0.94f, h), minDim * 0.003f)
        for (i in 0..5) {
            val u = ((i / 6f + time) % 1f)
            val y = horizon + h * 0.08f + (u * u) * (h - horizon - h * 0.08f)
            val half = w * (0.002f + 0.012f * u)
            drawLine(
                TonalPalette.withAlpha(sun, (0.40f - u * 0.15f) * dim),
                Offset(cx - half, y), Offset(cx + half, y), minDim * (0.002f + u * 0.005f),
            )
        }

        // Sparse foreground grasses sway in the breeze.
        grass.forEachIndexed { i, g ->
            val x = g[0] * w
            val baseY = g[1] * h
            val height = g[2] * minDim
            val sway = sin(time * 2f * PI.toFloat() + i * 0.9f) * height * 0.30f
            val color = TonalPalette.withAlpha(Color(0xFFC7C58A), g[3] * 0.43f * dim)
            drawLine(color, Offset(x, baseY), Offset(x + sway, baseY - height), minDim * 0.002f)
        }
    }
}
