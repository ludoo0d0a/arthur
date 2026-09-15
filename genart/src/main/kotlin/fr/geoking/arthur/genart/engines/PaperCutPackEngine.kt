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
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Dense paper-cut pack of capsules / soft blobs — Tapet packed-shape wallpaper picture.
 *
 * Algorithm: seeded scatter + z-order stack with drop shadows and palette mixing.
 * Tweakables via [quality]: piece count, shadow depth, gutter (via size range).
 */
@Composable
internal fun PaperCutPackEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 14, medium = 22, high = 32)
    val shadowMul = when (quality) {
        GenartQuality.Low -> 0.65f
        GenartQuality.Medium -> 0.95f
        GenartQuality.High -> 1.2f
    }
    val pieces = remember(count) {
        List(count) { i ->
            PaperCutPiece(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                wFrac = seededRange(i * 41 + 11, 0.12f, 0.42f),
                hFrac = seededRange(i * 53 + 13, 0.08f, 0.28f),
                rot = seededRange(i * 67 + 19, -0.55f, 0.55f) * PI.toFloat(),
                corner = seededRange(i * 79 + 23, 0.38f, 0.55f),
                colorIndex = (seededUnit(i * 89 + 29) * 8).toInt(),
                colorMix = seededUnit(i * 97 + 31),
                breathPhase = seededRange(i * 103 + 37, 0f, 2f * PI.toFloat()),
                breathAmp = seededRange(i * 107 + 41, 0.02f, 0.07f),
                z = seededUnit(i * 109 + 43),
                alpha = seededRange(i * 113 + 47, 0.72f, 0.98f),
            )
        }.sortedBy { it.z }
    }
    val transition = rememberInfiniteTransition(label = "paper_cut_pack")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "paper_cut_pack_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val gapTint = TonalPalette.mix(
            TonalPalette.pick(paletteColors, 3),
            Color(0xFF1A0A10),
            0.65f,
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(gapTint, brightness * 0.35f),
                    Color(0xFF0A0408),
                ),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.9f,
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val shadowPx = (minDim * 0.014f * shadowMul).coerceIn(4f, 22f)
        pieces.forEach { piece ->
            val breath = 1f + piece.breathAmp * (sin01(time + piece.breathPhase) * 2f - 1f)
            val cx = piece.x * w
            val cy = piece.y * h
            val bw = piece.wFrac * minDim * breath
            val bh = piece.hFrac * minDim * breath
            val cA = TonalPalette.pick(paletteColors, piece.colorIndex)
            val cB = TonalPalette.pick(paletteColors, piece.colorIndex + 1)
            val tint = TonalPalette.brightness(TonalPalette.mix(cA, cB, piece.colorMix), brightness)
            val rotDeg = Math.toDegrees(piece.rot.toDouble()).toFloat()
            rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    color = TonalPalette.withAlpha(Color.Black, 0.38f * dim * shadowMul),
                    topLeft = Offset(cx - bw * 0.5f + shadowPx * 0.45f, cy - bh * 0.5f + shadowPx),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw * piece.corner, bh * piece.corner),
                )
                // Soft highlight bias toward top-left for paper bevel.
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(
                                TonalPalette.mix(tint, Color.White, 0.12f),
                                piece.alpha * dim,
                            ),
                            TonalPalette.withAlpha(tint, piece.alpha * dim),
                            TonalPalette.withAlpha(
                                TonalPalette.mix(tint, Color.Black, 0.18f),
                                piece.alpha * dim,
                            ),
                        ),
                        start = Offset(cx - bw * 0.4f, cy - bh * 0.45f),
                        end = Offset(cx + bw * 0.35f, cy + bh * 0.45f),
                    ),
                    topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.5f),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw * piece.corner, bh * piece.corner),
                )
            }
        }
    }
}

private data class PaperCutPiece(
    val x: Float,
    val y: Float,
    val wFrac: Float,
    val hFrac: Float,
    val rot: Float,
    val corner: Float,
    val colorIndex: Int,
    val colorMix: Float,
    val breathPhase: Float,
    val breathAmp: Float,
    val z: Float,
    val alpha: Float,
)
