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
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Dynamic morphing blobs with color gradients and soft ambient shadow layers. */
@Composable
internal fun ChromaticBlobsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 7, high = 10)
    val blobs = remember(count) {
        List(count) { i ->
            BlobNode(
                xA = seededUnit(i * 19 + 3),
                yA = seededUnit(i * 31 + 7),
                xB = seededUnit(i * 43 + 11),
                yB = seededUnit(i * 59 + 13),
                radiusFrac = seededRange(i * 71 + 17, 0.25f, 0.5f),
                shadowOffsetFrac = seededRange(i * 83 + 23, 0.03f, 0.08f),
                phaseOffset = seededRange(i * 97 + 29, 0f, 2f * PI.toFloat()),
                alphaBase = seededRange(i * 109 + 31, 0.35f, 0.6f),
                colorIdxA = i,
                colorIdxB = i + 3,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "chromatic_blobs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chromatic_blobs_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = Color(0xFF08060D))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        blobs.forEach { blob ->
            val morph = sin01(time * 2f * PI.toFloat() + blob.phaseOffset)
            val cx = lerp(blob.xA, blob.xB, morph) * w
            val cy = lerp(blob.yA, blob.yB, morph) * h
            val radius = blob.radiusFrac * minDim * (0.85f + 0.3f * morph)
            val shadowOffset = blob.shadowOffsetFrac * minDim

            // Draw soft shadow layer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f * blob.alphaBase * dim),
                        Color.Black.copy(alpha = 0.15f * blob.alphaBase * dim),
                        Color.Transparent,
                    ),
                    center = Offset(cx + shadowOffset, cy + shadowOffset),
                    radius = radius * 1.15f,
                ),
                radius = radius * 1.15f,
                center = Offset(cx + shadowOffset, cy + shadowOffset),
            )

            // Draw chromatic gradient blob
            val cA = TonalPalette.brightness(TonalPalette.pick(paletteColors, blob.colorIdxA), brightness)
            val cB = TonalPalette.brightness(TonalPalette.pick(paletteColors, blob.colorIdxB), brightness)
            val colorTint = TonalPalette.mix(cA, cB, morph)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(colorTint, blob.alphaBase * dim),
                        TonalPalette.withAlpha(colorTint, blob.alphaBase * 0.5f * dim),
                        TonalPalette.withAlpha(colorTint, 0.08f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}

private data class BlobNode(
    val xA: Float,
    val yA: Float,
    val xB: Float,
    val yB: Float,
    val radiusFrac: Float,
    val shadowOffsetFrac: Float,
    val phaseOffset: Float,
    val alphaBase: Float,
    val colorIdxA: Int,
    val colorIdxB: Int,
)
