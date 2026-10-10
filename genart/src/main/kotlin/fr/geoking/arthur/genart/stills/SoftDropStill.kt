package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.floor
import kotlin.random.Random

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

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val dim = 0.65f + 0.35f * pulse
        val cycle = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(10, 12, 18)
        canvas.drawRect(0f, 0f, w, h, paint)

        val cols = 10
        val rows = 18
        val cell = minOf(w / (cols + 2f), h / (rows + 2f))
        val originX = (w - cols * cell) * 0.5f
        val originY = (h - rows * cell) * 0.5f

        paint.color = Color.argb((30 * dim).toInt(), Color.red(palette.colorAt(0)), Color.green(palette.colorAt(0)), Color.blue(palette.colorAt(0)))
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        for (c in 0..cols) {
            canvas.drawLine(originX + c * cell, originY, originX + c * cell, originY + rows * cell, paint)
        }
        for (r in 0..rows) {
            canvas.drawLine(originX, originY + r * cell, originX + cols * cell, originY + r * cell, paint)
        }

        paint.style = Paint.Style.FILL
        for (row in 0 until 4) {
            for (col in 0 until cols) {
                if (rnd.nextFloat() <= 0.55f) continue
                val hue = hues[(row * cols + col) % hues.size]
                drawCell(
                    canvas,
                    originX + col * cell,
                    originY + (rows - 1 - row) * cell,
                    cell,
                    hue,
                    palette.colorAt(col),
                    0.7f * dim,
                )
            }
        }

        for (i in 0 until 7) {
            val shape = shapes[rnd.nextInt(shapes.size)]
            val xCol = rnd.nextInt(9)
            val fall = ((cycle * (0.35f + rnd.nextFloat() * 0.55f) + rnd.nextFloat()) % 1f + 1f) % 1f
            val rowF = fall * (rows - 4f)
            val hue = hues[i % hues.size]
            shape.forEach { (dx, dy) ->
                val cx = xCol + dx
                val cy = floor(rowF).toInt() + dy
                if (cx in 0 until cols && cy in 0 until rows) {
                    drawCell(canvas, originX + cx * cell, originY + cy * cell, cell, hue, palette.colorAt(i), 0.9f * dim)
                }
            }
        }
        paint.alpha = 255
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
        val pad = cell * 0.08f
        val r = (((hue shr 16) and 0xFF) * 0.75f + Color.red(paletteColor) * 0.25f).toInt().coerceIn(0, 255)
        val g = (((hue shr 8) and 0xFF) * 0.75f + Color.green(paletteColor) * 0.25f).toInt().coerceIn(0, 255)
        val b = ((hue and 0xFF) * 0.75f + Color.blue(paletteColor) * 0.25f).toInt().coerceIn(0, 255)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
        canvas.drawRect(x + pad, y + pad, x + cell - pad, y + cell - pad, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = Color.argb((30 * alpha).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawRect(x + pad, y + pad, x + cell - pad, y + cell - pad, paint)
        paint.style = Paint.Style.FILL
    }
}
