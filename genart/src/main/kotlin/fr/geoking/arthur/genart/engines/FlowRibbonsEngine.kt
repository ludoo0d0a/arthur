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
import fr.geoking.arthur.genart.flowAngle01
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * Soft ink ribbons advected along an fbm flow field. Positions are recomputed each frame from
 * seed + global phase (no mutable trail buffer).
 */
@Composable
internal fun FlowRibbonsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ribbonCount = qualityCount(quality, low = 12, medium = 24, high = 40)
    val samplesPerRibbon = qualityCount(quality, low = 18, medium = 28, high = 40)
    val ribbons = remember(ribbonCount) {
        List(ribbonCount) { i ->
            FlowRibbonSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                stepFrac = seededRange(i * 41 + 11, 0.012f, 0.028f),
                fieldScale = seededRange(i * 53 + 13, 1.6f, 3.2f),
                fieldSeed = i * 97 + 19,
                colorIndex = i,
                widthScale = seededRange(i * 67 + 23, 0.6f, 1.4f),
                phaseOffset = seededUnit(i * 79 + 31),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "flow_ribbons")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flow_ribbons_t",
    )

    val dim = if (isActive) 1f else 0.5f
    val time = phase01(t)
    val fieldDrift = loopedFbm(time, radius = 1.2f, seedOffset = 7) * 0.35f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val bgTop = TonalPalette.brightness(Color(0xFF070B14), brightness)
        val bgBottom = TonalPalette.brightness(Color(0xFF02040A), brightness)
        drawRect(
            brush = Brush.verticalGradient(listOf(bgTop, bgBottom)),
        )

        ribbons.forEach { ribbon ->
            val ink = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, ribbon.colorIndex),
                brightness,
            )
            var x = (ribbon.x0 + fieldDrift * 0.15f +
                loopedFbm(time + ribbon.phaseOffset, radius = 0.9f, seedOffset = ribbon.fieldSeed) * 0.08f)
                .let { ((it % 1f) + 1f) % 1f }
            var y = (ribbon.y0 +
                loopedFbm(time + ribbon.phaseOffset + 0.3f, radius = 1.1f, seedOffset = ribbon.fieldSeed + 5) * 0.08f)
                .let { ((it % 1f) + 1f) % 1f }

            var prevX = x * w
            var prevY = y * h
            for (s in 0 until samplesPerRibbon) {
                val nx = x * ribbon.fieldScale + fieldDrift
                val ny = y * ribbon.fieldScale
                val angle = flowAngle01(nx, ny, ribbon.fieldSeed)
                x = (x + cos(angle) * ribbon.stepFrac).let { ((it % 1f) + 1f) % 1f }
                y = (y + sin(angle) * ribbon.stepFrac).let { ((it % 1f) + 1f) % 1f }
                val px = x * w
                val py = y * h
                val fade = 1f - s / samplesPerRibbon.toFloat()
                val alpha = (0.08f + 0.55f * fade) * dim
                val strokeW = (1.1f + 2.8f * ribbon.widthScale * fade) *
                    (minDim / 480f).coerceIn(0.55f, 1.6f)
                drawLine(
                    color = TonalPalette.withAlpha(ink, alpha),
                    start = Offset(prevX, prevY),
                    end = Offset(px, py),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round,
                )
                prevX = px
                prevY = py
            }
        }
    }
}

private data class FlowRibbonSeed(
    val x0: Float,
    val y0: Float,
    val stepFrac: Float,
    val fieldScale: Float,
    val fieldSeed: Int,
    val colorIndex: Int,
    val widthScale: Float,
    val phaseOffset: Float,
)
