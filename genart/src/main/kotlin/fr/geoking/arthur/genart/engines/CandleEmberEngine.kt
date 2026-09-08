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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** A single soft warm glow blob, breathing gently like a candle flame in a still room. */
@Composable
internal fun CandleEmberEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val haloCount = qualityCount(quality, low = 1, medium = 2, high = 3)
    val transition = rememberInfiniteTransition(label = "candle_ember")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "candle_ember_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF040203),
                0.65f to Color(0xFF0A0603),
                1f to Color(0xFF2A1206),
                startY = 0f,
                endY = h,
            ),
        )
        val time = phase01(t) * (2f * PI.toFloat())
        val dim = if (isActive) 1f else 0.6f
        val flicker = 0.5f +
            0.2f * sin(time * 1.3f) +
            0.18f * sin(time * 2.1f + 1.7f) +
            0.12f * sin(time * 0.7f + 4.1f)
        val flickerNorm = flicker.coerceIn(0.15f, 1f)

        val cx = w * 0.5f
        val cy = h * 0.72f
        val baseColor = TonalPalette.mix(Color(0xFFFFC266), TonalPalette.pick(paletteColors, 0), 0.35f)
        val glowColor = TonalPalette.brightness(baseColor, (brightness * (0.7f + 0.3f * flickerNorm) * dim).coerceAtLeast(0f))

        val coreRadius = (w.coerceAtMost(h) * 0.05f) * (0.9f + 0.15f * flickerNorm)
        for (halo in 0 until haloCount) {
            val haloScale = 1f + halo * 1.6f
            val haloRadius = coreRadius * (2.4f + haloScale * 1.8f)
            val haloAlpha = (0.5f / (halo + 1)) * flickerNorm * dim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, glowColor.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = haloRadius,
                ),
                radius = haloRadius,
                center = Offset(cx, cy),
                alpha = haloAlpha.coerceIn(0f, 1f),
            )
        }
        drawCircle(
            color = TonalPalette.brightness(glowColor, 1.15f),
            radius = coreRadius,
            center = Offset(cx, cy * 0.94f),
            alpha = (0.85f * dim).coerceIn(0f, 1f),
        )
    }
}
