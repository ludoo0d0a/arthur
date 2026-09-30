package fr.geoking.arthur.fractal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Live Custom Fractal loop — sparse luminous Bezier strokes on AMOLED black.
 * Pauses visual progression when [isActive] is false (keeps last phase; low cost).
 */
@Composable
fun CustomFractalEffectCanvas(
    params: CustomFractalParams,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: FractalQuality = FractalQuality.Medium,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "custom_fractal")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "custom_phase",
    )
    val t = if (isActive) phase else 0f
    val engineQuality = remember(quality) {
        when (quality) {
            FractalQuality.Low -> CustomFractalQuality.Low
            FractalQuality.Medium -> CustomFractalQuality.Medium
            FractalQuality.High -> CustomFractalQuality.High
        }
    }
    val palette = remember(params.colorSeed) {
        CustomFractalEngine.paletteArgb(params.colorSeed).map { Color(it) }
    }
    val strokes = remember(params, t, engineQuality) {
        CustomFractalEngine.frame(params, t, engineQuality)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Color.Black)
        // Soft radial wash so gradients have atmosphere against pure black
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette[0].copy(alpha = 0.14f),
                    palette.getOrElse(3) { palette[0] }.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = maxOf(size.width, size.height) * 0.7f,
            )
        )
        val w = size.width
        val h = size.height
        val baseStroke = (minOf(w, h) * 0.006f).coerceIn(1.8f, 5f)
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val c0 = palette[stroke.colorIndex % palette.size]
            val c1 = palette[(stroke.colorIndex + 2) % palette.size]
            val alpha = (stroke.alpha * if (isActive) 1f else 0.75f).coerceIn(0.12f, 0.98f)
            val start = stroke.points.first()
            val end = stroke.points.last()
            val brush = Brush.linearGradient(
                colors = listOf(
                    c0.copy(alpha = alpha),
                    c1.copy(alpha = (alpha * 0.92f).coerceIn(0.08f, 0.98f)),
                    c0.copy(alpha = (alpha * 0.75f).coerceIn(0.08f, 0.98f)),
                ),
                start = Offset(start.x * w, start.y * h),
                end = Offset(end.x * w, end.y * h),
            )
            for (i in 0 until stroke.points.lastIndex) {
                val a = stroke.points[i]
                val b = stroke.points[i + 1]
                drawLine(
                    brush = brush,
                    start = Offset(a.x * w, a.y * h),
                    end = Offset(b.x * w, b.y * h),
                    strokeWidth = baseStroke * stroke.strokeScale,
                    cap = StrokeCap.Round,
                )
            }
        }
        // Soft anchor glow so authored taps remain readable in preview
        params.points.forEach { p ->
            val center = Offset(p.x * w, p.y * h)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.first().copy(alpha = 0.55f),
                        palette.getOrElse(2) { palette.first() }.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = baseStroke * 3.4f,
                ),
                radius = baseStroke * 3.4f,
                center = center,
            )
            drawCircle(
                color = palette.first().copy(alpha = 0.4f),
                radius = baseStroke * 2.2f,
                center = center,
                style = Stroke(width = baseStroke * 0.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
