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

        // Warm indoor background
        paint.shader = RadialGradient(
            cx, cy, w.coerceAtLeast(h) * 0.8f,
            intArrayOf(0xFF2C261C.toInt(), 0xFF201A14.toInt(), 0xFF100E0A.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Warm sunbeam
        val sunColor = mixArgb(0xFFE8C898.toInt(), palette.colorAt(1), 0.2f)
        paint.shader = RadialGradient(
            w * 0.25f, h * 0.15f, w * 0.6f,
            intArrayOf(
                Color.argb(51, Color.red(sunColor), Color.green(sunColor), Color.blue(sunColor)),
                Color.argb(20, Color.red(sunColor), Color.green(sunColor), Color.blue(sunColor)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h * 0.6f, paint)
        paint.shader = null

        // Floor pool
        paint.shader = RadialGradient(
            cx, h * 0.74f, minDim * 0.48f,
            intArrayOf(Color.argb(90, 0x3A, 0x2E, 0x20), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - minDim * 0.48f, h * 0.64f, cx + minDim * 0.48f, h * 0.84f, paint)
        paint.shader = null

        val stretch = sin(phase) * 0.5f + 0.5f
        val amount = stretch * pulse
        paint.color = mixArgb(0xFF2A1C12.toInt(), palette.colorAt(0), 0.28f)

        // Body
        val bodyRx = minDim * (0.26f + 0.04f * amount)
        val bodyRy = minDim * (0.10f - 0.02f * amount)
        val bodyY = cy + minDim * 0.02f * amount
        rect.set(cx - bodyRx, bodyY - bodyRy, cx + bodyRx, bodyY + bodyRy)
        canvas.drawOval(rect, paint)

        // Head
        val headR = minDim * 0.09f
        val hx = cx - bodyRx * 0.95f
        val hy = bodyY - minDim * 0.04f - minDim * 0.03f * amount
        canvas.drawCircle(hx, hy, headR, paint)
        // Snout
        rect.set(hx - headR * 1.2f, hy - headR * 0.25f, hx - headR * 0.5f, hy + headR * 0.25f)
        canvas.drawOval(rect, paint)
        // Ear
        path.reset()
        path.moveTo(hx + headR * 0.1f, hy - headR * 0.4f)
        path.lineTo(hx + headR * 0.55f, hy - headR * 1.1f)
        path.lineTo(hx + headR * 0.55f, hy - headR * 0.1f)
        path.close()
        canvas.drawPath(path, paint)

        // Eye
        val eyeColor = mixArgb(0xFF5A3A1E.toInt(), palette.colorAt(1), 0.3f)
        paint.color = Color.argb(166, Color.red(eyeColor), Color.green(eyeColor), Color.blue(eyeColor))
        canvas.drawCircle(hx - headR * 0.35f, hy - headR * 0.1f, headR * 0.09f, paint)
        // Nose
        paint.color = 0xFF0E0A08.toInt()
        canvas.drawCircle(hx - headR * 1.0f, hy + headR * 0.02f, headR * 0.07f, paint)

        // Legs
        paint.color = mixArgb(0xFF2A1C12.toInt(), palette.colorAt(0), 0.28f)
        paint.strokeWidth = minDim * 0.028f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        path.reset()
        path.moveTo(cx - bodyRx * 0.55f, bodyY)
        path.lineTo(cx - bodyRx * 0.7f, bodyY + minDim * (0.16f + 0.04f * amount))
        path.moveTo(cx - bodyRx * 0.25f, bodyY)
        path.lineTo(cx - bodyRx * 0.35f, bodyY + minDim * (0.15f + 0.035f * amount))
        path.moveTo(cx + bodyRx * 0.45f, bodyY - bodyRy * 0.3f)
        path.lineTo(cx + bodyRx * 0.55f, bodyY + minDim * 0.08f)
        path.moveTo(cx + bodyRx * 0.75f, bodyY - bodyRy * 0.2f)
        path.lineTo(cx + bodyRx * 0.85f, bodyY + minDim * 0.07f)
        canvas.drawPath(path, paint)

        // Tail
        path.reset()
        val tailBaseX = cx + bodyRx * 0.95f
        val tailBaseY = bodyY - bodyRy
        path.moveTo(tailBaseX, tailBaseY)
        path.quadTo(
            tailBaseX + minDim * 0.05f, tailBaseY - minDim * 0.08f,
            tailBaseX + minDim * 0.02f, tailBaseY - minDim * 0.14f,
        )
        paint.strokeWidth = minDim * 0.025f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // Dust motes in sunbeam
        paint.color = Color.argb(35, Color.red(sunColor), Color.green(sunColor), Color.blue(sunColor))
        for (i in 0 until 4) {
            val mx = w * (0.15f + 0.1f * i)
            val my = h * (0.17f + 0.08f * i)
            canvas.drawCircle(mx, my, minDim * 0.006f, paint)
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
