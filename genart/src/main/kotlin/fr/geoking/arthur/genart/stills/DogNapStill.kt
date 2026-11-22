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

/** Bakes one frozen frame of Dog Nap for Auto/Ambient album art. */
internal object DogNapStill {
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
        val cy = h * 0.56f

        paint.shader = RadialGradient(
            cx, cy + minDim * 0.05f, w.coerceAtLeast(h) * 0.8f,
            intArrayOf(0xFF2E261E.toInt(), 0xFF1B1612.toInt(), 0xFF0E0B09.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.028f * breath * pulse
        paint.color = mixArgb(0xFF2A1E14.toInt(), palette.colorAt(0), 0.3f)

        val bodyRx = minDim * 0.30f * scale
        val bodyRy = minDim * 0.20f * scale
        rect.set(cx - bodyRx, cy - bodyRy, cx + bodyRx, cy + bodyRy)
        canvas.drawOval(rect, paint)
        val headR = minDim * 0.14f * scale
        val hx = cx - bodyRx * 0.5f
        val hy = cy - bodyRy * 0.25f
        canvas.drawCircle(hx, hy, headR, paint)
        rect.set(hx - headR * 1.15f, hy - headR * 0.25f, hx - headR * 0.45f, hy + headR * 0.3f)
        canvas.drawOval(rect, paint)

        path.reset()
        path.moveTo(hx + headR * 0.2f, hy - headR * 0.4f)
        path.quadTo(hx + headR * 0.9f, hy + headR * 0.1f, hx + headR * 0.55f, hy + headR * 0.85f)
        path.quadTo(hx + headR * 0.15f, hy + headR * 0.35f, hx + headR * 0.2f, hy - headR * 0.4f)
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
