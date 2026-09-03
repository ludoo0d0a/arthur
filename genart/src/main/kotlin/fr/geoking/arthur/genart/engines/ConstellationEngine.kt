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
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun ConstellationEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 14, medium = 22, high = 34)
    val stars = remember(count) {
        List(count) { i ->
            ConstellationStar(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 1.2f, 3.2f),
                twinkleFreq = seededRange(i * 53 + 13, 0.4f, 1.6f),
                phaseOffset = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "constellation")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "constellation_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF07040F), Color(0xFF050914)),
            ),
        )
        val amplitude = if (isActive) 0.5f else 0.25f
        val positions = stars.map { s -> Offset(s.x * w, s.y * h) }
        for (i in stars.indices) {
            if (i % 2 != 0) continue
            val j = (i + 1) % stars.size
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, i), brightness)
            drawLine(
                color = TonalPalette.withAlpha(color, 0.09f),
                start = positions[i],
                end = positions[j],
                strokeWidth = 0.75f,
            )
        }
        stars.forEachIndexed { i, s ->
            val twinkle = 0.5f + amplitude * sin(t * 2f * PI.toFloat() * s.twinkleFreq + s.phaseOffset)
            val base = TonalPalette.pick(paletteColors, s.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            drawCircle(
                color = TonalPalette.withAlpha(color, (0.25f + 0.65f * twinkle).coerceIn(0f, 1f) * brightness.coerceAtMost(1f)),
                radius = s.radius,
                center = positions[i],
            )
        }
    }
}

private data class ConstellationStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val twinkleFreq: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)
