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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * Soft overlapping blobs / capsules with paper-cut shadows — Tapet lava-lamp picture feel.
 *
 * Tweakables via [quality]: count, shadow strength.
 * Per-blob seeds randomize shape kind (round vs capsule), aspect, rotation, palette pick.
 */
@Composable
internal fun MorphingBlobsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 6, medium = 9, high = 14)
    val shadowMul = when (quality) {
        GenartQuality.Low -> 0.6f
        GenartQuality.Medium -> 0.9f
        GenartQuality.High -> 1.15f
    }
    val blobs = remember(count) {
        List(count) { i ->
            BlobMorphSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                driftX = seededRange(i * 41 + 11, 0.02f, 0.11f),
                driftY = seededRange(i * 53 + 13, 0.015f, 0.09f),
                radiusFrac = seededRange(i * 67 + 19, 0.10f, 0.28f),
                aspect = seededRange(i * 71 + 21, 0.55f, 1.85f),
                corner = seededRange(i * 73 + 27, 0.35f, 0.55f),
                rot0 = seededRange(i * 83 + 33, 0f, 2f * PI.toFloat()),
                rotSpeed = seededRange(i * 87 + 37, -0.15f, 0.15f),
                pulseFreq = seededRange(i * 79 + 23, 0.15f, 0.55f),
                pulsePhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                alphaBase = seededRange(i * 97 + 31, 0.35f, 0.72f),
                colorIndex = (seededUnit(i * 103 + 43) * 8).toInt(),
                colorMix = seededUnit(i * 109 + 47),
                capsule = seededUnit(i * 113 + 51) > 0.42f,
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
        val bgTint = TonalPalette.mix(
            TonalPalette.pick(paletteColors, 0),
            Color(0xFF0C0A14),
            0.88f,
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(bgTint, brightness * 0.4f),
                    Color(0xFF040308),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.85f,
            ),
        )
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f
        val shadowPx = (minDim * 0.018f * shadowMul).coerceIn(5f, 28f)
        blobs.forEach { blob ->
            val x = phase01(blob.x0 + time * blob.driftX) * w
            val y = phase01(blob.y0 + time * blob.driftY) * h
            val pulse = 0.82f + 0.18f * sin(time * 2f * PI.toFloat() * blob.pulseFreq + blob.pulsePhase)
            val base = blob.radiusFrac * minDim * pulse
            val bw = if (blob.capsule) base * blob.aspect else base * 2f
            val bh = if (blob.capsule) base else base * 2f
            val cA = TonalPalette.pick(paletteColors, blob.colorIndex)
            val cB = TonalPalette.pick(paletteColors, blob.colorIndex + 2)
            val tint = TonalPalette.brightness(TonalPalette.mix(cA, cB, blob.colorMix), brightness)
            val rotDeg = Math.toDegrees(
                (blob.rot0 + time * 2f * PI.toFloat() * blob.rotSpeed).toDouble(),
            ).toFloat()
            rotate(degrees = rotDeg, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = TonalPalette.withAlpha(Color.Black, 0.32f * dim * shadowMul),
                    topLeft = Offset(x - bw * 0.5f + shadowPx * 0.55f, y - bh * 0.5f + shadowPx),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw * blob.corner, bh * blob.corner),
                )
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, blob.alphaBase * dim),
                            TonalPalette.withAlpha(tint, blob.alphaBase * 0.55f * dim),
                            TonalPalette.withAlpha(tint, blob.alphaBase * 0.12f * dim),
                        ),
                        center = Offset(x - bw * 0.12f, y - bh * 0.18f),
                        radius = maxOf(bw, bh) * 0.85f,
                    ),
                    topLeft = Offset(x - bw * 0.5f, y - bh * 0.5f),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw * blob.corner, bh * blob.corner),
                )
            }
        }
    }
}

private data class BlobMorphSeed(
    val x0: Float,
    val y0: Float,
    val driftX: Float,
    val driftY: Float,
    val radiusFrac: Float,
    val aspect: Float,
    val corner: Float,
    val rot0: Float,
    val rotSpeed: Float,
    val pulseFreq: Float,
    val pulsePhase: Float,
    val alphaBase: Float,
    val colorIndex: Int,
    val colorMix: Float,
    val capsule: Boolean,
)
