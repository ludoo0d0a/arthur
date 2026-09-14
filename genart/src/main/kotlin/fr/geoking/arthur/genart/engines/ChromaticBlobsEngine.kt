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
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Dynamic merging liquid color blobs with contrasting ambient shadows. */
@Composable
internal fun ChromaticBlobsEngine(
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
            BlobSpec(
                x0 = seededUnit(i * 19 + 7),
                y0 = seededUnit(i * 31 + 11),
                rx = seededRange(i * 43 + 17, 0.15f, 0.35f),
                ry = seededRange(i * 53 + 23, 0.15f, 0.35f),
                freqX = seededRange(i * 67 + 29, 0.5f, 1.8f),
                freqY = seededRange(i * 79 + 31, 0.5f, 1.8f),
                phaseX = seededRange(i * 89 + 37, 0f, 2f * PI.toFloat()),
                phaseY = seededRange(i * 97 + 41, 0f, 2f * PI.toFloat()),
                colorIdx = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "chromatic_blobs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chromatic_blobs_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF07040B))

        val dim = if (isActive) 1f else 0.55f
        val time = phase01(t)

        // Draw Blob Shadow Base
        blobs.forEach { blob ->
            val offsetX = (sin01(time * 2f * PI.toFloat() * blob.freqX + blob.phaseX) - 0.5f) * blob.rx * w
            val offsetY = (sin01(time * 2f * PI.toFloat() * blob.freqY + blob.phaseY) - 0.5f) * blob.ry * h
            val x = blob.x0 * w + offsetX
            val y = blob.y0 * h + offsetY
            val r = (blob.rx + blob.ry) * 0.5f * minDim

            val shadowCenter = Offset(x + r * 0.15f, y + r * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f * dim),
                        Color.Transparent,
                    ),
                    center = shadowCenter,
                    radius = r * 1.4f
                ),
                radius = r * 1.4f,
                center = shadowCenter
            )
        }

        // Draw Chromatic Liquid Blobs
        blobs.forEach { blob ->
            val offsetX = (sin01(time * 2f * PI.toFloat() * blob.freqX + blob.phaseX) - 0.5f) * blob.rx * w
            val offsetY = (sin01(time * 2f * PI.toFloat() * blob.freqY + blob.phaseY) - 0.5f) * blob.ry * h
            val x = blob.x0 * w + offsetX
            val y = blob.y0 * h + offsetY
            val r = (blob.rx + blob.ry) * 0.5f * minDim

            val colorA = TonalPalette.brightness(TonalPalette.pick(paletteColors, blob.colorIdx), brightness)
            val colorB = TonalPalette.brightness(TonalPalette.pick(paletteColors, blob.colorIdx + 2), brightness)
            val morph = sin01(time * 2f * PI.toFloat() + blob.phaseX)
            val color = TonalPalette.mix(colorA, colorB, morph)

            val center = Offset(x, y)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(color, 0.85f * dim),
                        TonalPalette.withAlpha(color, 0.45f * dim),
                        TonalPalette.withAlpha(color, 0.1f * dim),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }
    }
}

private data class BlobSpec(
    val x0: Float,
    val y0: Float,
    val rx: Float,
    val ry: Float,
    val freqX: Float,
    val freqY: Float,
    val phaseX: Float,
    val phaseY: Float,
    val colorIdx: Int,
)
