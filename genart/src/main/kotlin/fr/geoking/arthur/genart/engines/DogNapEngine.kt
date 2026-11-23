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

        // Soft floor mat / shadow pool
        val matRx = minDim * 0.46f
        val matRy = minDim * 0.24f
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 1), 0.22f * restingDim),
                0.6f to TonalPalette.withAlpha(Color(0xFF33251B), 0.35f * restingDim),
                1f to Color.Transparent,
                center = Offset(cx, cy + minDim * 0.05f),
                radius = matRx,
            ),
            topLeft = Offset(cx - matRx, cy + minDim * 0.05f - matRy),
            size = Size(matRx * 2f, matRy * 2f),
        )

        val breath = sin(phase01(breathAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.028f else 0.01f) * breath
        val bodyColor = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF2A1E14), TonalPalette.pick(paletteColors, 0), 0.3f),
            brightness * restingDim,
        )

        // Curled body
        val bodyRx = minDim * 0.30f * scale
        val bodyRy = minDim * 0.20f * scale
        drawOval(
            color = bodyColor,
            topLeft = Offset(cx - bodyRx, cy - bodyRy),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )

        // Head resting on ground
        val headR = minDim * 0.14f * scale
        val head = Offset(cx - bodyRx * 0.5f, cy - bodyRy * 0.25f)
        drawCircle(color = bodyColor, radius = headR, center = head)

        // Snout
        drawOval(
            color = bodyColor,
            topLeft = Offset(head.x - headR * 1.15f, head.y - headR * 0.25f),
            size = Size(headR * 0.7f, headR * 0.55f),
        )

        // Front paw tucked under snout
        drawOval(
            color = bodyColor,
            topLeft = Offset(head.x - headR * 0.9f, head.y + headR * 0.25f),
            size = Size(headR * 0.65f, headR * 0.35f),
        )

        // Floppy ear with gentle breathing sway
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

        // Sleeping eye curve
        val eyePath = Path().apply {
            val ex = head.x - headR * 0.35f
            val ey = head.y - headR * 0.05f
            moveTo(ex - headR * 0.2f, ey)
            quadraticTo(ex, ey + headR * 0.12f, ex + headR * 0.2f, ey)
        }
        drawPath(
            path = eyePath,
            color = TonalPalette.withAlpha(TonalPalette.pick(paletteColors, 2), 0.35f * restingDim),
            style = Stroke(width = minDim * 0.008f, cap = StrokeCap.Round),
        )

        // Curled tail along hind flank
        val tail = Path().apply {
            val base = Offset(cx + bodyRx * 0.75f, cy)
            val tipX = base.x + minDim * 0.05f
            val tipY = base.y - minDim * (0.12f + 0.02f * breath)
            moveTo(base.x, base.y)
            quadraticTo(
                base.x + minDim * 0.07f,
                base.y - minDim * 0.06f,
                tipX,
                tipY,
            )
        }
        drawPath(
            path = tail,
            color = bodyColor,
            style = Stroke(width = minDim * 0.032f, cap = StrokeCap.Round),
        )
    }
}
