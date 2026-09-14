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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Glassmorphic floating tile grid with drop shadows and internal gradient fills. */
@Composable
internal fun GlassTilesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cols = qualityCount(quality, low = 3, medium = 4, high = 5)
    val rows = qualityCount(quality, low = 4, medium = 6, high = 7)
    val tileCount = cols * rows

    val tileSpecs = remember(cols, rows) {
        List(tileCount) { i ->
            GlassTileSpec(
                colorIdx = i,
                phaseOffset = seededRange(i * 17 + 5, 0f, 2f * PI.toFloat()),
                baseAlpha = seededRange(i * 29 + 11, 0.35f, 0.75f),
                cornerRadius = seededRange(i * 41 + 13, 12f, 28f),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "glass_tiles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((22000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glass_tiles_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF040508))

        val gapX = w * 0.04f
        val gapY = h * 0.03f
        val tileW = (w - gapX * (cols + 1)) / cols
        val tileH = (h - gapY * (rows + 1)) / rows
        val dim = if (isActive) 1f else 0.55f
        val time = phase01(t)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val spec = tileSpecs[idx]
                val x = gapX + c * (tileW + gapX)
                val y = gapY + r * (tileH + gapY)

                val morph = sin01(time * 2f * PI.toFloat() + spec.phaseOffset)
                val colorA = TonalPalette.brightness(TonalPalette.pick(paletteColors, spec.colorIdx), brightness)
                val colorB = TonalPalette.brightness(TonalPalette.pick(paletteColors, spec.colorIdx + 2), brightness)
                val color = TonalPalette.mix(colorA, colorB, morph)

                val corner = CornerRadius(spec.cornerRadius, spec.cornerRadius)

                // Draw Drop Shadow
                val shadowOffset = Offset(tileW * 0.08f, tileH * 0.08f)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.45f * dim),
                    topLeft = Offset(x, y) + shadowOffset,
                    size = Size(tileW, tileH),
                    cornerRadius = corner
                )

                // Draw Glass Tile Body Gradient
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(color, spec.baseAlpha * dim),
                            TonalPalette.withAlpha(color, spec.baseAlpha * 0.4f * dim),
                        ),
                        start = Offset(x, y),
                        end = Offset(x + tileW, y + tileH)
                    ),
                    topLeft = Offset(x, y),
                    size = Size(tileW, tileH),
                    cornerRadius = corner
                )

                // Draw Glass Rim Top Highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f * dim),
                            Color.Transparent,
                        ),
                        startY = y,
                        endY = y + tileH * 0.4f
                    ),
                    topLeft = Offset(x, y),
                    size = Size(tileW, tileH),
                    cornerRadius = corner
                )
            }
        }
    }
}

private data class GlassTileSpec(
    val colorIdx: Int,
    val phaseOffset: Float,
    val baseAlpha: Float,
    val cornerRadius: Float,
)
