package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.min

/** A pale blurred moon over a rippling water surface, joined by a soft vertical reflection beam. */
@Composable
internal fun MoonlightRipplesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val ringCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val originCount = 2
    val origins = remember(originCount) {
        List(originCount) { o ->
            Offset(
                x = seededRange(o * 131 + 5, 0.32f, 0.68f),
                y = seededRange(o * 173 + 11, 0.62f, 0.86f),
            )
        }
    }
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            MoonRippleSeed(
                originIndex = i % originCount,
                phaseOffset = seededUnit(i * 47 + 3),
                cyclesPerLoop = seededRange(i * 61 + 7, 2.2f, 3.4f),
                maxRadiusFrac = seededRange(i * 79 + 13, 0.14f, 0.3f),
                baseAlpha = seededRange(i * 97 + 17, 0.2f, 0.36f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "moonlight_ripples")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moonlight_ripples_t",
    )
    val glowT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((9000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "moonlight_ripples_glow",
    )

    val dim = if (isActive) 1f else 0.55f
    val moonColor = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF03060F), Color(0xFF060A16), Color(0xFF01030A)),
                ),
            )

            val minDim = min(w, h)
            val moonCx = w * 0.5f
            val moonCy = h * 0.22f
            val moonRadius = minDim * 0.09f

            drawCircle(color = Color(0xFFEFF3FA), radius = moonRadius, center = Offset(moonCx, moonCy))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(Color.White, 0.95f * dim), Color.Transparent),
                    center = Offset(moonCx - moonRadius * 0.35f, moonCy - moonRadius * 0.35f),
                    radius = moonRadius * 1.25f,
                ),
                radius = moonRadius,
                center = Offset(moonCx, moonCy),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, TonalPalette.withAlpha(Color(0xFF1A2436), 0.55f * dim)),
                    center = Offset(moonCx + moonRadius * 0.45f, moonCy + moonRadius * 0.45f),
                    radius = moonRadius * 1.3f,
                ),
                radius = moonRadius,
                center = Offset(moonCx, moonCy),
            )

            rings.forEach { ring ->
                val life = phase01(t * ring.cyclesPerLoop + ring.phaseOffset)
                val origin = origins[ring.originIndex]
                val center = Offset(origin.x * w, origin.y * h)
                val radius = life * ring.maxRadiusFrac * minDim
                if (radius > 0.5f) {
                    val alpha = (1f - life) * ring.baseAlpha * brightness.coerceAtMost(1.2f) * dim
                    val color = TonalPalette.brightness(TonalPalette.pick(paletteColors, ring.colorIndex), brightness)
                    val strokeWidth = (2.4f - life * 1.4f).coerceAtLeast(0.6f)
                    drawCircle(
                        color = TonalPalette.withAlpha(color, alpha.coerceIn(0f, 1f)),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth),
                    )
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(20.dp)) {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)
            val moonCx = w * 0.5f
            val moonCy = h * 0.22f
            val moonRadius = minDim * 0.09f
            val waterTop = h * 0.58f
            val pulse = 0.85f + 0.15f * sin01(phase01(glowT) * 2f * PI.toFloat())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(moonColor, 0.4f * dim * pulse), Color.Transparent),
                    center = Offset(moonCx, moonCy),
                    radius = moonRadius * 3.4f,
                ),
                radius = moonRadius * 3.4f,
                center = Offset(moonCx, moonCy),
            )

            val beamWidth = minDim * 0.05f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        TonalPalette.withAlpha(Color.White, 0.22f * dim * pulse),
                        Color.Transparent,
                    ),
                    startX = moonCx - beamWidth,
                    endX = moonCx + beamWidth,
                ),
                topLeft = Offset(moonCx - beamWidth, moonCy + moonRadius * 0.6f),
                size = Size(beamWidth * 2f, waterTop - (moonCy + moonRadius * 0.6f)),
            )
        }
    }
}

private data class MoonRippleSeed(
    val originIndex: Int,
    val phaseOffset: Float,
    val cyclesPerLoop: Float,
    val maxRadiusFrac: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)
