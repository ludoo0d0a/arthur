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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.min

/**
 * Dense nested / interlocking pack — ovals, oblongs, teardrops, irregular
 * triangles & quads, and soft Tetris polyominoes. Shape kind is seeded-random;
 * color bands follow horizontal position across the palette (Tapet multicolor nest).
 */
@Composable
internal fun NestedPackEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cols = qualityCount(quality, low = 6, medium = 9, high = 12)
    val rows = qualityCount(quality, low = 9, medium = 13, high = 17)
    val shadowMul = when (quality) {
        GenartQuality.Low -> 0.6f
        GenartQuality.Medium -> 0.9f
        GenartQuality.High -> 1.15f
    }
    val pieces = remember(cols, rows) {
        List(cols * rows) { i ->
            val col = i % cols
            val row = i / cols
            NestedPackPiece(
                col = col,
                row = row,
                kind = (seededUnit(i * 17 + 3) * 7f).toInt().coerceIn(0, 6),
                rot = seededRange(i * 29 + 7, -0.35f, 0.35f) * PI.toFloat(),
                sizeJitter = seededRange(i * 41 + 11, 0.88f, 1.18f),
                aspect = seededRange(i * 53 + 13, 0.55f, 1.55f),
                corner = seededRange(i * 67 + 19, 0.22f, 0.5f),
                colorMix = seededUnit(i * 79 + 23),
                breathPhase = seededRange(i * 89 + 29, 0f, 2f * PI.toFloat()),
                breathAmp = seededRange(i * 97 + 31, 0.012f, 0.045f),
                skewA = seededRange(i * 103 + 37, -0.22f, 0.22f),
                skewB = seededRange(i * 107 + 41, -0.22f, 0.22f),
                tetrisVariant = (seededUnit(i * 109 + 43) * 4f).toInt().coerceIn(0, 3),
                alpha = seededRange(i * 113 + 47, 0.82f, 1f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "nested_pack")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((48000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nested_pack_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val gapTint = TonalPalette.mix(
            TonalPalette.pick(paletteColors, 2),
            Color(0xFF0C0812),
            0.72f,
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    TonalPalette.brightness(gapTint, brightness * 0.4f),
                    Color(0xFF08060C),
                ),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val cellW = w / (cols - 0.35f)
        val cellH = h / (rows - 0.35f)
        val shadowPx = (minDim * 0.012f * shadowMul).coerceIn(3f, 18f)
        val nPalette = paletteColors.size.coerceAtLeast(1)

        // Back-to-front: top rows first so lower pieces nest over upper ones.
        pieces.sortedBy { it.row * cols + it.col }.forEach { piece ->
            val stagger = if (piece.row % 2 == 0) 0f else cellW * 0.5f
            val cx = piece.col * cellW + stagger + cellW * 0.15f
            val cy = piece.row * cellH * 0.62f + cellH * 0.35f
            val breath = 1f + piece.breathAmp * (sin01(time + piece.breathPhase) * 2f - 1f)
            val bw = cellW * 0.92f * piece.sizeJitter * piece.aspect.coerceIn(0.7f, 1.4f) * breath
            val bh = cellH * 0.95f * piece.sizeJitter * breath
            // Horizontal multicolor bands from x position.
            val bandT = (cx / w).coerceIn(0f, 1f)
            val colorIndex = (bandT * (nPalette - 1).coerceAtLeast(1)).toInt()
            val cA = TonalPalette.pick(paletteColors, colorIndex)
            val cB = TonalPalette.pick(paletteColors, colorIndex + 1)
            val tint = TonalPalette.brightness(
                TonalPalette.mix(cA, cB, piece.colorMix * 0.45f + bandT * 0.2f),
                brightness,
            )
            val rotDeg = Math.toDegrees(piece.rot.toDouble()).toFloat()
            rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
                drawNestedShape(
                    kind = piece.kind,
                    cx = cx,
                    cy = cy,
                    bw = bw,
                    bh = bh,
                    corner = piece.corner,
                    skewA = piece.skewA,
                    skewB = piece.skewB,
                    tetrisVariant = piece.tetrisVariant,
                    fill = TonalPalette.withAlpha(tint, piece.alpha * dim),
                    highlight = TonalPalette.withAlpha(
                        TonalPalette.mix(tint, Color.White, 0.18f),
                        piece.alpha * dim,
                    ),
                    shade = TonalPalette.withAlpha(
                        TonalPalette.mix(tint, Color.Black, 0.22f),
                        piece.alpha * dim,
                    ),
                    shadow = TonalPalette.withAlpha(Color.Black, 0.34f * dim * shadowMul),
                    shadowPx = shadowPx,
                )
            }
        }
    }
}

private fun DrawScope.drawNestedShape(
    kind: Int,
    cx: Float,
    cy: Float,
    bw: Float,
    bh: Float,
    corner: Float,
    skewA: Float,
    skewB: Float,
    tetrisVariant: Int,
    fill: Color,
    highlight: Color,
    shade: Color,
    shadow: Color,
    shadowPx: Float,
) {
    when (kind) {
        0 -> { // oval
            drawOval(color = shadow, topLeft = Offset(cx - bw * 0.5f + shadowPx * 0.4f, cy - bh * 0.5f + shadowPx), size = Size(bw, bh))
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(highlight, fill, shade),
                    start = Offset(cx - bw * 0.35f, cy - bh * 0.4f),
                    end = Offset(cx + bw * 0.3f, cy + bh * 0.4f),
                ),
                topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.5f),
                size = Size(bw, bh),
            )
        }
        1 -> { // oblong / capsule
            val cr = CornerRadius(bh * 0.5f, bh * 0.5f)
            drawRoundRect(
                color = shadow,
                topLeft = Offset(cx - bw * 0.5f + shadowPx * 0.4f, cy - bh * 0.5f + shadowPx),
                size = Size(bw, bh),
                cornerRadius = cr,
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(highlight, fill, shade),
                    start = Offset(cx - bw * 0.4f, cy - bh * 0.4f),
                    end = Offset(cx + bw * 0.35f, cy + bh * 0.4f),
                ),
                topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.5f),
                size = Size(bw, bh),
                cornerRadius = cr,
            )
        }
        2 -> { // teardrop / petal (reference-like)
            val path = nestedTeardropPath(cx, cy, bw, bh)
            drawPath(path = Path().apply { addPath(path, Offset(shadowPx * 0.4f, shadowPx)) }, color = shadow)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(highlight, fill, shade),
                    start = Offset(cx - bw * 0.25f, cy - bh * 0.45f),
                    end = Offset(cx + bw * 0.25f, cy + bh * 0.4f),
                ),
            )
        }
        3 -> { // irregular triangle
            val path = Path().apply {
                moveTo(cx + bw * skewA * 0.3f, cy - bh * 0.52f)
                lineTo(cx + bw * (0.52f + skewB * 0.2f), cy + bh * (0.42f + skewA * 0.15f))
                lineTo(cx - bw * (0.48f + skewA * 0.2f), cy + bh * (0.38f + skewB * 0.15f))
                close()
            }
            drawPath(path = Path().apply { addPath(path, Offset(shadowPx * 0.4f, shadowPx)) }, color = shadow)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(highlight, fill, shade),
                    start = Offset(cx - bw * 0.2f, cy - bh * 0.4f),
                    end = Offset(cx + bw * 0.2f, cy + bh * 0.35f),
                ),
            )
        }
        4 -> { // irregular rectangle (jittered quad)
            val path = Path().apply {
                moveTo(cx - bw * 0.5f + bw * skewA * 0.15f, cy - bh * 0.5f + bh * skewB * 0.1f)
                lineTo(cx + bw * 0.5f + bw * skewB * 0.12f, cy - bh * 0.5f + bh * skewA * 0.08f)
                lineTo(cx + bw * 0.5f - bw * skewA * 0.1f, cy + bh * 0.5f + bh * skewB * 0.12f)
                lineTo(cx - bw * 0.5f - bw * skewB * 0.08f, cy + bh * 0.5f - bh * skewA * 0.1f)
                close()
            }
            drawPath(path = Path().apply { addPath(path, Offset(shadowPx * 0.4f, shadowPx)) }, color = shadow)
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(highlight, fill, shade),
                    start = Offset(cx - bw * 0.35f, cy - bh * 0.35f),
                    end = Offset(cx + bw * 0.3f, cy + bh * 0.35f),
                ),
            )
        }
        5 -> { // soft tetris polyomino
            drawNestedTetris(
                cx = cx,
                cy = cy,
                bw = bw,
                bh = bh,
                variant = tetrisVariant,
                corner = corner,
                fill = fill,
                highlight = highlight,
                shade = shade,
                shadow = shadow,
                shadowPx = shadowPx,
            )
        }
        else -> { // soft rounded rect
            val cr = CornerRadius(bw * corner, bh * corner)
            drawRoundRect(
                color = shadow,
                topLeft = Offset(cx - bw * 0.5f + shadowPx * 0.4f, cy - bh * 0.5f + shadowPx),
                size = Size(bw, bh),
                cornerRadius = cr,
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(highlight, fill, shade),
                    start = Offset(cx - bw * 0.4f, cy - bh * 0.4f),
                    end = Offset(cx + bw * 0.35f, cy + bh * 0.4f),
                ),
                topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.5f),
                size = Size(bw, bh),
                cornerRadius = cr,
            )
        }
    }
}

