package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Brick Cascade for Auto/Ambient album art. */
internal object BrickCascadeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val hues = intArrayOf(0xFF6B6B, 0xFFB347, 0x6BCB77, 0x4D96FF, 0xC77DFF, 0xFFE066)

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
        paint.color = Color.rgb(10, 12, 20)
        canvas.drawRect(0f, 0f, w, h, paint)

        val cols = 10
        val rows = 5
        val marginX = w * 0.08f
        val marginTop = h * 0.1f
        val brickAreaH = h * 0.32f
        val gap = 3f
        val brickW = (w - marginX * 2f - gap * (cols - 1)) / cols
        val brickH = (brickAreaH - gap * (rows - 1)) / rows

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val fadePhase = rnd.nextFloat()
                val fadeSpeed = 0.15f + rnd.nextFloat() * 0.4f
                val life = ((sin((cycle * fadeSpeed + fadePhase) * 2f * PI.toFloat()) + 1.0) / 2.0).toFloat()
                val alpha = (0.15f + 0.7f * life) * dim
                if (alpha < 0.05f) continue
                val hue = hues[(row * cols + col) % hues.size]
                val tint = palette.colorAt(col)
                val r = (((hue shr 16) and 0xFF) * 0.75f + Color.red(tint) * 0.25f).toInt().coerceIn(0, 255)
                val g = (((hue shr 8) and 0xFF) * 0.75f + Color.green(tint) * 0.25f).toInt().coerceIn(0, 255)
                val b = ((hue and 0xFF) * 0.75f + Color.blue(tint) * 0.25f).toInt().coerceIn(0, 255)
                val x = marginX + col * (brickW + gap)
                val y = marginTop + row * (brickH + gap)
                paint.color = Color.argb((alpha * 255).toInt(), r, g, b)
                canvas.drawRect(x, y, x + brickW, y + brickH, paint)
                paint.color = Color.argb((25 * alpha).toInt(), 255, 255, 255)
                canvas.drawRect(x, y, x + brickW, y + brickH * 0.25f, paint)
            }
        }

        val paddleY = h * 0.88f
        val paddleW = w * 0.18f
        val tri = abs(((sin(cycle * 2f * PI.toFloat()) + 1.0) / 2.0).toFloat() * 2f - 1f)
        val paddleX = w * 0.5f + (tri - 0.5f) * w * 0.5f
        paint.color = Color.argb((220 * dim).toInt(), 0xE8, 0xE4, 0xD8)
        canvas.drawRect(paddleX - paddleW * 0.5f, paddleY, paddleX + paddleW * 0.5f, paddleY + h * 0.018f, paint)

        val bx = marginX + abs(((sin(cycle * 2.4f * PI.toFloat()) + 1.0) / 2.0).toFloat() * 2f - 1f) * (w - marginX * 2f)
        val byTop = marginTop + brickAreaH + h * 0.05f
        val byBot = paddleY - h * 0.03f
        val by = byTop + abs(((sin(cycle * 3.6f * PI.toFloat() + 0.6f) + 1.0) / 2.0).toFloat() * 2f - 1f) * (byBot - byTop)
        val ballR = minOf(w, h) * 0.012f
        paint.color = Color.argb((60 * dim).toInt(), 0xFF, 0xF6, 0xE8)
        canvas.drawCircle(bx, by, ballR * 2.2f, paint)
        paint.color = Color.argb((240 * dim).toInt(), 0xFF, 0xF6, 0xE8)
        canvas.drawCircle(bx, by, ballR, paint)
        paint.alpha = 255
    }
}
