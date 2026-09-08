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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** A winding river band crossing soft green/brown banks, with drifting ripple highlights. */
@Composable
internal fun RiversEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rippleCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val ripples = remember(rippleCount) {
        List(rippleCount) { i ->
            RiverRippleSeed(
                phaseOffset = seededUnit(i * 47 + 3),
                cyclesPerLoop = seededRange(i * 61 + 7, 0.6f, 1.1f),
                travelOffset = seededUnit(i * 71 + 9),
                maxRadiusFrac = seededRange(i * 79 + 13, 0.02f, 0.05f),
                baseAlpha = seededRange(i * 97 + 17, 0.25f, 0.45f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "rivers")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rivers_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val bankTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness * 0.7f)
        val bankBottom = TonalPalette.brightness(TonalPalette.pick(paletteColors, 3), brightness * 0.55f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(bankTop, bankBottom),
            ),
        )

        val segments = 48
        val amp1 = 0.12f * h
        val amp2 = 0.06f * h
        val amp3 = 0.03f * h
        val freq1 = 2f * PI.toFloat() / w.coerceAtLeast(1f)
        val timePhase = phase01(t) * 2f * PI.toFloat()

        fun riverX(fracY: Float): Float {
            val y = fracY * h
            return w * 0.5f +
                amp1 * sin(y * freq1 * 1.3f + timePhase * 0.6f) +
                amp2 * sin(y * freq1 * 2.7f - timePhase * 0.9f + 1.7f) +
                amp3 * sin(y * freq1 * 4.1f + timePhase * 1.3f + 0.4f)
        }

        val halfWidth = w * 0.09f
        val path = Path()
        for (k in 0..segments) {
            val fracY = k / segments.toFloat()
            val y = fracY * h
            val x = riverX(fracY) - halfWidth
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        for (k in segments downTo 0) {
            val fracY = k / segments.toFloat()
            val y = fracY * h
            val x = riverX(fracY) + halfWidth
            path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * dim),
                    TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.85f * dim),
                ),
            ),
        )

        ripples.forEach { ripple ->
            val life = phase01(t * ripple.cyclesPerLoop + ripple.phaseOffset)
            val fracY = phase01(ripple.travelOffset + t * 0.5f)
            val centerX = riverX(fracY)
            val centerY = fracY * h
            val radius = life * ripple.maxRadiusFrac * w
            if (radius > 0.5f) {
                val alpha = (1f - life) * ripple.baseAlpha * brightness.coerceAtMost(1.2f) * dim
                val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, ripple.colorIndex), brightness)
                val strokeWidth = (1.6f - life * 1.0f).coerceAtLeast(0.5f)
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha.coerceIn(0f, 1f)),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }
}

private data class RiverRippleSeed(
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val travelOffset: Float,
    val maxRadiusFrac: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)
