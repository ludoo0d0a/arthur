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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun TunnelEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rings = qualityCount(quality, low = 10, medium = 16, high = 24)
    val segments = qualityCount(quality, low = 12, medium = 18, high = 28)
    val transition = rememberInfiniteTransition(label = "tunnel")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tunnel_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = min(w, h) * 0.72f
        drawRect(color = Color(0xFF020617))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF020617)),
                center = Offset(cx, cy),
                radius = maxR,
            ),
            radius = maxR,
            center = Offset(cx, cy),
        )
        val scroll = phase01(t)
        for (i in 0 until rings) {
            val depth = phase01((i.toFloat() / rings) + scroll)
            val radius = 8f + depth * depth * depth
            val alpha = (0.15f + 0.7f * (1f - depth)).coerceIn(0.08f, 0.85f) * brightness.coerceAtMost(1.2f)
            val color = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, i),
                brightness,
            )
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha * 0.55f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = if (isActive) 2.4f else 1.6f),
            )
            val segCount = if (isActive) segments else segments / 2
            val spin = scroll * 2f * PI.toFloat() + i * 0.2f
            for (s in 0 until segCount) {
                val a0 = spin + s * (2f * PI.toFloat() / segCount)
                val a1 = a0 + (2f * PI.toFloat() / segCount) * 0.45f
                val rInner = radius * 0.86f
                val p0 = Offset(cx + cos(a0) * rInner, cy + sin(a0) * rInner)
                val p1 = Offset(cx + cos(a1) * radius, cy + sin(a1) * radius)
                drawLine(
                    color = TonalPalette.withAlpha(color, alpha),
                    start = p0,
                    end = p1,
                    strokeWidth = if (isActive) 2f else 1.4f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(
            color = TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 0), 0.35f * brightness),
            radius = 10f,
            center = Offset(cx, cy),
        )
    }
}
