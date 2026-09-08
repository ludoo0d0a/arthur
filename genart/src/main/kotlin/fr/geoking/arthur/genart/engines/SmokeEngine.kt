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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Soft translucent wisps drifting up from the bottom, widening and dispersing as they rise. */
@Composable
internal fun SmokeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val wispCount = qualityCount(quality, low = 3, medium = 5, high = 8)
    val wisps = remember(wispCount) {
        List(wispCount) { i ->
            SmokeWispSeed(
                x0 = seededUnit(i * 17 + 3),
                startOffset = seededUnit(i * 29 + 7),
                riseSpeedFactor = seededRange(i * 41 + 11, 0.55f, 1.25f),
                radiusBaseFrac = seededRange(i * 53 + 13, 0.05f, 0.11f),
                swayFreq = seededRange(i * 67 + 19, 0.3f, 0.9f),
                swayPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                swayAmpFrac = seededRange(i * 89 + 29, 0.08f, 0.2f),
                alphaBase = seededRange(i * 101 + 31, 0.14f, 0.3f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "smoke")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "smoke_t",
    )

    val driftScale = if (isActive) 1f else 0.4f
    val alphaScale = if (isActive) 1f else 0.6f
    val grey = Color(0xFFC7C9CE)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val top = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.18f)
            val bottom = Color(0xFF07080A)
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(top, bottom)),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(16.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val time = phase01(t) * (2f * PI.toFloat())

            wisps.forEach { wisp ->
                val heightFrac = phase01(t * wisp.riseSpeedFactor * driftScale + wisp.startOffset)
                val sway = sin(time * wisp.swayFreq + wisp.swayPhase) * wisp.swayAmpFrac * heightFrac * w
                val x = wisp.x0 * w + sway
                val y = (1f - heightFrac) * h
                val radius = wisp.radiusBaseFrac * minDim * (1f + heightFrac * 2.4f)
                val fadeIn = (heightFrac / 0.1f).coerceIn(0f, 1f)
                val alpha = (wisp.alphaBase * (1f - heightFrac) * fadeIn * alphaScale).coerceIn(0f, 1f)
                val tint = TonalPalette.brightness(
                    TonalPalette.mix(grey, TonalPalette.pick(paletteColors, wisp.colorIndex), 0.25f),
                    brightness.coerceAtLeast(0f),
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, alpha),
                            TonalPalette.withAlpha(tint, 0f),
                        ),
                        center = Offset(x, y),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private data class SmokeWispSeed(
    val x0: Float,
    val startOffset: Float,
    val riseSpeedFactor: Float,
    val radiusBaseFrac: Float,
    val swayFreq: Float,
    val swayPhase: Float,
    val swayAmpFrac: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
