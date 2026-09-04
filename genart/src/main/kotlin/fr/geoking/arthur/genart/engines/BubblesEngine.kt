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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Rising bubbles in deep water — soft rings that fade near the surface. */
@Composable
internal fun BubblesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 16, medium = 28, high = 42)
    val bubbles = remember(count) {
        List(count) { i ->
            BubbleSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                riseSpeed = seededRange(i * 41 + 11, 0.25f, 0.85f),
                radius = seededRange(i * 53 + 13, 3f, 12f),
                wobbleAmp = seededRange(i * 67 + 19, 0.01f, 0.04f),
                wobbleFreq = seededRange(i * 79 + 23, 0.5f, 2f),
                wobblePhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "bubbles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((24000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bubbles_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF04101C), Color(0xFF02060C)),
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        bubbles.forEach { b ->
            val life = phase01(b.y0 + b.riseSpeed * time)
            val y = (1f - life) * h
            val wobble = sin(time * 2f * PI.toFloat() * b.wobbleFreq + b.wobblePhase) * b.wobbleAmp
            val x = phase01(b.x0 + wobble) * w
            val fade = (life / 0.1f).coerceIn(0f, 1f) * ((1f - life) / 0.2f).coerceIn(0f, 1f)
            val base = TonalPalette.mix(Color(0xFFA8D8F0), TonalPalette.pick(paletteColors, b.colorIndex), 0.3f)
            val color = TonalPalette.brightness(base, brightness)
            val alpha = 0.35f * fade * dim
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha * 0.35f),
                radius = b.radius,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(color, alpha),
                radius = b.radius,
                center = Offset(x, y),
                style = Stroke(width = 1.2f),
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, alpha * 0.5f),
                radius = b.radius * 0.22f,
                center = Offset(x - b.radius * 0.3f, y - b.radius * 0.3f),
            )
        }
    }
}

private data class BubbleSeed(
    val x0: Float,
    val y0: Float,
    val riseSpeed: Float,
    val radius: Float,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val wobblePhase: Float,
    val colorIndex: Int,
)
