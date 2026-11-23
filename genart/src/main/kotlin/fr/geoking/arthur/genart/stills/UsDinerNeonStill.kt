package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of US Diner Neon for Auto/Ambient album art. */
internal object UsDinerNeonStill {
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
        val unit = size.toFloat()
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val shimmer = 0.92f + 0.08f * loopedFbm(t01, radius = 1.1f, seedOffset = 23)
        val dim = 0.85f + 0.15f * pulse
        val pink = mixArgb(0xFFFF40A0.toInt(), palette.colorAt(0), 0.2f)
        val cyan = 0xFF4DEBFF.toInt()
        val horizon = h * 0.60f
        val left = w * 0.17f
        val right = w * 0.83f
        val top = h * 0.32f
        val bottom = h * 0.71f

        paint.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF07060D.toInt(), 0xFF100818.toInt(), 0xFF080A10.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.shader = RadialGradient(w * 0.5f, h * 0.57f, unit * 0.72f, intArrayOf(withAlpha(pink, (0.22f * shimmer * dim * 255).toInt()), withAlpha(0xFF50143C.toInt(), (0.10f * dim * 255).toInt()), Color.TRANSPARENT), floatArrayOf(0f, 0.58f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(w * 0.5f, h * 0.57f, unit * 0.72f, paint)
        paint.shader = null

        // Diner facade and inset wall.
        paint.color = 0xFF100D18.toInt()
        rect.set(left, top, right, bottom)
        canvas.drawRoundRect(rect, unit * 0.025f, unit * 0.025f, paint)
        paint.shader = LinearGradient(0f, top, 0f, bottom, intArrayOf(0xFF211326.toInt(), 0xFF16101E.toInt(), 0xFF0C0B12.toInt()), floatArrayOf(0f, 0.36f, 1f), Shader.TileMode.CLAMP)
        rect.set(left + unit * 0.012f, top + unit * 0.012f, right - unit * 0.012f, bottom - unit * 0.012f)
        canvas.drawRect(rect, paint)
        paint.shader = null

        // Neon haze and double tube sign.
        val signLeft = w * 0.30f
        val signTop = h * 0.24f
        val signRight = w * 0.70f
        val signBottom = h * 0.39f
        paint.shader = RadialGradient(w * 0.5f, (signTop + signBottom) * 0.5f, unit * 0.47f, intArrayOf(withAlpha(pink, (0.20f * shimmer * dim * 255).toInt()), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        canvas.drawOval(w * 0.22f, signTop - unit * 0.14f, w * 0.78f, signBottom + unit * 0.30f, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * 0.018f
        paint.color = withAlpha(pink, (0.22f * shimmer * dim * 255).toInt())
        rect.set(signLeft, signTop, signRight, signBottom)
        canvas.drawRoundRect(rect, unit * 0.018f, unit * 0.018f, paint)
        paint.strokeWidth = unit * 0.006f
        paint.color = withAlpha(pink, (0.90f * shimmer * dim * 255).toInt())
        canvas.drawRoundRect(rect, unit * 0.018f, unit * 0.018f, paint)
        rect.inset(unit * 0.012f, unit * 0.012f)
        paint.color = withAlpha(cyan, (0.78f * dim * 255).toInt())
        paint.strokeWidth = unit * 0.003f
        canvas.drawRoundRect(rect, unit * 0.012f, unit * 0.012f, paint)
        paint.style = Paint.Style.FILL
        // Stylized luminous lettering strokes.
        for (i in 0..4) {
            val x = signLeft + (signRight - signLeft) * (0.16f + i * 0.15f)
            val y0 = signTop + (signBottom - signTop) * 0.28f
            val y1 = signTop + (signBottom - signTop) * 0.73f
            paint.color = withAlpha(pink, (0.92f * dim).times(255).toInt())
            canvas.drawLine(x, y0, x, y1, paint.apply { strokeWidth = unit * 0.007f })
            paint.color = withAlpha(cyan, (0.72f * dim * 255).toInt())
            paint.strokeWidth = unit * 0.004f
            canvas.drawLine(x, y0, x + w * 0.026f, y0, paint)
            canvas.drawLine(x, (y0 + y1) * 0.5f, x + w * 0.022f, (y0 + y1) * 0.5f, paint)
            canvas.drawLine(x, y1, x + w * 0.026f, y1, paint)
        }

        // Lit front windows.
        val windowTop = h * 0.45f
        val windowBottom = h * 0.65f
        for (i in 0..2) {
            val x = w * (0.23f + i * 0.20f)
            val ww = w * 0.15f
            paint.shader = LinearGradient(0f, windowTop, 0f, windowBottom, intArrayOf(0xFF27172D.toInt(), 0xFF120F1B.toInt(), 0xFF090C13.toInt()), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            rect.set(x, windowTop, x + ww, windowBottom)
            canvas.drawRoundRect(rect, unit * 0.009f, unit * 0.009f, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = unit * 0.003f
            paint.color = withAlpha(if (i == 1) cyan else pink, (0.76f * dim * 255).toInt())
            canvas.drawRoundRect(rect, unit * 0.009f, unit * 0.009f, paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF080A10.toInt()
            canvas.drawRect(x + ww * 0.48f, windowTop, x + ww * 0.50f, windowBottom, paint)
            paint.color = 0xFF191323.toInt()
            canvas.drawRect(x + ww * 0.08f, windowTop + unit * 0.014f, x + ww * 0.92f, windowTop + unit * 0.018f, paint)
        }
        paint.color = 0xFF0B0A11.toInt()
        canvas.drawRect(left - unit * 0.02f, bottom, right + unit * 0.02f, bottom + h * 0.055f, paint)

        // Rain-dark street and long, soft neon reflections.
        paint.shader = LinearGradient(0f, horizon, 0f, h, intArrayOf(0xFF090A11.toInt(), 0xFF07080D.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, horizon, w, h, paint)
        paint.shader = LinearGradient(0f, horizon, 0f, h, intArrayOf(withAlpha(pink, (0.16f * shimmer * dim * 255).toInt()), withAlpha(pink, (0.035f * dim * 255).toInt()), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(w * 0.2f, horizon, w * 0.8f, h, paint)
        paint.shader = null
        for (i in 0..4) {
            val y = horizon + (i + 1) * h * 0.065f
            val shrink = 1f - i * 0.12f
            paint.color = withAlpha(if (i % 2 == 0) pink else cyan, ((0.16f - i * 0.022f) * shimmer * dim * 255).toInt())
            paint.strokeWidth = unit * (0.003f - i * 0.0003f)
            canvas.drawLine(w * (0.5f - 0.27f * shrink), y, w * (0.5f + 0.27f * shrink), y + h * 0.008f, paint)
        }
        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.2f + i * 0.04f, seedOffset = i * 97 + 5)
            val x = ((0.09f + i * 0.13f) % 1f) * w
            val y = ((0.12f + (i % 5) * 0.14f) + 0.025f * sin(t01 * 2f * PI.toFloat() + i)) * h
            paint.color = withAlpha(cyan, ((0.10f + 0.12f * n) * dim * 255).toInt())
            paint.strokeWidth = unit * 0.0015f
            canvas.drawLine(x, y, x - w * 0.003f, y + h * 0.045f, paint)
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
