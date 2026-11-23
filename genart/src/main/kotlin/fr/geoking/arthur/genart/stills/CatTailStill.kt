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

/** Bakes one frozen frame of Cat Tail Sway for Auto/Ambient album art. */
internal object CatTailStill {
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
        val cx = w * 0.48f
        val cy = h * 0.58f

        // Deep moody twilight room
        paint.shader = RadialGradient(
            cx, cy, w.coerceAtLeast(h) * 0.85f,
            intArrayOf(0xFF2A2430.toInt(), 0xFF1C1822.toInt(), 0xFF100E14.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Ambient light pool on floor
        paint.shader = RadialGradient(
            cx, h * 0.78f, minDim * 0.42f,
            intArrayOf(
                Color.argb(77, 0x3A, 0x2E, 0x3A),
                Color.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - minDim * 0.42f, h * 0.68f, cx + minDim * 0.42f, h * 0.88f, paint)
        paint.shader = null

        // Twilight window glow
        paint.shader = RadialGradient(
            w * 0.72f, h * 0.2f, w * 0.35f,
            intArrayOf(Color.argb(56, 0x4A, 0x38, 0x58), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h * 0.5f, paint)
        paint.shader = null

        // Cat body
        paint.color = mixArgb(0xFF1C1614.toInt(), palette.colorAt(0), 0.25f)
        val bodyRx = minDim * 0.16f
        val bodyRy = minDim * 0.20f
        rect.set(cx - bodyRx, cy - bodyRy, cx + bodyRx, cy + bodyRy)
        canvas.drawOval(rect, paint)

        // Head
        val headR = minDim * 0.11f
        val hx = cx
        val hy = cy - bodyRy * 1.15f
        canvas.drawCircle(hx, hy, headR, paint)

        // Ears
        path.reset()
        path.moveTo(hx - headR * 0.7f, hy - headR * 0.2f)
        path.lineTo(hx - headR * 0.95f, hy - headR * 1.35f)
        path.lineTo(hx - headR * 0.15f, hy - headR * 0.5f)
        path.close()
        path.moveTo(hx + headR * 0.15f, hy - headR * 0.5f)
        path.lineTo(hx + headR * 0.95f, hy - headR * 1.35f)
        path.lineTo(hx + headR * 0.7f, hy - headR * 0.2f)
        path.close()
        canvas.drawPath(path, paint)

        // Eye glints
        val eyeColor = mixArgb(0xFF8A9AB0.toInt(), palette.colorAt(1), 0.3f)
        paint.color = Color.argb(140, Color.red(eyeColor), Color.green(eyeColor), Color.blue(eyeColor))
        canvas.drawCircle(hx - headR * 0.3f, hy + headR * 0.05f, headR * 0.08f, paint)
        canvas.drawCircle(hx + headR * 0.3f, hy + headR * 0.05f, headR * 0.08f, paint)

        // Front paws
        paint.color = mixArgb(0xFF1C1614.toInt(), palette.colorAt(0), 0.25f)
        rect.set(cx - bodyRx * 0.55f, cy + bodyRy * 0.55f, cx - bodyRx * 0.55f + minDim * 0.065f, cy + bodyRy * 0.55f + minDim * 0.055f)
        canvas.drawOval(rect, paint)
        rect.set(cx + bodyRx * 0.15f, cy + bodyRy * 0.55f, cx + bodyRx * 0.15f + minDim * 0.065f, cy + bodyRy * 0.55f + minDim * 0.055f)
        canvas.drawOval(rect, paint)

        // Tail
        val sway = sin(phase) * pulse
        path.reset()
        val baseX = cx + bodyRx * 0.75f
        val baseY = cy + bodyRy * 0.15f
        path.moveTo(baseX, baseY)
        path.quadTo(
            baseX + minDim * (0.12f + 0.04f * sway),
            baseY - minDim * 0.18f,
            baseX + minDim * (0.08f + 0.12f * sway),
            baseY - minDim * 0.38f,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.032f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // Dust motes
        val moteGlow = mixArgb(0xFFD0C8E0.toInt(), palette.colorAt(1), 0.2f)
        paint.color = Color.argb(45, Color.red(moteGlow), Color.green(moteGlow), Color.blue(moteGlow))
        for (i in 0 until 4) {
            val mx = w * (0.2f + 0.15f * i)
            val my = h * (0.22f + 0.1f * i)
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
