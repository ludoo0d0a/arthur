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
import androidx.compose.ui.graphics.drawscope.scale
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.min

private data class SoftTideRippleSeed(
    val originX: Float,
    val originY: Float,
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val maxRadiusFrac: Float,
    val stretchX: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)

/** Soft Ambient Soft Tide Ripples — slow elliptical rings on deep teal water. */
@Composable
internal fun SoftTideRipplesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ringCount = qualityCount(quality, low = 3, medium = 5, high = 8)
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            SoftTideRippleSeed(
                originX = seededRange(i * 131 + 5, 0.28f, 0.72f),
                originY = seededRange(i * 173 + 11, 0.42f, 0.78f),
                phaseOffset = seededUnit(i * 47 + 3),
                cyclesPerLoop = seededRange(i * 61 + 7, 0.45f, 0.85f),
                maxRadiusFrac = seededRange(i * 79 + 13, 0.18f, 0.42f),
                stretchX = seededRange(i * 89 + 19, 1.35f, 1.9f),
                baseAlpha = seededRange(i * 97 + 17, 0.12f, 0.24f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "softtideripples")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "softtideripples_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.3f, octaves = 2, seedOffset = 19)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF0C2430), brightness * dim * (0.92f + 0.08f * wash)),
                    TonalPalette.brightness(Color(0xFF061018), brightness * dim),
                    TonalPalette.brightness(Color(0xFF03080C), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFD8ECF4), TonalPalette.pick(paletteColors, 0), 0.4f),
            brightness * dim,
        )
        // Soft horizon sheen
        val horizon = h * (0.38f + 0.03f * wash)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.45f to TonalPalette.withAlpha(accent, 0.14f * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.04f,
                endY = horizon + h * 0.08f,
            ),
        )
        val minDim = min(w, h)
        rings.forEach { ring ->
            val life = phase01(t * ring.cyclesPerLoop + ring.phaseOffset)
            val center = Offset(ring.originX * w, ring.originY * h)
            val radius = life * ring.maxRadiusFrac * minDim
            if (radius <= 0.5f) return@forEach
            val alpha = (1f - life) * ring.baseAlpha * brightness.coerceAtMost(1.2f) * dim
            val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIndex), brightness)
            val strokeWidth = (2.4f - life * 1.4f).coerceAtLeast(0.6f)
            scale(scaleX = ring.stretchX, scaleY = 1f, pivot = center) {
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha.coerceIn(0f, 1f)),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(accent, alpha * 0.45f),
                    radius = radius * 0.92f,
                    center = center,
                    style = Stroke(width = strokeWidth * 0.55f),
                )
            }
        }
    }
}
