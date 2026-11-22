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
import kotlin.math.PI
import kotlin.math.sin

private data class HarborMistBlob(
    val x0: Float,
    val y0: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val driftX: Float,
    val swayAmp: Float,
    val phaseOffset: Float,
    val baseAlpha: Float,
    val colorIndex: Int,
)

/** Soft Ambient Harbor Mist — calm water under drifting mist sheets. */
@Composable
internal fun HarborMistEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val mistCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val mist = remember(mistCount) {
        List(mistCount) { i ->
            HarborMistBlob(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededRange(i * 29 + 7, 0.28f, 0.72f),
                widthFrac = seededRange(i * 41 + 11, 0.22f, 0.48f),
                heightFrac = seededRange(i * 53 + 13, 0.08f, 0.2f),
                driftX = seededRange(i * 67 + 19, 0.04f, 0.12f),
                swayAmp = seededRange(i * 79 + 23, 0.01f, 0.035f),
                phaseOffset = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                baseAlpha = seededRange(i * 97 + 31, 0.08f, 0.2f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "harbormist")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((56000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "harbormist_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.4f, octaves = 2, seedOffset = 7)
        val waterTop = TonalPalette.brightness(Color(0xFF143040), brightness * dim * (0.9f + 0.1f * wash))
        val waterMid = TonalPalette.brightness(Color(0xFF0A1C28), brightness * dim)
        val waterBottom = TonalPalette.brightness(Color(0xFF040A10), brightness * dim)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(waterTop, waterMid, waterBottom),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE4F0F6), TonalPalette.pick(paletteColors, 1), 0.3f),
            brightness * dim,
        )
        // Quiet specular band near mid-water
        val bandY = h * (0.55f + 0.03f * wash)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to TonalPalette.withAlpha(accent, 0.1f * dim),
                1f to Color.Transparent,
                startY = bandY - h * 0.03f,
                endY = bandY + h * 0.05f,
            ),
        )
        mist.forEach { blob ->
            val n = loopedFbm(time, radius = 1.2f + blob.widthFrac, seedOffset = blob.colorIndex * 73 + 11)
            val x = (blob.x0 + blob.driftX * (n * 2f - 1f) + 1f) % 1f * w
            val y = (blob.y0 + blob.swayAmp * sin(time * 2f * PI.toFloat() + blob.phaseOffset) + 1f) % 1f * h
            val rw = w * blob.widthFrac * (0.9f + 0.2f * n)
            val rh = h * blob.heightFrac * (0.85f + 0.25f * n)
            val col = TonalPalette.mix(
                Color(0xFFC8D8E4),
                TonalPalette.pick(paletteColors, blob.colorIndex),
                0.25f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(col, blob.baseAlpha * brightness.coerceAtMost(1.2f) * dim),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = maxOf(rw, rh),
                ),
                radius = maxOf(rw, rh),
                center = Offset(x, y),
            )
        }
    }
}
