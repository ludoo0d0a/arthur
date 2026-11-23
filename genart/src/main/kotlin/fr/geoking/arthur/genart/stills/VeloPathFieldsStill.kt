package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Velo Path and Fields for Auto/Ambient album art. */
internal object VeloPathFieldsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
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
        val minDim = size.toFloat()
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val horizon = h * (0.49f + 0.025f * (wash - 0.5f))
        val dim = 0.85f + 0.15f * pulse
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF6A9A58.toInt(), 0xFF9AAD73.toInt(), 0xFF3A6040.toInt()),
            floatArrayOf(0f, 0.52f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val accent = mixArgb(0xFFE8E0A0.toInt(), palette.colorAt(0), 0.25f)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.13f, 0f, horizon + h * 0.14f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.23f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.14f, w, horizon + h * 0.14f, paint)
        paint.shader = null

        val bandColors = intArrayOf(0xFF718C55.toInt(), 0xFF557546.toInt(), 0xFF345338.toInt())
        for (layer in 0..2) {
            val y = horizon + h * (0.09f + layer * 0.10f)
            path.reset()
            path.moveTo(0f, y)
            path.cubicTo(w * 0.23f, y - h * 0.055f, w * 0.69f, y + h * 0.04f, w, y - h * 0.02f)
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            paint.color = withAlpha(bandColors[layer], ((0.52f + layer * 0.13f) * dim * 255).toInt())
            canvas.drawPath(path, paint)
        }

        val drift = (loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 61) - 0.5f) * w * 0.035f
        val cx = w * 0.5f + drift
        path.reset()
        path.moveTo(cx - w * 0.016f, horizon + h * 0.045f)
        path.lineTo(w * 0.06f, h)
        path.lineTo(w * 0.39f, h)
        path.lineTo(cx - w * 0.016f, horizon + h * 0.045f)
        path.close()
        paint.color = withAlpha(0xFF6B6948.toInt(), (0.92f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(cx + w * 0.016f, horizon + h * 0.045f)
        path.lineTo(w * 0.61f, h)
        path.lineTo(w * 0.94f, h)
        path.lineTo(cx + w * 0.016f, horizon + h * 0.045f)
        path.close()
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.003f
        paint.color = withAlpha(0xFFD8D39D.toInt(), (0.30f * dim * 255).toInt())
        canvas.drawLine(cx - w * 0.016f, horizon + h * 0.045f, w * 0.06f, h, paint)
        canvas.drawLine(cx + w * 0.016f, horizon + h * 0.045f, w * 0.94f, h, paint)
        for (i in 0..5) {
            val u = ((i / 6f + t01) % 1f)
            val y = horizon + h * 0.08f + u * u * (h - horizon - h * 0.08f)
            val half = w * (0.002f + 0.012f * u)
            paint.color = withAlpha(accent, ((0.4f - u * 0.15f) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.002f + u * 0.005f)
            canvas.drawLine(cx - half, y, cx + half, y, paint)
        }

        for (i in 0 until 16) {
            val x = frac(i * 0.618f + 0.13f) * w
            val baseY = (0.62f + frac(i * 0.327f + 0.21f) * 0.36f) * h
            val height = minDim * (0.015f + frac(i * 0.173f + 0.41f) * 0.055f)
            val sway = sin(t01 * 2f * PI.toFloat() + i * 0.9f) * height * 0.3f
            paint.color = withAlpha(0xFFC7C58A.toInt(), ((0.32f + frac(i * 0.43f) * 0.16f) * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.002f
            canvas.drawLine(x, baseY, x + sway, baseY - height, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun frac(value: Float): Float = value - kotlin.math.floor(value)

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
