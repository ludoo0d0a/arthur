package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
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

        // Moonlit bamboo glade background
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF142018.toInt(), 0xFF1C2820.toInt(), 0xFF0C120E.toInt()),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Bamboo silhouettes
        paint.color = 0xFF1A2A18.toInt()
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.012f
        for (i in 0 until 6) {
            val bx = w * (0.05f + i * 0.19f)
            canvas.drawLine(bx, h * 0.95f, bx + sin(phase + i) * minDim * 0.006f, h * 0.02f, paint)
        }
        paint.style = Paint.Style.FILL

        // Moonlight glow
        val moonGlow = mixArgb(0xFFD0D8E8.toInt(), palette.colorAt(1), 0.15f)
        paint.shader = RadialGradient(
            w * 0.2f, h * 0.08f, w * 0.5f,
            intArrayOf(Color.argb(51, Color.red(moonGlow), Color.green(moonGlow), Color.blue(moonGlow)), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.2f, h * 0.08f, w * 0.5f, paint)
        paint.shader = null

        // Grass ground
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
        // Belly shadow
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
        // Eye patches
        rect.set(hx - headR * 0.7f, hy - headR * 0.2f, hx - headR * 0.2f, hy + headR * 0.2f)
        canvas.drawOval(rect, paint)
        rect.set(hx + headR * 0.2f, hy - headR * 0.2f, hx + headR * 0.7f, hy + headR * 0.2f)
        canvas.drawOval(rect, paint)
        // Nose
        canvas.drawCircle(hx, hy + headR * 0.15f, headR * 0.07f, paint)
        // Limbs
        canvas.drawCircle(cx - bodyR * 0.75f, cy + bodyR * 0.35f, bodyR * 0.22f, paint)
        canvas.drawCircle(cx + bodyR * 0.8f, cy - bodyR * 0.15f, bodyR * 0.22f, paint)
        canvas.drawCircle(cx + bodyR * 0.35f, cy + bodyR * 0.75f, bodyR * 0.2f, paint)
        canvas.drawCircle(cx - bodyR * 0.4f, cy + bodyR * 0.72f, bodyR * 0.2f, paint)
        canvas.restore()

        // Fireflies
        paint.color = Color.argb(45, 0xE0, 0xF0, 0xC0)
        for (i in 0 until 4) {
            val mx = w * (0.15f + 0.2f * i)
            val my = h * (0.2f + 0.1f * i)
            canvas.drawCircle(mx, my, minDim * 0.008f, paint)
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
