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
import kotlin.math.min

@Composable
internal fun PondRipplesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ringCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val originCount = 3
    val origins = remember(originCount) {
        List(originCount) { o ->
            Offset(
                x = seededRange(o * 131 + 5, 0.18f, 0.82f),
                y = seededRange(o * 173 + 11, 0.22f, 0.78f),
            )
        }
    }
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            RippleSeed(
                originIndex = i % originCount,
                phaseOffset = seededUnit(i * 47 + 3),
                cyclesPerLoop = seededRange(i * 61 + 7, 2.4f, 3.6f),
                maxRadiusFrac = seededRange(i * 79 + 13, 0.16f, 0.34f),
                baseAlpha = seededRange(i * 97 + 17, 0.22f, 0.4f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "pond_ripples")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pond_ripples_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF010204), Color(0xFF04141C)),
            ),
        )
        val streakCount = if (isActive) 4 else 2
        for (s in 0 until streakCount) {
            val fy = (s + 1f) / (streakCount + 1f)
            val y = h * fy
            drawRect(
                color = TonalPalette.withAlpha(Color.White, 0.02f * brightness.coerceAtMost(1f)),
                topLeft = Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(w, 1.4f),
            )
        }
        val minDim = min(w, h)
        val dimming = if (isActive) 1f else 0.55f
        rings.forEach { ring ->
            val life = phase01(t * ring.cyclesPerLoop + ring.phaseOffset)
            val origin = origins[ring.originIndex]
            val center = Offset(origin.x * w, origin.y * h)
            val radius = life * ring.maxRadiusFrac * minDim
            if (radius > 0.5f) {
                val alpha = (1f - life) * ring.baseAlpha * brightness.coerceAtMost(1.2f) * dimming
                val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIndex), brightness)
                val strokeWidth = (2.6f - life * 1.6f).coerceAtLeast(0.6f)
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha.coerceIn(0f, 1f)),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }
}

private data class RippleSeed(
    val originIndex: Int,
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val maxRadiusFrac: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)
