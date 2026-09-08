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

/** Still lake beneath a soft sky, disturbed by only a few gentle ripples. */
@Composable
internal fun LakeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ringCount = qualityCount(quality, low = 2, medium = 3, high = 5)
    val originCount = 2
    val origins = remember(originCount) {
        List(originCount) { o ->
            Offset(
                x = seededRange(o * 131 + 5, 0.22f, 0.78f),
                y = seededRange(o * 173 + 11, 0.6f, 0.92f),
            )
        }
    }
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            LakeRippleSeed(
                originIndex = i % originCount,
                phaseOffset = seededUnit(i * 47 + 3),
                cyclesPerLoop = seededRange(i * 61 + 7, 0.6f, 1.1f),
                maxRadiusFrac = seededRange(i * 79 + 13, 0.1f, 0.22f),
                baseAlpha = seededRange(i * 97 + 17, 0.14f, 0.26f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "lake")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lake_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.5f

        val skyTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.45f)
        val skyMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.28f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid),
                startY = 0f,
                endY = horizon,
            ),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, horizon),
        )

        val waterTop = TonalPalette.brightness(TonalPalette.mix(skyMid, Color(0xFF03080C), 0.35f), 0.9f)
        val waterBottom = Color(0xFF010204)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(waterTop, waterBottom),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = androidx.compose.ui.geometry.Size(w, h - horizon),
        )

        val minDim = min(w, h)
        val dimming = if (isActive) 1f else 0.5f
        rings.forEach { ring ->
            val life = phase01(t * ring.cyclesPerLoop + ring.phaseOffset)
            val origin = origins[ring.originIndex]
            val center = Offset(origin.x * w, horizon + origin.y * (h - horizon))
            val radius = life * ring.maxRadiusFrac * minDim
            if (radius > 0.5f) {
                val alpha = (1f - life) * ring.baseAlpha * brightness.coerceAtMost(1.2f) * dimming
                val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIndex), brightness)
                val strokeWidth = (2.2f - life * 1.2f).coerceAtLeast(0.5f)
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

private data class LakeRippleSeed(
    val originIndex: Int,
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val maxRadiusFrac: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)
