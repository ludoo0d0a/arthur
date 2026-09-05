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
import kotlin.math.PI
import kotlin.math.sin

/** Soft overlapping translucent blobs slowly drifting and pulsing. */
@Composable
internal fun MorphingBlobsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val blobs = remember(count) {
        List(count) { i ->
            BlobMorphSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                driftX = seededRange(i * 41 + 11, 0.03f, 0.12f),
                driftY = seededRange(i * 53 + 13, 0.02f, 0.10f),
                radiusFrac = seededRange(i * 67 + 19, 0.14f, 0.32f),
                pulseFreq = seededRange(i * 79 + 23, 0.2f, 0.6f),
                pulsePhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                alphaBase = seededRange(i * 97 + 31, 0.18f, 0.38f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "morphing_blobs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((35000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "morphing_blobs_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0C0A14), Color(0xFF040308)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.8f,
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        blobs.forEach { blob ->
            val x = phase01(blob.x0 + time * blob.driftX) * w
            val y = phase01(blob.y0 + time * blob.driftY) * h
            val pulse = 0.8f + 0.2f * sin(time * 2f * PI.toFloat() * blob.pulseFreq + blob.pulsePhase)
            val radius = blob.radiusFrac * minDim * pulse
            val tint = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, blob.colorIndex),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(tint, blob.alphaBase * dim),
                        TonalPalette.withAlpha(tint, blob.alphaBase * 0.4f * dim),
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

private data class BlobMorphSeed(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val radiusFrac: Float,
    val pulseFreq: Float,
    val pulsePhase: Float,
    val alphaBase: Float,
    val colorIndex: Int,
)
