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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Warm ground-level gradient with slow, sine-warped shimmer bands — desert-road heat haze. */
@Composable
internal fun HeatHazeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val bandCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val bands = remember(bandCount) {
        List(bandCount) { i ->
            val depthFrac = if (bandCount > 1) i / (bandCount - 1f) else 0.5f
            HeatBandSeed(
                depthFrac = depthFrac,
                yFrac = 0.42f + depthFrac * 0.5f + seededRange(i * 13 + 5, -0.02f, 0.02f),
                thicknessFrac = seededRange(i * 17 + 7, 0.02f, 0.045f),
                widthFrac = seededRange(i * 19 + 11, 0.82f, 0.97f),
                shimmerAmpFrac = seededRange(i * 23 + 13, 0.01f, 0.035f),
                shimmerFreq = seededRange(i * 29 + 17, 1.4f, 3.2f),
                shimmerPhase = seededRange(i * 31 + 19, 0f, 2f * PI.toFloat()),
                driftSpeed = seededRange(i * 37 + 23, 0.03f, 0.08f),
                alphaBase = seededRange(i * 41 + 29, 0.04f, 0.12f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "heathaze")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((30000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heathaze_t",
    )

    val shimmerScale = if (isActive) 1f else 0.3f
    val alphaScale = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val groundTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 0.9f)
        val groundMid = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.65f)
        val groundLow = TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness * 0.4f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundTop, groundMid, groundLow),
            ),
        )

        bands.forEach { band ->
            val drift = phase01(t * band.driftSpeed) * 2f * PI.toFloat()
            val shimmer = sin(t * band.shimmerFreq * 2f * PI.toFloat() + band.shimmerPhase) *
                band.shimmerAmpFrac * shimmerScale
            val bandWidth = band.widthFrac * w
            val centerX = w * 0.5f + (shimmer + sin(drift) * 0.006f) * w
            val y = band.yFrac * h
            val thickness = band.thicknessFrac * h
            val base = TonalPalette.pick(paletteColors, band.colorIndex)
            val tinted = TonalPalette.brightness(base, brightness * (0.7f + band.depthFrac * 0.3f))
            val alpha = (band.alphaBase * alphaScale).coerceIn(0f, 0.14f)
            drawRect(
                color = TonalPalette.withAlpha(tinted, alpha),
                topLeft = Offset(centerX - bandWidth / 2f, y - thickness / 2f),
                size = Size(bandWidth, thickness),
            )
        }
    }
}

private data class HeatBandSeed(
    val depthFrac: Float,
    val yFrac: Float,
    val thicknessFrac: Float,
    val widthFrac: Float,
    val shimmerAmpFrac: Float,
    val shimmerFreq: Float,
    val shimmerPhase: Float,
    val driftSpeed: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
