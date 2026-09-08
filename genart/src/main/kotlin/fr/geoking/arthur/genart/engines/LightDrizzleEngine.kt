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

/** Sparse, slow dawn-grey drizzle streaks — gentler mood cousin of Rain. */
@Composable
internal fun LightDrizzleEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 12, medium = 20, high = 30)
    val drops = remember(count) {
        List(count) { i ->
            DrizzleSeed(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededUnit(i * 31 + 9),
                fallSpeed = seededRange(i * 43 + 13, 0.25f, 0.6f),
                lengthFrac = seededRange(i * 59 + 17, 0.01f, 0.028f),
                thickness = seededRange(i * 71 + 21, 0.6f, 1.4f),
                driftAmp = seededRange(i * 83 + 25, 0.003f, 0.014f),
                driftFreq = seededRange(i * 101 + 33, 0.2f, 1f),
                alphaBase = seededRange(i * 109 + 37, 0.08f, 0.28f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "light_drizzle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "light_drizzle_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3E4A5E), Color(0xFF2B3444)),
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val slant = 0.08f
        drops.forEach { drop ->
            val fall = phase01(drop.y0 + drop.fallSpeed * time)
            val y = fall * h
            val sway = sin(time * 2f * PI.toFloat() * drop.driftFreq) * drop.driftAmp
            val x = phase01(drop.x0 + sway) * w
            val len = drop.lengthFrac * h
            val start = Offset(x, y)
            val end = Offset(x + slant * len, y + len)
            val base = TonalPalette.mix(Color(0xFFD6E2F0), TonalPalette.pick(paletteColors, drop.colorIndex), 0.15f)
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

private data class DrizzleSeed(
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
