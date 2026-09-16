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
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Diagonal rhombus / diamond weave — Tapet Havana-style geometric wallpaper picture.
 *
 * Seeded per-tile color + subtle shade split; [quality] tweaks grid density and shadow.
 */
@Composable
internal fun DiamondWeaveEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cols = qualityCount(quality, low = 5, medium = 7, high = 9)
    val rows = qualityCount(quality, low = 8, medium = 11, high = 14)
    val shadowMul = when (quality) {
        GenartQuality.Low -> 0.5f
        GenartQuality.Medium -> 0.8f
        GenartQuality.High -> 1.05f
    }
    val tiles = remember(cols, rows) {
        List(cols * rows) { i ->
            DiamondTile(
                colorIndex = (seededUnit(i * 17 + 3) * 8).toInt(),
                colorMix = seededUnit(i * 29 + 7),
                shadeSkew = seededRange(i * 41 + 11, 0.08f, 0.28f),
                pulsePhase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                pulseAmp = seededRange(i * 67 + 19, 0.0f, 0.06f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "diamond_weave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "diamond_weave_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bg = TonalPalette.mix(TonalPalette.pick(paletteColors, 4), Color(0xFF121018), 0.75f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(bg, brightness * 0.4f),
                    Color(0xFF08060C),
                ),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        // Diamond cell size — stagger odd rows for weave.
        val cellW = w / (cols - 0.5f).coerceAtLeast(1f)
        val cellH = h / (rows - 0.5f).coerceAtLeast(1f)
        val rx = cellW * 0.52f
        val ry = cellH * 0.52f
        val gap = minOf(rx, ry) * 0.04f
        tiles.forEachIndexed { index, tile ->
            val col = index % cols
            val row = index / cols
            val stagger = if (row % 2 == 0) 0f else cellW * 0.5f
            val cx = col * cellW + stagger
            val cy = row * cellH * 0.55f + cellH * 0.2f
            val pulse = 1f + tile.pulseAmp * (sin01(time + tile.pulsePhase) * 2f - 1f)
            val halfX = (rx - gap) * pulse
            val halfY = (ry - gap) * pulse
            val cA = TonalPalette.pick(paletteColors, tile.colorIndex)
            val cB = TonalPalette.pick(paletteColors, tile.colorIndex + 2)
            val base = TonalPalette.brightness(TonalPalette.mix(cA, cB, tile.colorMix), brightness)
            val light = TonalPalette.mix(base, Color.White, tile.shadeSkew)
            val dark = TonalPalette.mix(base, Color.Black, tile.shadeSkew * 1.35f)
            // Soft drop under diamond.
            val shadow = Path().apply {
                moveTo(cx, cy - halfY + 4f * shadowMul)
                lineTo(cx + halfX + 3f * shadowMul, cy + 4f * shadowMul)
                lineTo(cx, cy + halfY + 6f * shadowMul)
                lineTo(cx - halfX + 1f * shadowMul, cy + 4f * shadowMul)
                close()
            }
            drawPath(
                path = shadow,
                color = TonalPalette.withAlpha(Color.Black, 0.28f * dim * shadowMul),
            )
            // Split-shade diamond (folded paper look).
            val left = Path().apply {
                moveTo(cx, cy - halfY)
                lineTo(cx - halfX, cy)
                lineTo(cx, cy + halfY)
                close()
            }
            val right = Path().apply {
                moveTo(cx, cy - halfY)
                lineTo(cx + halfX, cy)
                lineTo(cx, cy + halfY)
                close()
            }
            drawPath(path = left, color = TonalPalette.withAlpha(light, 0.92f * dim))
            drawPath(path = right, color = TonalPalette.withAlpha(dark, 0.92f * dim))
        }
    }
}

private data class DiamondTile(
    val colorIndex: Int,
    val colorMix: Float,
    val shadeSkew: Float,
    val pulsePhase: Float,
    val pulseAmp: Float,
)
