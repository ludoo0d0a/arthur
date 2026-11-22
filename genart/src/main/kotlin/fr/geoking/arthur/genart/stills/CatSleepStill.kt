package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Sleeping Cat for Auto/Ambient album art. */
internal object CatSleepStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val path = Path()

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
        val cy = h * 0.58f

        paint.shader = RadialGradient(
            cx, cy, w.coerceAtLeast(h) * 0.85f,
            intArrayOf(0xFF2C221C.toInt(), 0xFF1A1410.toInt(), 0xFF0C0908.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.03f * breath * pulse
        val bodyColor = mixArgb(0xFF241810.toInt(), palette.colorAt(0), 0.28f)
        paint.color = bodyColor

        val bodyRx = minDim * 0.28f * scale
        val bodyRy = minDim * 0.18f * scale
        rect.set(cx - bodyRx, cy - bodyRy, cx + bodyRx, cy + bodyRy)
        canvas.drawOval(rect, paint)
        val headR = minDim * 0.13f * scale
        val hx = cx - bodyRx * 0.55f
        val hy = cy - bodyRy * 0.45f
        canvas.drawCircle(hx, hy, headR, paint)
        canvas.drawCircle(cx + bodyRx * 0.4f, cy - bodyRy * 0.2f, minDim * 0.10f * scale, paint)

        path.reset()
        path.moveTo(hx - headR * 0.7f, hy - headR * 0.15f)
        path.lineTo(hx - headR * 0.95f, hy - headR * 1.4f)
        path.lineTo(hx - headR * 0.1f, hy - headR * 0.55f)
        path.close()
        path.moveTo(hx + headR * 0.05f, hy - headR * 0.5f)
        path.lineTo(hx + headR * 0.85f, hy - headR * 1.35f)
        path.lineTo(hx + headR * 0.65f, hy - headR * 0.1f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
