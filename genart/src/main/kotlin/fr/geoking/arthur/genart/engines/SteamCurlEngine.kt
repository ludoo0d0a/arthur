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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** A few pale wisps of steam curling upward from a fixed low point, thinning and dissipating fast. */
@Composable
internal fun SteamCurlEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val wispCount = qualityCount(quality, low = 2, medium = 3, high = 5)
    val wisps = remember(wispCount) {
        List(wispCount) { i ->
            SteamWispSeed(
                baseXFrac = 0.5f + seededRange(i * 17 + 3, -0.06f, 0.06f),
                baseYFrac = seededRange(i * 29 + 7, 0.8f, 0.9f),
                riseHeightFrac = seededRange(i * 41 + 11, 0.42f, 0.62f),
                phaseOffset = seededRange(i * 53 + 13, 0f, 1f),
                windowWidth = seededRange(i * 59 + 17, 0.3f, 0.42f),
                freq1 = seededRange(i * 67 + 19, 1.1f, 1.9f),
                freq1Speed = seededRange(i * 71 + 23, 0.4f, 0.8f),
                phase1 = seededRange(i * 73 + 29, 0f, 2f * PI.toFloat()),
                amp1Frac = seededRange(i * 79 + 31, 0.03f, 0.06f),
                freq2 = seededRange(i * 83 + 37, 2.2f, 3.4f),
                freq2Speed = seededRange(i * 89 + 41, 0.5f, 1.0f),
                phase2 = seededRange(i * 97 + 43, 0f, 2f * PI.toFloat()),
                amp2Frac = seededRange(i * 101 + 47, 0.012f, 0.028f),
                widthFrac = seededRange(i * 103 + 53, 0.01f, 0.018f),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "steamcurl")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "steamcurl_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val segments = 24
    val warmWhite = Color(0xFFFFF4E6)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF14100C), Color(0xFF0A0806)),
                ),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(4.dp)) {
            val w = size.width
            val h = size.height
            val minDim = w.coerceAtMost(h)
            val time = phase01(t)

            wisps.forEach { wisp ->
                val center = phase01(time + wisp.phaseOffset)
                val baseX = wisp.baseXFrac * w
                val baseY = wisp.baseYFrac * h
                val base = TonalPalette.mix(warmWhite, TonalPalette.pick(paletteColors, wisp.colorIndex), 0.12f)
                val tint = TonalPalette.brightness(base, brightness.coerceAtMost(1.1f))

                var prev: Offset? = null
                for (k in 0..segments) {
                    val s = k / segments.toFloat()
                    val angle1 = s * wisp.freq1 * 2f * PI.toFloat() +
                        time * 2f * PI.toFloat() * wisp.freq1Speed + wisp.phase1
                    val angle2 = s * wisp.freq2 * 2f * PI.toFloat() +
                        time * 2f * PI.toFloat() * wisp.freq2Speed + wisp.phase2
                    val wobble = (sin(angle1) * wisp.amp1Frac + sin(angle2) * wisp.amp2Frac) * s * minDim
                    val x = baseX + wobble
                    val y = baseY - s * wisp.riseHeightFrac * h
                    val point = Offset(x, y)

                    val dist = s - center
                    val windowed = dist / wisp.windowWidth
                    val envelope = (1f - windowed * windowed).coerceAtLeast(0f)
                    val fade = (1f - s).pow(1.6f)
                    val alpha = (envelope * fade * 0.55f * dim).coerceIn(0f, 1f)
                    val strokeWidth = (wisp.widthFrac * minDim * (1f - s).pow(1.3f)).coerceAtLeast(0.4f)

                    if (prev != null && alpha > 0.01f) {
                        drawLine(
                            color = TonalPalette.withAlpha(tint, alpha),
                            start = prev,
                            end = point,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                    prev = point
                }
            }
        }
    }
}

private data class SteamWispSeed(
    val baseXFrac: Float,
    val baseYFrac: Float,
    val riseHeightFrac: Float,
    val phaseOffset: Float,
    val windowWidth: Float,
    val freq1: Float,
    val freq1Speed: Float,
    val phase1: Float,
    val amp1Frac: Float,
    val freq2: Float,
    val freq2Speed: Float,
    val phase2: Float,
    val amp2Frac: Float,
    val widthFrac: Float,
    val colorIndex: Int,
)
