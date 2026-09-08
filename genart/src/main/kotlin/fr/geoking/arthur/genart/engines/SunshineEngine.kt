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
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bright sunny-day sky with a soft corner sun and a handful of gentle rays. */
@Composable
internal fun SunshineEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 7, high = 10)
    val rays = remember(count) {
        List(count) { i ->
            RaySeed(
                angle = seededRange(i * 19 + 5, -1.8f, 1.8f),
                widthHalf = seededRange(i * 31 + 9, 0.1f, 0.2f),
                swayAmp = seededRange(i * 43 + 13, 0.015f, 0.045f),
                swayFreq = seededRange(i * 59 + 17, 0.08f, 0.28f),
                alphaBase = seededRange(i * 71 + 23, 0.05f, 0.12f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "sunshine")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((70000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sunshine_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF8ED6FB), Color(0xFF5CB3EA), Color(0xFF2E86D4)),
            ),
        )
        val sun = Offset(w * 0.82f, h * 0.18f)
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.6f
        val breathe = 0.9f + 0.1f * sin01(time * 0.4f)
        val reach = maxOf(w, h) * 1.3f
        rays.forEach { ray ->
            val sway = ray.swayAmp * sin(time * ray.swayFreq)
            val a = -PI.toFloat() / 4f + ray.angle + sway
            val half = ray.widthHalf
            val left = Offset(
                sun.x + sin(a - half) * reach,
                sun.y + cos(a - half) * reach,
            )
            val right = Offset(
                sun.x + sin(a + half) * reach,
                sun.y + cos(a + half) * reach,
            )
            val path = Path().apply {
                moveTo(sun.x, sun.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            val base = TonalPalette.mix(Color(0xFFFFF6D9), TonalPalette.pick(paletteColors, ray.colorIndex), 0.25f)
            val tint = TonalPalette.brightness(base, brightness)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, ray.alphaBase * dim * breathe),
                        Color.Transparent,
                    ),
                    start = sun,
                    end = Offset((left.x + right.x) * 0.5f, (left.y + right.y) * 0.5f),
                ),
            )
        }
        val glowRadius = minOf(w, h) * 0.3f * (0.95f + 0.05f * breathe)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFFFFFDE7), 0.9f * dim * brightness.coerceAtMost(1.2f)),
                    TonalPalette.withAlpha(Color(0xFFFFE082), 0.5f * dim * breathe * brightness.coerceAtMost(1.2f)),
                    Color.Transparent,
                ),
                center = sun,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = sun,
        )
    }
}

private data class RaySeed(
    val angle: Float,
    val widthHalf: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
