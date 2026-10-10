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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Two paddles + bouncing ball on a phosphor-green CRT court — soft scanlines, car-safe.
 */
@Composable
internal fun CourtBounceEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val scanCount = qualityCount(quality, low = 32, medium = 48, high = 64)
    val transition = rememberInfiniteTransition(label = "courtbounce")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "courtbounce_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val phosphor = TonalPalette.mix(Color(0xFF33FF66), TonalPalette.pick(paletteColors, 0), 0.2f)
    val ink = TonalPalette.brightness(Color(0xFF031008), brightness * dim)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = ink)

        val cycle = phase01(t)
        val green = TonalPalette.brightness(phosphor, brightness * dim)

        // Soft court border
        val margin = minOf(w, h) * 0.06f
        drawRect(
            color = TonalPalette.withAlpha(green, 0.35f * dim),
            topLeft = Offset(margin, margin),
            size = Size(w - margin * 2f, h - margin * 2f),
            style = Stroke(width = 2f),
        )
        // Center dashed line
        val dashCount = 9
        val dashH = (h - margin * 2f) / (dashCount * 2f)
        for (i in 0 until dashCount) {
            val y0 = margin + i * dashH * 2f + dashH * 0.25f
            drawRect(
                color = TonalPalette.withAlpha(green, 0.28f * dim),
                topLeft = Offset(w * 0.5f - 1.5f, y0),
                size = Size(3f, dashH),
            )
        }

        // Paddle motion (slow, independent sine)
        val leftY = h * 0.5f + sin(cycle * 2f * PI.toFloat()) * h * 0.22f
        val rightY = h * 0.5f + sin(cycle * 2f * PI.toFloat() + 1.7f) * h * 0.2f
        val paddleW = w * 0.018f
        val paddleH = h * 0.14f
        drawRect(
            color = TonalPalette.withAlpha(green, 0.85f * dim),
            topLeft = Offset(margin * 1.6f, leftY - paddleH * 0.5f),
            size = Size(paddleW, paddleH),
        )
        drawRect(
            color = TonalPalette.withAlpha(green, 0.85f * dim),
            topLeft = Offset(w - margin * 1.6f - paddleW, rightY - paddleH * 0.5f),
            size = Size(paddleW, paddleH),
        )

        // Ball path: triangle-wave bounce in x/y for a calm rally
        val bx = margin * 2.2f + abs(sin01(cycle * 2f * PI.toFloat()) * 2f - 1f) * (w - margin * 4.4f)
        val by = margin * 2f + abs(sin01(cycle * 3.2f * PI.toFloat() + 0.4f) * 2f - 1f) * (h - margin * 4f)
        val ballR = minOf(w, h) * 0.014f
        drawCircle(
            color = TonalPalette.withAlpha(green, 0.95f * dim),
            radius = ballR,
            center = Offset(bx, by),
        )
        drawCircle(
            color = TonalPalette.withAlpha(green, 0.2f * dim),
            radius = ballR * 2.4f,
            center = Offset(bx, by),
        )

        // Soft CRT scanlines
        val scanStep = h / scanCount.toFloat()
        var sy = 0f
        while (sy < h) {
            drawLine(
                color = TonalPalette.withAlpha(green, 0.04f * dim),
                start = Offset(0f, sy),
                end = Offset(w, sy),
                strokeWidth = 1f,
            )
            sy += scanStep
        }
    }
}
