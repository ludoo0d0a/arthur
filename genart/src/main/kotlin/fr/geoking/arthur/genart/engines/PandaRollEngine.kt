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
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Soft panda silhouette gently rocking / rolling side to side. */
@Composable
internal fun PandaRollEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "panda_roll_loop")
    val rollAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_roll_t",
    )
    val restingDim = if (isActive) 1f else 0.65f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.55f
        val t = phase01(rollAnim)

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF1C2820),
                1f to Color(0xFF0C120E),
            ),
        )
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFF2A3828), 0.4f * restingDim),
                1f to Color.Transparent,
                center = Offset(cx, h * 0.75f),
                radius = minDim * 0.4f,
            ),
            topLeft = Offset(cx - minDim * 0.4f, h * 0.65f),
            size = Size(minDim * 0.8f, minDim * 0.22f),
        )

        val rock = sin(t * 2f * PI.toFloat()) * (if (isActive) 18f else 6f)
        val white = TonalPalette.brightness(Color(0xFFE8E4DC), brightness * restingDim)
        val black = TonalPalette.brightness(Color(0xFF12100E), brightness * restingDim)
        val bodyR = minDim * 0.20f

        rotate(degrees = rock, pivot = Offset(cx, cy + bodyR * 0.3f)) {
            drawCircle(color = white, radius = bodyR, center = Offset(cx, cy))
            drawCircle(
                color = TonalPalette.withAlpha(black, 0.5f),
                radius = bodyR * 0.55f,
                center = Offset(cx, cy + bodyR * 0.15f),
            )
            val headR = bodyR * 0.55f
            val head = Offset(cx - bodyR * 0.15f, cy - bodyR * 0.75f)
            drawCircle(color = white, radius = headR, center = head)
            drawCircle(color = black, radius = headR * 0.32f, center = Offset(head.x - headR * 0.7f, head.y - headR * 0.7f))
            drawCircle(color = black, radius = headR * 0.32f, center = Offset(head.x + headR * 0.7f, head.y - headR * 0.7f))
            if (quality != GenartQuality.Low) {
                drawOval(
                    color = black,
                    topLeft = Offset(head.x - headR * 0.7f, head.y - headR * 0.2f),
                    size = Size(headR * 0.5f, headR * 0.4f),
                )
                drawOval(
                    color = black,
                    topLeft = Offset(head.x + headR * 0.2f, head.y - headR * 0.2f),
                    size = Size(headR * 0.5f, headR * 0.4f),
                )
            }
            // Limb blobs suggesting a roll
            drawCircle(color = black, radius = bodyR * 0.22f, center = Offset(cx - bodyR * 0.75f, cy + bodyR * 0.35f))
            drawCircle(color = black, radius = bodyR * 0.22f, center = Offset(cx + bodyR * 0.8f, cy - bodyR * 0.15f))
            drawCircle(color = black, radius = bodyR * 0.2f, center = Offset(cx + bodyR * 0.35f, cy + bodyR * 0.75f))
        }
    }
}
