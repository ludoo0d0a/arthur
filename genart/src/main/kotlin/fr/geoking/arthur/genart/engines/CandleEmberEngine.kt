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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
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
    val emberCount = qualityCount(quality, low = 4, medium = 6, high = 8)
    val embers = remember(emberCount) {
        List(emberCount) { i ->
            CandleSparkSeed(
                x0 = seededRange(i * 17 + 3, -0.08f, 0.08f),
                phaseOffset = seededUnit(i * 29 + 7),
                riseSpeed = seededRange(i * 41 + 11, 0.6f, 1.3f),
                sizeFrac = seededRange(i * 53 + 13, 0.004f, 0.01f),
                alphaBase = seededRange(i * 67 + 19, 0.25f, 0.55f),
                colorIndex = i,
            )
        }
    }
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
            0.12f * sin(time * 0.7f + 4.1f) +
            0.08f * sin(time * 4.3f + 2.2f)
        val flickerNorm = flicker.coerceIn(0.15f, 1f)

        val jitterX = sin(time * 3.7f) * w * 0.008f + sin(time * 6.1f + 1.2f) * w * 0.004f
        val jitterY = sin(time * 2.9f + 0.8f) * h * 0.006f
        val cx = w * 0.5f + jitterX
        val cy = h * 0.72f + jitterY
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

        // Rising ember sparks.
        val minDim = minOf(w, h)
        embers.forEach { ember ->
            val life = phase01(phase01(t) * ember.riseSpeed + ember.phaseOffset)
            val ex = cx + ember.x0 * w + sin(time * 2.2f + ember.phaseOffset * 6f) * w * 0.015f
            val ey = cy - life * h * 0.28f
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.12f).coerceIn(0f, 1f)
            val tint = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFE0A0), TonalPalette.pick(paletteColors, ember.colorIndex), 0.25f),
                brightness,
            )
            drawCircle(
                color = TonalPalette.withAlpha(tint, ember.alphaBase * fade * dim * flickerNorm),
                radius = ember.sizeFrac * minDim * (1f - life * 0.4f),
                center = Offset(ex, ey),
            )
        }
    }
}

private data class CandleSparkSeed(
    val x0: Float,
    val phaseOffset: Float,
    val riseSpeed: Float,
    val sizeFrac: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
