package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Sitting Panda for Auto/Ambient album art. */
internal object PandaSitStill {
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
        paint.shader = null
        paint.color = 0xFF1E2A22.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = RadialGradient(
            cx, h * 0.78f, minDim * 0.45f,
            intArrayOf(Color.argb(115, 0x2A, 0x3A, 0x28), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - minDim * 0.45f, h * 0.68f, cx + minDim * 0.45f, h * 0.93f, paint)
        paint.shader = null

        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.025f * breath * pulse
        val white = mixArgb(0xFFE8E4DC.toInt(), palette.colorAt(1), 0.12f)
        val black = mixArgb(0xFF141210.toInt(), palette.colorAt(0), 0.15f)

        val bodyRx = minDim * 0.22f * scale
        val bodyRy = minDim * 0.26f * scale
        paint.color = white
        rect.set(cx - bodyRx, cy - bodyRy * 0.3f, cx + bodyRx, cy + bodyRy * 1.7f)
        canvas.drawOval(rect, paint)
        paint.color = Color.argb(140, Color.red(black), Color.green(black), Color.blue(black))
        rect.set(cx - bodyRx * 0.55f, cy + bodyRy * 0.15f, cx + bodyRx * 0.55f, cy + bodyRy * 1.05f)
        canvas.drawOval(rect, paint)

        val headR = minDim * 0.14f * scale
        val hy = cy - bodyRy * 0.55f
        paint.color = white
        canvas.drawCircle(cx, hy, headR, paint)
        paint.color = black
        canvas.drawCircle(cx - headR * 0.75f, hy - headR * 0.75f, headR * 0.38f, paint)
        canvas.drawCircle(cx + headR * 0.75f, hy - headR * 0.75f, headR * 0.38f, paint)
        rect.set(cx - headR * 0.75f, hy - headR * 0.25f, cx - headR * 0.2f, hy + headR * 0.2f)
        canvas.drawOval(rect, paint)
        rect.set(cx + headR * 0.2f, hy - headR * 0.25f, cx + headR * 0.75f, hy + headR * 0.2f)
        canvas.drawOval(rect, paint)
        canvas.drawCircle(cx - bodyRx * 0.85f, cy + bodyRy * 0.35f, minDim * 0.055f * scale, paint)
        canvas.drawCircle(cx + bodyRx * 0.85f, cy + bodyRy * 0.35f, minDim * 0.055f * scale, paint)
        canvas.drawCircle(cx - bodyRx * 0.35f, cy + bodyRy * 1.35f, minDim * 0.06f * scale, paint)
        canvas.drawCircle(cx + bodyRx * 0.35f, cy + bodyRy * 1.35f, minDim * 0.06f * scale, paint)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
