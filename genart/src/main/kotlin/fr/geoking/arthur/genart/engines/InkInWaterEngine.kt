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
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit

/** Slow-blooming ink drops diffusing into calm water. */
@Composable
internal fun InkInWaterEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 4)
    val blobs = remember(count) {
        List(count) { i ->
            InkBlobSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                phaseOffset = seededUnit(i * 41 + 11),
                cycleFrac = seededRange(i * 53 + 13, 0.7f, 1.3f),
                maxRadiusFrac = seededRange(i * 67 + 19, 0.22f, 0.42f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "ink_in_water")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((60000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ink_in_water_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val waterTop = TonalPalette.brightness(Color(0xFF0A1220), brightness)
        val waterBottom = TonalPalette.brightness(Color(0xFF03050C), brightness)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(waterTop, waterBottom),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.85f,
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.5f
        blobs.forEach { blob ->
            val bloom = phase01(time * blob.cycleFrac + blob.phaseOffset)
            val radius = bloom * blob.maxRadiusFrac * minDim
            val alpha = (1f - bloom).coerceIn(0f, 1f)
            val x = blob.x0 * w
            val y = blob.y0 * h
            val ink = TonalPalette.mix(
                TonalPalette.pick(paletteColors, blob.colorIndex),
                Color(0xFF05030F),
                0.65f,
            )
            val core = TonalPalette.brightness(ink, brightness * 0.8f)
            val edge = TonalPalette.brightness(
                TonalPalette.mix(ink, Color(0xFF2A2050), 0.5f),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(core, alpha * 0.55f * dim),
                        TonalPalette.withAlpha(edge, alpha * 0.28f * dim),
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

private data class InkBlobSeed(
    val x0: Float,
    val y0: Float,
    val phaseOffset: Float,
    val cycleFrac: Float,
    val maxRadiusFrac: Float,
    val colorIndex: Int,
)
