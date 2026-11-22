package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Panda and Bamboo for Auto/Ambient album art. */
internal object PandaBambooStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

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
        val minDim = w.coerceAtMost(h)

        paint.style = Paint.Style.FILL
        paint.color = 0xFF243028.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val bamboo = mixArgb(0xFF4A6B3A.toInt(), palette.colorAt(2), 0.2f)
        paint.color = bamboo
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val stalkXs = floatArrayOf(0.15f, 0.32f, 0.48f, 0.72f, 0.88f)
        stalkXs.forEachIndexed { i, nx ->
            val baseX = nx * w
            val tipX = baseX + sin(phase + i) * minDim * 0.012f
            paint.strokeWidth = minDim * (0.014f + i * 0.0015f)
            canvas.drawLine(baseX, h * 0.95f, tipX, h * 0.05f, paint)
        }

        paint.style = Paint.Style.FILL
        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.02f * breath * pulse
        val white = 0xFFE6E2DA.toInt()
        val black = 0xFF12100E.toInt()
        val cx = w * 0.55f
        val cy = h * 0.62f
        val bodyRx = minDim * 0.16f * scale
        val bodyRy = minDim * 0.14f * scale
        paint.color = white
        rect.set(cx - bodyRx, cy - bodyRy, cx + bodyRx, cy + bodyRy)
        canvas.drawOval(rect, paint)
        val headR = minDim * 0.09f * scale
        val hx = cx - bodyRx * 0.15f
        val hy = cy - bodyRy * 1.15f
        canvas.drawCircle(hx, hy, headR, paint)
        paint.color = black
        canvas.drawCircle(hx - headR * 0.7f, hy - headR * 0.7f, headR * 0.35f, paint)
        canvas.drawCircle(hx + headR * 0.7f, hy - headR * 0.7f, headR * 0.35f, paint)
        canvas.drawCircle(cx - bodyRx * 0.9f, cy, minDim * 0.04f * scale, paint)
        canvas.drawCircle(cx + bodyRx * 0.85f, cy + bodyRy * 0.1f, minDim * 0.04f * scale, paint)

        paint.color = bamboo
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.012f
        canvas.drawLine(cx + bodyRx * 0.3f, cy - bodyRy * 0.2f, cx + bodyRx * 0.5f, cy - bodyRy * 1.8f, paint)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
