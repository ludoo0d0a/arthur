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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Narrow, fast falling water column between rocky banks with a rising mist cloud at the base. */
@Composable
internal fun WaterfallMistEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val streakCount = qualityCount(quality, low = 48, medium = 80, high = 120)
    val streaks = remember(streakCount) {
        List(streakCount) { i ->
            FallSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                fallSpeed = seededRange(i * 41 + 11, 0.95f, 1.9f),
                lengthFrac = seededRange(i * 53 + 13, 0.03f, 0.08f),
                thickness = seededRange(i * 67 + 19, 1f, 2.2f),
                driftAmp = seededRange(i * 79 + 23, 0.002f, 0.008f),
                driftFreq = seededRange(i * 89 + 29, 0.3f, 1.2f),
                alphaBase = seededRange(i * 97 + 31, 0.22f, 0.6f),
                colorIndex = i,
            )
        }
    }
    val mistCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val mistBlobs = remember(mistCount) {
        List(mistCount) { i ->
            MistSeed(
                x0 = seededRange(i * 31 + 5, 0.38f, 0.62f),
                riseSpeed = seededRange(i * 43 + 9, 0.05f, 0.14f),
                startDelay = seededUnit(i * 59 + 15),
                widthFrac = seededRange(i * 71 + 21, 0.16f, 0.34f),
                heightFrac = seededRange(i * 83 + 27, 0.1f, 0.22f),
                swayAmp = seededRange(i * 101 + 33, 0.01f, 0.03f),
                swayFreq = seededRange(i * 113 + 39, 0.2f, 0.7f),
                alphaBase = seededRange(i * 127 + 45, 0.1f, 0.26f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "waterfallmist")
    val fallT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((7000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waterfallmist_fall",
    )
    val mistT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((16000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waterfallmist_mist",
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dim = if (isActive) 1f else 0.55f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF10161C), Color(0xFF05080B)),
                ),
            )
            val rockWidth = w * 0.22f
            val rockColor = TonalPalette.brightness(Color(0xFF23201D), brightness * dim)
            drawRect(
                color = rockColor,
                topLeft = Offset(0f, 0f),
                size = Size(rockWidth, h),
            )
            drawRect(
                color = rockColor,
                topLeft = Offset(w - rockWidth, 0f),
                size = Size(rockWidth, h),
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, TonalPalette.withAlpha(Color.Black, 0.35f)),
                ),
                topLeft = Offset(rockWidth, 0f),
                size = Size(w * 0.08f, h),
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(TonalPalette.withAlpha(Color.Black, 0.35f), Color.Transparent),
                ),
                topLeft = Offset(w - rockWidth - w * 0.08f, 0f),
                size = Size(w * 0.08f, h),
            )

            val time = phase01(fallT)
            val bandLeft = w * 0.38f
            val bandWidth = w * 0.24f
            streaks.forEach { s ->
                val fall = phase01(s.y0 + s.fallSpeed * time)
                val y = fall * h
                val sway = sin(time * 2f * PI.toFloat() * s.driftFreq) * s.driftAmp
                val x = bandLeft + phase01(s.x0 + sway) * bandWidth
                val len = s.lengthFrac * h
                val start = Offset(x, y)
                val end = Offset(x, y + len)
                val base = TonalPalette.mix(Color(0xFFDCEEFF), TonalPalette.pick(paletteColors, s.colorIndex), 0.15f)
                val color = TonalPalette.brightness(base, brightness)
                drawLine(
                    color = TonalPalette.withAlpha(color, s.alphaBase * dim),
                    start = start,
                    end = end,
                    strokeWidth = s.thickness,
                    cap = StrokeCap.Round,
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            val w = size.width
            val h = size.height
            val dim = if (isActive) 1f else 0.6f
            val time = phase01(mistT)
            mistBlobs.forEach { blob ->
                val rise = phase01(time + blob.startDelay)
                val y = h * (0.92f - rise * blob.riseSpeed * 6f)
                if (y < -h * 0.2f) return@forEach
                val fade = (1f - rise).coerceIn(0f, 1f)
                val sway = sin(time * 2f * PI.toFloat() * blob.swayFreq) * blob.swayAmp
                val x = phase01(blob.x0 + sway) * w
                val rw = blob.widthFrac * w * (0.7f + rise * 0.6f)
                val rh = blob.heightFrac * h * (0.7f + rise * 0.6f)
                val base = TonalPalette.mix(Color(0xFFE8F2FF), TonalPalette.pick(paletteColors, blob.colorIndex), 0.1f)
                val tint = TonalPalette.brightness(base, brightness * 0.95f)
                val alpha = blob.alphaBase * fade * dim
                val radius = maxOf(rw, rh) * 0.6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, alpha),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius.coerceAtLeast(1f),
                    ),
                    radius = radius.coerceAtLeast(1f),
                    center = Offset(x, y),
                )
            }
        }
    }
}

private data class FallSeed(
    val x0: Float,
    val y0: Float,
    val fallSpeed: Float,
    val lengthFrac: Float,
    val thickness: Float,
    val driftAmp: Float,
    val driftFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)

private data class MistSeed(
    val x0: Float,
    val riseSpeed: Float,
    val startDelay: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
