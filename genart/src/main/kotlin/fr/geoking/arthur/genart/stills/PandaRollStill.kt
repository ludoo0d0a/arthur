package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Panda Roll for Auto/Ambient album art. */
internal object PandaRollStill {
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
        val cx = w * 0.5f
        val cy = h * 0.55f

        paint.style = Paint.Style.FILL
        paint.color = 0xFF1C2820.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = RadialGradient(
            cx, h * 0.75f, minDim * 0.4f,
            intArrayOf(Color.argb(100, 0x2A, 0x38, 0x28), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - minDim * 0.4f, h * 0.65f, cx + minDim * 0.4f, h * 0.87f, paint)
        paint.shader = null

        val rock = sin(phase) * 18f * pulse
        val white = 0xFFE8E4DC.toInt()
        val black = mixArgb(0xFF12100E.toInt(), palette.colorAt(0), 0.1f)
        val bodyR = minDim * 0.20f

        canvas.save()
        canvas.rotate(rock, cx, cy + bodyR * 0.3f)
        paint.color = white
        canvas.drawCircle(cx, cy, bodyR, paint)
        paint.color = Color.argb(128, Color.red(black), Color.green(black), Color.blue(black))
        canvas.drawCircle(cx, cy + bodyR * 0.15f, bodyR * 0.55f, paint)
        val headR = bodyR * 0.55f
        val hx = cx - bodyR * 0.15f
        val hy = cy - bodyR * 0.75f
        paint.color = white
        canvas.drawCircle(hx, hy, headR, paint)
        paint.color = black
        canvas.drawCircle(hx - headR * 0.7f, hy - headR * 0.7f, headR * 0.32f, paint)
        canvas.drawCircle(hx + headR * 0.7f, hy - headR * 0.7f, headR * 0.32f, paint)
        rect.set(hx - headR * 0.7f, hy - headR * 0.2f, hx - headR * 0.2f, hy + headR * 0.2f)
        canvas.drawOval(rect, paint)
        rect.set(hx + headR * 0.2f, hy - headR * 0.2f, hx + headR * 0.7f, hy + headR * 0.2f)
        canvas.drawOval(rect, paint)
        canvas.drawCircle(cx - bodyR * 0.75f, cy + bodyR * 0.35f, bodyR * 0.22f, paint)
        canvas.drawCircle(cx + bodyR * 0.8f, cy - bodyR * 0.15f, bodyR * 0.22f, paint)
        canvas.restore()
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
