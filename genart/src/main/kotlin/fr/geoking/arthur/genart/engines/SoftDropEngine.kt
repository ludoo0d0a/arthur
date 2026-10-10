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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.floor

/**
 * Falling rectangular blocks stacking ambient — tetromino-like shapes, generic (no branding).
 */
@Composable
internal fun SoftDropEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val pieceCount = qualityCount(quality, low = 5, medium = 7, high = 10)
    val stackRows = qualityCount(quality, low = 3, medium = 4, high = 5)
    val pieces = remember(pieceCount) {
        List(pieceCount) { i ->
            SoftDropSeed(
                shape = (seededUnit(i * 17 + 3) * SHAPES.size).toInt().coerceIn(0, SHAPES.lastIndex),
                xCol = seededRange(i * 29 + 7, 0.5f, 8.5f).toInt().coerceIn(0, 8),
                fallPhase = seededUnit(i * 41 + 11),
                fallSpeed = seededRange(i * 53 + 13, 0.35f, 0.9f),
                colorIndex = i,
                rot = (seededUnit(i * 67 + 19) * 4f).toInt() % 4,
            )
        }
    }
    val stack = remember(stackRows) {
        List(stackRows * 10) { i ->
            SoftStackCell(
                col = i % 10,
                row = i / 10,
                filled = seededUnit(i * 23 + 5) > 0.55f,
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "softdrop")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "softdrop_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val cycle = phase01(t)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = TonalPalette.brightness(Color(0xFF0A0C12), brightness * dim))

        val cols = 10
        val rows = 18
        val cell = minOf(w / (cols + 2f), h / (rows + 2f))
        val originX = (w - cols * cell) * 0.5f
        val originY = (h - rows * cell) * 0.5f
        val grid = TonalPalette.withAlpha(
            TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * dim),
            0.12f * dim,
        )
        for (c in 0..cols) {
            drawLine(
                color = grid,
                start = Offset(originX + c * cell, originY),
                end = Offset(originX + c * cell, originY + rows * cell),
                strokeWidth = 1f,
            )
        }
        for (r in 0..rows) {
            drawLine(
                color = grid,
                start = Offset(originX, originY + r * cell),
                end = Offset(originX + cols * cell, originY + r * cell),
                strokeWidth = 1f,
            )
        }

        // Stacked base
        stack.forEach { cellSeed ->
            if (!cellSeed.filled) return@forEach
            val rowFromBottom = cellSeed.row
            val gy = rows - 1 - rowFromBottom
            val tint = TonalPalette.brightness(
                TonalPalette.mix(
                    SoftDropColors[cellSeed.colorIndex % SoftDropColors.size],
                    TonalPalette.pick(paletteColors, cellSeed.colorIndex),
                    0.3f,
                ),
                brightness * dim,
            )
            drawRoundedCell(
                originX + cellSeed.col * cell,
                originY + gy * cell,
                cell,
                tint,
                0.7f * dim,
            )
        }

        // Falling pieces
        pieces.forEach { piece ->
            val fall = phase01(cycle * piece.fallSpeed + piece.fallPhase)
            val rowF = fall * (rows - 4f)
            val shape = rotateShape(SHAPES[piece.shape], piece.rot)
            val tint = TonalPalette.brightness(
                TonalPalette.mix(
                    SoftDropColors[piece.colorIndex % SoftDropColors.size],
                    TonalPalette.pick(paletteColors, piece.colorIndex),
                    0.25f,
                ),
                brightness * dim,
            )
            shape.forEach { (dx, dy) ->
                val cx = piece.xCol + dx
                val cy = floor(rowF).toInt() + dy
                if (cx in 0 until cols && cy in 0 until rows) {
                    drawRoundedCell(originX + cx * cell, originY + cy * cell, cell, tint, 0.9f * dim)
                }
            }
        }
    }
}

private fun DrawScope.drawRoundedCell(
    x: Float,
    y: Float,
    cell: Float,
    tint: Color,
    alpha: Float,
) {
    val pad = cell * 0.08f
    drawRect(
        color = TonalPalette.withAlpha(tint, alpha),
        topLeft = Offset(x + pad, y + pad),
        size = Size(cell - pad * 2f, cell - pad * 2f),
    )
    drawRect(
        color = TonalPalette.withAlpha(Color.White, 0.12f * alpha),
        topLeft = Offset(x + pad, y + pad),
        size = Size(cell - pad * 2f, cell - pad * 2f),
        style = Stroke(width = 1.2f),
    )
}

private val SoftDropColors = listOf(
    Color(0xFF5EC8FF),
    Color(0xFFFFB347),
    Color(0xFF7CFC9A),
    Color(0xFFFF6B9D),
    Color(0xFFC9A0FF),
    Color(0xFFFFE066),
)

/** Generic tetromino-like offsets (no trademarked naming). */
private val SHAPES: List<List<Pair<Int, Int>>> = listOf(
    listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
    listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
    listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1),
    listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1),
    listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1),
    listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
    listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
)

private fun rotateShape(cells: List<Pair<Int, Int>>, rot: Int): List<Pair<Int, Int>> {
    var out = cells
    repeat(rot % 4) {
        out = out.map { (x, y) -> y to -x }
        val minX = out.minOf { it.first }
        val minY = out.minOf { it.second }
        out = out.map { (x, y) -> x - minX to y - minY }
    }
    return out
}

private data class SoftDropSeed(
    val shape: Int,
    val xCol: Int,
    val fallPhase: Float,
    val fallSpeed: Float,
    val colorIndex: Int,
    val rot: Int,
)

private data class SoftStackCell(
    val col: Int,
    val row: Int,
    val filled: Boolean,
    val colorIndex: Int,
)
