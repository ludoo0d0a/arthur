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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Sitting cat silhouette with a slowly swaying tail. */
@Composable
internal fun CatTailEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cat_tail_loop")
    val swayAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cat_tail_sway",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.48f
        val cy = h * 0.58f
        val t = phase01(swayAnim)

        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF2A2430),
                1f to Color(0xFF100E14),
                center = Offset(cx, cy),
                radius = w.coerceAtLeast(h) * 0.85f,
            ),
        )

        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF1C1614), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * restingDim,
        )
        val bodyRx = minDim * 0.16f
        val bodyRy = minDim * 0.20f
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, cy - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )
        val headR = minDim * 0.11f
        val head = Offset(cx, cy - bodyRy * 1.15f)
        drawCircle(color = bodyColor, radius = headR, center = head)
        val ears = Path().apply {
            moveTo(head.x - headR * 0.7f, head.y - headR * 0.2f)
            lineTo(head.x - headR * 0.95f, head.y - headR * 1.35f)
            lineTo(head.x - headR * 0.15f, head.y - headR * 0.5f)
            close()
            moveTo(head.x + headR * 0.15f, head.y - headR * 0.5f)
            lineTo(head.x + headR * 0.95f, head.y - headR * 1.35f)
            lineTo(head.x + headR * 0.7f, head.y - headR * 0.2f)
            close()
        }
        drawPath(ears, color = bodyColor)

        val sway = sin(t * 2f * PI.toFloat()) * (if (isActive) 1f else 0.35f)
        val tail = Path().apply {
            val base = Offset(cx + bodyRx * 0.75f, cy + bodyRy * 0.15f)
            val mid = Offset(
                base.x + minDim * (0.12f + 0.04f * sway),
                base.y - minDim * 0.18f,
            )
            val tip = Offset(
                base.x + minDim * (0.08f + 0.12f * sway),
                base.y - minDim * 0.38f,
            )
            moveTo(base.x, base.y)
            quadraticTo(mid.x, mid.y, tip.x, tip.y)
        }
        val strokeW = minDim * (if (quality == GenartQuality.Low) 0.028f else 0.032f)
        drawPath(tail, color = bodyColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}
