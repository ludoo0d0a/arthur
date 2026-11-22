package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.FastOutSlowInEasing
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

/** Dog silhouette in a slow play-bow / stretch pose. */
@Composable
internal fun DogStretchEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dog_stretch_loop")
    val stretchAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((8000 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dog_stretch_t",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.58f

        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF2C261C),
                1f to Color(0xFF100E0A),
                center = Offset(cx, cy),
                radius = w.coerceAtLeast(h) * 0.8f,
            ),
        )

        val stretch = sin(phase01(stretchAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val amount = (if (isActive) 1f else 0.4f) * stretch
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A1C12), TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * restingDim,
        )

        // Elongating body in play-bow
        val bodyRx = minDim * (0.26f + 0.04f * amount)
        val bodyRy = minDim * (0.10f - 0.02f * amount)
        val bodyY = cy + minDim * 0.02f * amount
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, bodyY - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )
        val headR = minDim * 0.09f
        val head = Offset(cx - bodyRx * 0.95f, bodyY - minDim * 0.04f - minDim * 0.03f * amount)
        drawCircle(color = bodyColor, radius = headR, center = head)
        // Snout
        drawOval(
            color = bodyColor,
            topLeft = Offset(head.x - headR * 1.2f, head.y - headR * 0.25f),
            size = Size(headR * 0.7f, headR * 0.5f),
        )
        // Ear
        val ear = Path().apply {
            moveTo(head.x + headR * 0.1f, head.y - headR * 0.4f)
            lineTo(head.x + headR * 0.55f, head.y - headR * 1.1f)
            lineTo(head.x + headR * 0.55f, head.y - headR * 0.1f)
            close()
        }
        drawPath(ear, color = bodyColor)

        // Front legs lowered, rear up
        val legStroke = minDim * 0.028f
        val front = Path().apply {
            moveTo(cx - bodyRx * 0.55f, bodyY)
            lineTo(cx - bodyRx * 0.7f, bodyY + minDim * (0.16f + 0.04f * amount))
            moveTo(cx - bodyRx * 0.25f, bodyY)
            lineTo(cx - bodyRx * 0.35f, bodyY + minDim * (0.15f + 0.035f * amount))
        }
        drawPath(front, color = bodyColor, style = Stroke(width = legStroke, cap = StrokeCap.Round))
        val rear = Path().apply {
            moveTo(cx + bodyRx * 0.45f, bodyY - bodyRy * 0.3f)
            lineTo(cx + bodyRx * 0.55f, bodyY + minDim * 0.08f)
            moveTo(cx + bodyRx * 0.75f, bodyY - bodyRy * 0.2f)
            lineTo(cx + bodyRx * 0.85f, bodyY + minDim * 0.07f)
        }
        drawPath(rear, color = bodyColor, style = Stroke(width = legStroke, cap = StrokeCap.Round))

        if (quality != GenartQuality.Low) {
            val tail = Path().apply {
                val base = Offset(cx + bodyRx * 0.95f, bodyY - bodyRy)
                moveTo(base.x, base.y)
                quadraticTo(
                    base.x + minDim * 0.05f,
                    base.y - minDim * 0.08f,
                    base.x + minDim * 0.02f,
                    base.y - minDim * 0.14f,
                )
            }
            drawPath(tail, color = bodyColor, style = Stroke(width = legStroke * 0.9f, cap = StrokeCap.Round))
        }
    }
}
