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

/** Soft dog silhouette curled up for a nap, gentle breath and ear flop. */
@Composable
internal fun DogNapEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dog_nap_loop")
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((6000 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_nap_breath",
    )
    val earAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((14000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_nap_ear",
    )
    val restingDim = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.56f

        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF2E261E),
                0.6f to Color(0xFF1B1612),
                1f to Color(0xFF0E0B09),
                center = Offset(cx, cy + minDim * 0.05f),
                radius = w.coerceAtLeast(h) * 0.8f,
            ),
        )

        val breath = sin(phase01(breathAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.028f else 0.01f) * breath
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A1E14), TonalPalette.pick(paletteColors, 0), 0.3f),
            brightness * restingDim,
        )

        val bodyRx = minDim * 0.30f * scale
        val bodyRy = minDim * 0.20f * scale
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, cy - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )
        val headR = minDim * 0.14f * scale
        val head = Offset(cx - bodyRx * 0.5f, cy - bodyRy * 0.25f)
        drawCircle(color = bodyColor, radius = headR, center = head)
        // Soft snout
        drawOval(
            color = bodyColor,
            topLeft = Offset(head.x - headR * 1.15f, head.y - headR * 0.25f),
            size = Size(headR * 0.7f, headR * 0.55f),
        )
        // Floppy ear
        val earSwing = sin(phase01(earAnim) * 2f * PI.toFloat()) * (if (isActive) 0.08f else 0.03f)
        val ear = Path().apply {
            moveTo(head.x + headR * 0.2f, head.y - headR * 0.4f)
            quadraticTo(
                head.x + headR * (0.9f + earSwing),
                head.y + headR * 0.1f,
                head.x + headR * 0.55f,
                head.y + headR * 0.85f,
            )
            quadraticTo(
                head.x + headR * 0.15f,
                head.y + headR * 0.35f,
                head.x + headR * 0.2f,
                head.y - headR * 0.4f,
            )
            close()
        }
        drawPath(ear, color = bodyColor)

        if (quality != GenartQuality.Low) {
            val tail = Path().apply {
                val base = Offset(cx + bodyRx * 0.75f, cy)
                moveTo(base.x, base.y)
                quadraticTo(
                    base.x + minDim * 0.06f,
                    base.y - minDim * 0.08f,
                    base.x + minDim * 0.04f,
                    base.y - minDim * 0.14f,
                )
            }
            drawPath(
                path = tail,
                color = bodyColor,
                style = Stroke(width = minDim * 0.028f, cap = StrokeCap.Round),
            )
        }
    }
}
