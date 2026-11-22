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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin

/** Sitting panda silhouette with soft black/white patches, gentle breath. */
@Composable
internal fun PandaSitEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "panda_sit_loop")
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((5800 / speed.coerceAtLeast(0.2f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "panda_sit_breath",
    )
    val restingDim = if (isActive) 1f else 0.6f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.55f

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF1E2A22),
                1f to Color(0xFF0E1410),
            ),
        )
        // Soft moss ground
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFF2A3A28), 0.45f * restingDim),
                1f to Color.Transparent,
                center = Offset(cx, h * 0.78f),
                radius = minDim * 0.45f,
            ),
            topLeft = Offset(cx - minDim * 0.45f, h * 0.68f),
            size = Size(minDim * 0.9f, minDim * 0.25f),
        )

        val breath = sin(phase01(breathAnim) * 2f * PI.toFloat()) * 0.5f + 0.5f
        val scale = 1f + (if (isActive) 0.025f else 0.01f) * breath
        val white = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8E4DC), TonalPalette.pick(paletteColors, 1), 0.12f),
            brightness * restingDim,
        )
        val black = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF141210), TonalPalette.pick(paletteColors, 0), 0.15f),
            brightness * restingDim,
        )

        val bodyRx = minDim * 0.22f * scale
        val bodyRy = minDim * 0.26f * scale
        drawOval(
            color = white,
            topLeft = Offset(cx - bodyRx, cy - bodyRy * 0.3f),
            size = Size(bodyRx * 2f, bodyRy * 2f),
        )
        // Belly patch
        drawOval(
            color = TonalPalette.withAlpha(black, 0.55f),
            topLeft = Offset(cx - bodyRx * 0.55f, cy + bodyRy * 0.15f),
            size = Size(bodyRx * 1.1f, bodyRy * 0.9f),
        )
        val headR = minDim * 0.14f * scale
        val head = Offset(cx, cy - bodyRy * 0.55f)
        drawCircle(color = white, radius = headR, center = head)
        // Ears
        drawCircle(color = black, radius = headR * 0.38f, center = Offset(head.x - headR * 0.75f, head.y - headR * 0.75f))
        drawCircle(color = black, radius = headR * 0.38f, center = Offset(head.x + headR * 0.75f, head.y - headR * 0.75f))
        // Eye patches
        if (quality != GenartQuality.Low) {
            drawOval(
                color = black,
                topLeft = Offset(head.x - headR * 0.75f, head.y - headR * 0.25f),
                size = Size(headR * 0.55f, headR * 0.45f),
            )
            drawOval(
                color = black,
                topLeft = Offset(head.x + headR * 0.2f, head.y - headR * 0.25f),
                size = Size(headR * 0.55f, headR * 0.45f),
            )
        }
        // Arms / legs blobs
        drawCircle(color = black, radius = minDim * 0.055f * scale, center = Offset(cx - bodyRx * 0.85f, cy + bodyRy * 0.35f))
        drawCircle(color = black, radius = minDim * 0.055f * scale, center = Offset(cx + bodyRx * 0.85f, cy + bodyRy * 0.35f))
        drawCircle(color = black, radius = minDim * 0.06f * scale, center = Offset(cx - bodyRx * 0.35f, cy + bodyRy * 1.35f))
        drawCircle(color = black, radius = minDim * 0.06f * scale, center = Offset(cx + bodyRx * 0.35f, cy + bodyRy * 1.35f))
    }
}
