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

        // Warm dark ambient background
        paint.shader = RadialGradient(
            cx, cy, w.coerceAtLeast(h) * 0.85f,
            intArrayOf(0xFF2C221C.toInt(), 0xFF1A1410.toInt(), 0xFF0C0908.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        // Cozy rug / soft pool under sleeping cat
        val rugRx = minDim * 0.44f
        val rugRy = minDim * 0.22f
        paint.shader = RadialGradient(
            cx, cy + minDim * 0.04f, rugRx,
            intArrayOf(
                Color.argb((70 * pulse).toInt().coerceIn(40, 100), Color.red(palette.colorAt(2)), Color.green(palette.colorAt(2)), Color.blue(palette.colorAt(2))),
                Color.argb(80, 0x38, 0x29, 0x1F),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        rect.set(cx - rugRx, cy + minDim * 0.04f - rugRy, cx + rugRx, cy + minDim * 0.04f + rugRy)
        canvas.drawOval(rect, paint)
        paint.shader = null

        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.03f * breath * pulse
        val bodyColor = mixArgb(0xFF241810.toInt(), palette.colorAt(0), 0.28f)
        paint.color = bodyColor

        // Curled body
        val bodyRx = minDim * 0.28f * scale
        val bodyRy = minDim * 0.18f * scale
        rect.set(cx - bodyRx, cy - bodyRy, cx + bodyRx, cy + bodyRy)
        canvas.drawOval(rect, paint)

        // Head nestled into body
        val headR = minDim * 0.13f * scale
        val hx = cx - bodyRx * 0.55f
        val hy = cy - bodyRy * 0.45f
        canvas.drawCircle(hx, hy, headR, paint)
        canvas.drawCircle(cx + bodyRx * 0.4f, cy - bodyRy * 0.2f, minDim * 0.10f * scale, paint)

        // Pointed ears
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

        // Curled tail
        path.reset()
        val tailBaseX = cx + bodyRx * 0.78f
        val tailBaseY = cy - bodyRy * 0.1f
        val tailTipX = cx - bodyRx * 0.1f
        val tailTipY = cy + bodyRy * 0.82f
        path.moveTo(tailBaseX, tailBaseY)
        path.cubicTo(
            tailBaseX + minDim * 0.14f,
            tailBaseY + bodyRy * 1.1f,
            cx + bodyRx * 0.35f,
            cy + bodyRy * 1.05f,
            tailTipX,
            tailTipY,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.038f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)

        // Sleeping eye curve
        path.reset()
        val ex = hx - headR * 0.2f
        val ey = hy + headR * 0.15f
        path.moveTo(ex - headR * 0.25f, ey - headR * 0.05f)
        path.quadTo(ex, ey + headR * 0.12f, ex + headR * 0.25f, ey - headR * 0.05f)
        paint.strokeWidth = minDim * 0.008f
        paint.color = Color.argb(90, Color.red(palette.colorAt(1)), Color.green(palette.colorAt(1)), Color.blue(palette.colorAt(1)))
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // Motes
        val moteGlow = palette.colorAt(1)
        for (i in 0 until 4) {
            val mx = cx + minDim * (0.15f * i - 0.25f)
            val my = cy - minDim * (0.12f * i + 0.15f)
            paint.shader = RadialGradient(
                mx, my, minDim * 0.045f,
                intArrayOf(Color.argb(70, Color.red(moteGlow), Color.green(moteGlow), Color.blue(moteGlow)), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(mx, my, minDim * 0.045f, paint)
        }
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
