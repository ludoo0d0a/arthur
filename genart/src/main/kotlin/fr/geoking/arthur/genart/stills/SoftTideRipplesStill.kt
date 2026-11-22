package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI

/** Bakes one frozen frame of Soft Tide Ripples for Auto/Ambient album art. */
internal object SoftTideRipplesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val wash = loopedFbm(t01, radius = 1.3f, octaves = 2, seedOffset = 19)
        val dim = 0.85f + 0.15f * pulse
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0C2430.toInt(), 0xFF061018.toInt(), 0xFF03080C.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val accent = mixArgb(0xFFD8ECF4.toInt(), palette.colorAt(0), 0.4f)
        val horizon = h * (0.38f + 0.03f * wash)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.04f, 0f, horizon + h * 0.08f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.18f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.04f, w, horizon + h * 0.08f, paint)
        paint.shader = null

        val minDim = minOf(w, h)
        paint.style = Paint.Style.STROKE
        for (i in 0 until 5) {
            val life = ((t01 * (0.5f + i * 0.07f) + i * 0.17f) % 1f)
            val cx = w * (0.3f + (i % 3) * 0.18f)
            val cy = h * (0.45f + (i % 4) * 0.08f)
            val radius = life * (0.2f + i * 0.03f) * minDim
            if (radius <= 0.5f) continue
            val alpha = (1f - life) * (0.14f + i * 0.02f) * dim
            val stretch = 1.4f + i * 0.08f
            val tint = mixArgb(palette.colorAt(i), accent, 0.35f)
            paint.strokeWidth = (2.4f - life * 1.4f).coerceAtLeast(0.6f)
            paint.color = withAlpha(tint, (alpha * 255).toInt())
            canvas.save()
            canvas.scale(stretch, 1f, cx, cy)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.color = withAlpha(accent, (alpha * 0.45f * 255).toInt())
            paint.strokeWidth *= 0.55f
            canvas.drawCircle(cx, cy, radius * 0.92f, paint)
            canvas.restore()
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
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
