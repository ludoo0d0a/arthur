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

/** Bakes one frozen frame of Dog Stretch for Auto/Ambient album art. */
internal object DogStretchStill {
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
            cx, cy, w.coerceAtLeast(h) * 0.8f,
            intArrayOf(0xFF2C261C.toInt(), 0xFF100E0A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val stretch = sin(phase) * 0.5f + 0.5f
        val amount = stretch * pulse
        paint.color = mixArgb(0xFF2A1C12.toInt(), palette.colorAt(0), 0.28f)

        val bodyRx = minDim * (0.26f + 0.04f * amount)
        val bodyRy = minDim * (0.10f - 0.02f * amount)
        val bodyY = cy + minDim * 0.02f * amount
        rect.set(cx - bodyRx, bodyY - bodyRy, cx + bodyRx, bodyY + bodyRy)
        canvas.drawOval(rect, paint)
        val headR = minDim * 0.09f
        val hx = cx - bodyRx * 0.95f
        val hy = bodyY - minDim * 0.04f - minDim * 0.03f * amount
        canvas.drawCircle(hx, hy, headR, paint)
        rect.set(hx - headR * 1.2f, hy - headR * 0.25f, hx - headR * 0.5f, hy + headR * 0.25f)
        canvas.drawOval(rect, paint)

        paint.strokeWidth = minDim * 0.028f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        path.reset()
        path.moveTo(cx - bodyRx * 0.55f, bodyY)
        path.lineTo(cx - bodyRx * 0.7f, bodyY + minDim * (0.16f + 0.04f * amount))
        path.moveTo(cx + bodyRx * 0.45f, bodyY - bodyRy * 0.3f)
        path.lineTo(cx + bodyRx * 0.55f, bodyY + minDim * 0.08f)
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
