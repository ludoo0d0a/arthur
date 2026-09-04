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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Soft diagonal rain streaks — calm weather loop, no splash flash. */
@Composable
internal fun RainEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 36, medium = 60, high = 90)
    val drops = remember(count) {
        List(count) { i ->
            RainSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                fallSpeed = seededRange(i * 41 + 11, 0.55f, 1.35f),
                lengthFrac = seededRange(i * 53 + 13, 0.018f, 0.055f),
                thickness = seededRange(i * 67 + 19, 1f, 2.4f),
                driftAmp = seededRange(i * 79 + 23, 0.004f, 0.02f),
                driftFreq = seededRange(i * 89 + 29, 0.3f, 1.4f),
                alphaBase = seededRange(i * 97 + 31, 0.18f, 0.55f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "rain")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A1220), Color(0xFF05080F)),
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val slant = 0.12f
        drops.forEach { drop ->
            val fall = phase01(drop.y0 + drop.fallSpeed * time)
            val y = fall * h
            val sway = sin(time * 2f * PI.toFloat() * drop.driftFreq) * drop.driftAmp
            val x = phase01(drop.x0 + sway) * w
            val len = drop.lengthFrac * h
            val start = Offset(x, y)
            val end = Offset(x + slant * len, y + len)
            val base = TonalPalette.mix(Color(0xFFB8D4F0), TonalPalette.pick(paletteColors, drop.colorIndex), 0.25f)
            val color = TonalPalette.brightness(base, brightness)
            drawLine(
                color = TonalPalette.withAlpha(color, drop.alphaBase * dim),
                start = start,
                end = end,
                strokeWidth = drop.thickness,
                cap = StrokeCap.Round,
            )
        }
    }
}

private data class RainSeed(
    val x0: Float,
    val y0: Float,
    val fallSpeed: Float,
    val lengthFrac: Float,
    val thickness: Float,
    val driftAmp: Float,
    val driftFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
