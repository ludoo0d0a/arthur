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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

/**
 * Scrolling horizontal color bands over a checker floor — demoscene copper-bars vibe.
 */
@Composable
internal fun CopperBarsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val barCount = qualityCount(quality, low = 7, medium = 10, high = 14)
    val checkerCols = qualityCount(quality, low = 8, medium = 10, high = 14)
    val transition = rememberInfiniteTransition(label = "copperbars")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "copperbars_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val cycle = phase01(t)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.52f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0614), Color(0xFF120A1C), Color(0xFF050308)),
            ),
        )

        // Checker floor vanishing toward horizon
        val rows = checkerCols
        val scrollFloor = phase01(cycle * 0.4f)
        for (row in 0 until rows) {
            val depth = (row + scrollFloor) / rows.toFloat()
            val nearness = 1f / (1f + depth * 4f)
            val y0 = horizon + (h - horizon) * (1f - nearness)
            val nextDepth = (row + 1 + scrollFloor) / rows.toFloat()
            val nextNear = 1f / (1f + nextDepth * 4f)
            val y1 = horizon + (h - horizon) * (1f - nextNear)
            val halfFar = w * 0.04f
            val halfNear = w * 0.55f
            val half0 = halfFar + (halfNear - halfFar) * (1f - nearness)
            val half1 = halfFar + (halfNear - halfFar) * (1f - nextNear)
            val cols = checkerCols
            for (col in 0 until cols) {
                val u0 = col / cols.toFloat()
                val u1 = (col + 1) / cols.toFloat()
                val dark = ((row + col) % 2 == 0)
                val base = if (dark) Color(0xFF1A1428) else Color(0xFF2A2038)
                val floor = TonalPalette.brightness(
                    TonalPalette.mix(base, TonalPalette.pick(paletteColors, col), 0.15f),
                    brightness * dim * (0.4f + 0.6f * (1f - nearness)),
                )
                val x0a = w * 0.5f - half0 + u0 * half0 * 2f
                val x0b = w * 0.5f - half0 + u1 * half0 * 2f
                val x1a = w * 0.5f - half1 + u0 * half1 * 2f
                val x1b = w * 0.5f - half1 + u1 * half1 * 2f
                val path = Path().apply {
                    moveTo(x0a, y0)
                    lineTo(x0b, y0)
                    lineTo(x1b, y1)
                    lineTo(x1a, y1)
                    close()
                }
                drawPath(path, color = TonalPalette.withAlpha(floor, 0.85f * dim))
            }
        }

        // Copper bars scrolling vertically with soft sine thickness
        val bandHues = listOf(
            Color(0xFFFF6B2D),
            Color(0xFFFFB347),
            Color(0xFFFF3D7A),
            Color(0xFFFFD166),
            Color(0xFFE85D04),
            Color(0xFFFF8FAB),
        )
        val scroll = phase01(cycle)
        for (i in 0 until barCount) {
            val raw = phase01(i / barCount.toFloat() + scroll)
            val y = raw * h
            val thickness = h * (0.018f + 0.012f * sin01(raw * 2f * PI.toFloat() + i))
            val hue = bandHues[i % bandHues.size]
            val bar = TonalPalette.brightness(
                TonalPalette.mix(hue, TonalPalette.pick(paletteColors, i), 0.25f),
                brightness * dim,
            )
            val alpha = (0.35f + 0.35f * sin(raw * PI.toFloat())).coerceIn(0.15f, 0.7f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(bar, alpha * dim),
                        TonalPalette.withAlpha(bar, alpha * 0.5f * dim),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(0f, y - thickness),
                size = Size(w, thickness * 2f),
            )
        }
    }
}
