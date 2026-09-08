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
import kotlin.math.cos
import kotlin.math.sin

/** Pale sparkle glyphs scattered across the frame, each fading in, holding, then fading out on its own slow cycle, like frost forming and receding across a window. */
@Composable
internal fun FrostCrystalsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 18, medium = 30, high = 46)
    val crystals = remember(count) {
        List(count) { i ->
            val s = i * 131 + 7
            FrostCrystal(
                x = seededUnit(s + 3),
                y = seededUnit(s + 11),
                radius = seededRange(s + 19, 3f, 7f),
                lineCount = if (seededUnit(s + 23) < 0.5f) 2 else 3,
                rotation = seededRange(s + 29, 0f, 2f * PI.toFloat()),
                cycleFreq = seededRange(s + 37, 0.4f, 0.9f),
                phaseOffset = seededUnit(s + 43),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "frostcrystals")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "frostcrystals_t",
    )
    val dim = if (isActive) 1f else 0.5f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF060B14), Color(0xFF0C1A26)),
            ),
        )
        crystals.forEach { c ->
            val cyclePos = phase01(t * c.cycleFreq + c.phaseOffset)
            val envelope = frostEnvelope(cyclePos)
            if (envelope <= 0.001f) return@forEach
            val base = TonalPalette.mix(Color.White, TonalPalette.pick(paletteColors, c.colorIndex), 0.3f)
            val tint = TonalPalette.brightness(base, brightness)
            val alpha = (envelope * dim).coerceIn(0f, 1f)
            val center = Offset(c.x * w, c.y * h)
            val armLength = c.radius * (0.75f + 0.25f * envelope)
            val angleStep = PI.toFloat() / c.lineCount
            for (line in 0 until c.lineCount) {
                val angle = c.rotation + line * angleStep
                val dx = cos(angle) * armLength
                val dy = sin(angle) * armLength
                drawLine(
                    color = TonalPalette.withAlpha(tint, alpha),
                    start = Offset(center.x - dx, center.y - dy),
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = 1.1f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun frostEnvelope(cyclePos: Float): Float {
    val fadeIn = 0.35f
    val fadeOut = 0.65f
    return when {
        cyclePos < fadeIn -> smoothStep(cyclePos / fadeIn)
        cyclePos < fadeOut -> 1f
        else -> smoothStep((1f - cyclePos) / (1f - fadeOut))
    }
}

private fun smoothStep(x: Float): Float {
    val u = x.coerceIn(0f, 1f)
    return u * u * (3f - 2f * u)
}

private data class FrostCrystal(
    val x: Float,
    val y: Float,
    val radius: Float,
    val lineCount: Int,
    val rotation: Float,
    val cycleFreq: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)