private fun nestedTeardropPath(cx: Float, cy: Float, bw: Float, bh: Float): Path {
    val top = cy - bh * 0.5f
    val bottom = cy + bh * 0.5f
    val left = cx - bw * 0.5f
    val right = cx + bw * 0.5f
    return Path().apply {
        moveTo(cx, top)
        cubicTo(right, top + bh * 0.15f, right, cy + bh * 0.15f, cx + bw * 0.08f, bottom)
        cubicTo(cx + bw * 0.02f, bottom + bh * 0.02f, cx - bw * 0.02f, bottom + bh * 0.02f, cx - bw * 0.08f, bottom)
        cubicTo(left, cy + bh * 0.15f, left, top + bh * 0.15f, cx, top)
        close()
    }
}

private fun DrawScope.drawNestedTetris(
    cx: Float,
    cy: Float,
    bw: Float,
    bh: Float,
    variant: Int,
    corner: Float,
    fill: Color,
    highlight: Color,
    shade: Color,
    shadow: Color,
    shadowPx: Float,
) {
    val cell = min(bw, bh) * 0.42f
    val cr = CornerRadius(cell * corner.coerceIn(0.2f, 0.45f), cell * corner.coerceIn(0.2f, 0.45f))
    val offsets = when (variant) {
        0 -> listOf( // L
            Offset(-cell * 0.55f, -cell * 0.55f),
            Offset(-cell * 0.55f, 0f),
            Offset(-cell * 0.55f, cell * 0.55f),
            Offset(cell * 0.55f, cell * 0.55f),
        )
        1 -> listOf( // T
            Offset(-cell * 0.55f, -cell * 0.35f),
            Offset(0f, -cell * 0.35f),
            Offset(cell * 0.55f, -cell * 0.35f),
            Offset(0f, cell * 0.55f),
        )
        2 -> listOf( // S / Z
            Offset(-cell * 0.55f, cell * 0.3f),
            Offset(0f, cell * 0.3f),
            Offset(0f, -cell * 0.4f),
            Offset(cell * 0.55f, -cell * 0.4f),
        )
        else -> listOf( // square block
            Offset(-cell * 0.35f, -cell * 0.35f),
            Offset(cell * 0.35f, -cell * 0.35f),
            Offset(-cell * 0.35f, cell * 0.35f),
            Offset(cell * 0.35f, cell * 0.35f),
        )
    }
    offsets.forEach { off ->
        val ox = cx + off.x
        val oy = cy + off.y
        drawRoundRect(
            color = shadow,
            topLeft = Offset(ox - cell * 0.5f + shadowPx * 0.35f, oy - cell * 0.5f + shadowPx),
            size = Size(cell, cell),
            cornerRadius = cr,
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(highlight, fill, shade),
                start = Offset(ox - cell * 0.35f, oy - cell * 0.35f),
                end = Offset(ox + cell * 0.3f, oy + cell * 0.3f),
            ),
            topLeft = Offset(ox - cell * 0.5f, oy - cell * 0.5f),
            size = Size(cell, cell),
            cornerRadius = cr,
        )
    }
}

private data class NestedPackPiece(
    val col: Int,
    val row: Int,
    val kind: Int,
    val rot: Float,
    val sizeJitter: Float,
    val aspect: Float,
    val corner: Float,
    val colorMix: Float,
    val breathPhase: Float,
    val breathAmp: Float,
    val skewA: Float,
    val skewB: Float,
    val tetrisVariant: Int,
    val alpha: Float,
)
