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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit

/**
 * A handful of large, slow-breathing color regions driven by looping fbm noise — fewer and
 * bigger than Soft Noise Field's many small wobbling blobs, reading as a genuinely large-scale,
 * low-frequency field rather than a busier variant.
 */
@Composable
internal fun LowFreqNoiseFieldEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 3, medium = 5, high = 8)
    val cells = remember(count) {
        List(count) { i ->
            LowFreqCell(
                x0 = seededUnit(i * 13 + 1),
                y0 = seededUnit(i * 19 + 5),
                driftAmp = seededRange(i * 31 + 7, 0.03f, 0.09f),
                radiusFrac = seededRange(i * 59 + 17, 0.34f, 0.68f),
                alphaBase = seededRange(i * 97 + 31, 0.07f, 0.16f),
                seedOffset = i * 941 + 53,
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "lowfreq_noise_field")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((95000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lowfreq_noise_field_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF07060C))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        cells.forEach { cell ->
            val driftX = (loopedFbm(time, radius = 1.3f, seedOffset = cell.seedOffset) * 2f - 1f) * cell.driftAmp
            val driftY = (loopedFbm(time, radius = 1.7f, seedOffset = cell.seedOffset + 1) * 2f - 1f) * cell.driftAmp
            val breathe = 0.82f + 0.18f * loopedFbm(time, radius = 1.1f, seedOffset = cell.seedOffset + 2)
            val x = phase01(cell.x0 + driftX) * w
            val y = phase01(cell.y0 + driftY) * h
            val radius = cell.radiusFrac * minDim * breathe
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, cell.colorIndex),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, cell.alphaBase * dim),
                        TonalPalette.withAlpha(tint, cell.alphaBase * 0.3f * dim),
                        Color.Transparent,
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

private data class LowFreqCell(
    val x0: Float,
    val y0: Float,
    val driftAmp: Float,
    val radiusFrac: Float,
    val alphaBase: Float,
    val seedOffset: Int,
    val colorIndex: Int,
)
