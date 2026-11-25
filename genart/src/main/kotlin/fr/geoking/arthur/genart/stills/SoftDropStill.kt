package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI

private const val SOFT_DROP_COLS = 10
private const val SOFT_DROP_ROWS = 16
private const val SOFT_DROP_STACK_ROWS = 4
private const val SOFT_DROP_FALL_ROWS = SOFT_DROP_ROWS - SOFT_DROP_STACK_ROWS
private const val SOFT_DROP_PIECE_COUNT = 7

/** Bakes one frozen frame of Soft Drop for Auto/Ambient album art. */
internal object SoftDropStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val shapes = listOf(
        listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
        listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
    )
    private val hues = intArrayOf(0x5EC8FF, 0xFFB347, 0x7CFC9A, 0xFF6B9D, 0xC9A0FF, 0xFFE066)

    @Suppress("UNUSED_PARAMETER")
    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val w = size.toFloat()
        val h = size.toFloat()
        val dim = 0.65f + 0.35f * pulse
        val cycle = phase01(
            (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f,
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(10, 12, 18)
        canvas.drawRect(0f, 0f, w, h, paint)

        val cols = SOFT_DROP_COLS
        val rows = SOFT_DROP_ROWS
        val stackRows = SOFT_DROP_STACK_ROWS
        val cell = minOf(w / (cols + 0.9f), h / (rows + 1.4f))
        val originX = (w - cols * cell) * 0.5f
        val originY = (h - rows * cell) * 0.5f + cell * 0.45f

        paint.color = Color.argb((36 * dim).toInt(), Color.red(palette.colorAt(0)), Color.green(palette.colorAt(0)), Color.blue(palette.colorAt(0)))
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        for (c in 0..cols) {
            canvas.drawLine(originX + c * cell, originY, originX + c * cell, originY + rows * cell, paint)
        }
        for (r in 0..rows) {
            paint.alpha = if (r == 0) (72 * dim).toInt() else (36 * dim).toInt()
            paint.strokeWidth = if (r == 0) 1.4f else 1f
            canvas.drawLine(originX, originY + r * cell, originX + cols * cell, originY + r * cell, paint)
        }

        val stackTopRow = rows - stackRows
        paint.alpha = (56 * dim).toInt()
        paint.strokeWidth = 1.2f
        canvas.drawLine(originX, originY + stackTopRow * cell, originX + cols * cell, originY + stackTopRow * cell, paint)

        paint.style = Paint.Style.FILL
        for (i in 0 until stackRows * cols) {
            if (seededUnit(i * 23 + 5) <= 0.32f) continue
            val col = i % cols
            val row = i / cols
            val hue = hues[i % hues.size]
            drawCell(
                canvas,
                originX + col * cell,
                originY + (rows - stackRows + row) * cell,
                cell,
                hue,
                palette.colorAt(i),
                0.78f * dim,
            )
        }

        val spawnRow = -2.5f
        val landRow = (SOFT_DROP_FALL_ROWS - 1).toFloat()
        for (i in 0 until SOFT_DROP_PIECE_COUNT) {
            val shapeIndex = (seededUnit(i * 17 + 3) * shapes.size).toInt().coerceIn(0, shapes.lastIndex)
            val rot = (seededUnit(i * 67 + 19) * 4f).toInt() % 4
            val shape = rotateShape(shapes[shapeIndex], rot)
            val width = shape.maxOf { it.first } + 1
            val maxCol = (cols - width).coerceAtLeast(1)
            val xCol = (seededUnit(i * 29 + 7) * maxCol).toInt().coerceIn(0, cols - width)
            val fallProgress = phase01(cycle + seededUnit(i * 41 + 11))
            val rowF = spawnRow + fallProgress * (landRow - spawnRow)
            val hue = hues[i % hues.size]
            shape.forEach { (dx, dy) ->
                val cx = xCol + dx
                val cy = rowF + dy
                if (cx in 0 until cols && cy >= -2f && cy < rows) {
                    drawCell(canvas, originX + cx * cell, originY + cy * cell, cell, hue, palette.colorAt(i), 0.92f * dim)
                }
            }
        }
        paint.alpha = 255
    }

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

    private fun drawCell(
        canvas: Canvas,
        x: Float,
        y: Float,
        cell: Float,
        hue: Int,
        paletteColor: Int,
        alpha: Float,
    ) {
        val pad = cell * 0.06f
        val r = (((hue shr 16) and 0xFF) * 0.75f + Color.red(paletteColor) * 0.25f).toInt().coerceIn(0, 255)
        val g = (((hue shr 8) and 0xFF) * 0.75f + Color.green(paletteColor) * 0.25f).toInt().coerceIn(0, 255)
        val b = ((hue and 0xFF) * 0.75f + Color.blue(paletteColor) * 0.25f).toInt().coerceIn(0, 255)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
        canvas.drawRect(x + pad, y + pad, x + cell - pad, y + cell - pad, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = Color.argb((36 * alpha).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawRect(x + pad, y + pad, x + cell - pad, y + cell - pad, paint)
        paint.style = Paint.Style.FILL
    }
}