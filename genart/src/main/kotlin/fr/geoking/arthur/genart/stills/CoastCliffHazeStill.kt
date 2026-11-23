package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Coast Cliff Haze for Auto/Ambient album art. */
internal object CoastCliffHazeStill {
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
        val dim = 0.85f + 0.15f * pulse
        val drift = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 19)
        val haze = loopedFbm(t01, radius = 1.6f, octaves = 2, seedOffset = 47)
        val accent = mixArgb(0xFFC8D4E0.toInt(), palette.colorAt(0), 0.28f)
        val horizon = h * 0.52f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF6A7A88.toInt(), 0xFF3A4854.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Distant headlands sit low in the haze.
        path.reset()
        path.moveTo(0f, horizon + h * 0.02f)
        path.cubicTo(w * 0.12f, horizon - h * 0.23f, w * 0.24f, horizon - h * 0.16f, w * 0.37f, horizon - h * 0.04f)
        path.cubicTo(w * 0.52f, horizon - h * 0.20f, w * 0.68f, horizon - h * 0.24f, w * 0.82f, horizon - h * 0.08f)
        path.cubicTo(w * 0.91f, horizon - h * 0.01f, w * 0.96f, horizon - h * 0.03f, w, horizon - h * 0.09f)
        path.lineTo(w, h * 0.68f)
        path.lineTo(0f, h * 0.68f)
        path.close()
        paint.color = withAlpha(0xFF586570.toInt(), (0.52f * dim * 255).toInt())
        canvas.drawPath(path, paint)

        // Faint distant glow and a soft haze band over the water.
        val glowX = w * (0.53f + (drift - 0.5f) * 0.08f)
        val glowY = horizon + h * 0.01f
        paint.shader = RadialGradient(
            glowX, glowY, minDim * 0.52f,
            intArrayOf(withAlpha(accent, (0.19f * dim * 255).toInt()), withAlpha(accent, (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, minDim * 0.52f, paint)
        paint.shader = null
        paint.shader = LinearGradient(
            0f, horizon - h * 0.12f, 0f, horizon + h * 0.18f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.13f * dim * (0.9f + haze * 0.1f) * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.12f, w, horizon + h * 0.18f, paint)
        paint.shader = null

        // Thin reflected light bands soften the sea.
        for (i in 0 until 7) {
            val f = (i + 1f) / 8f
            val y = horizon + h * (0.06f + f * 0.25f)
            val sway = sin(t01 * 2f * PI.toFloat() + i * 1.3f) * minDim * 0.014f
            paint.color = withAlpha(accent, ((0.07f + 0.10f * (1f - f)) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.0015f + 0.001f * (1f - f))
            canvas.drawLine(w * 0.18f + sway, y, w * 0.84f + sway, y + h * 0.008f, paint)
        }

        // Near cliffs form a quiet frame around the bay.
        path.reset()
        path.moveTo(0f, h * 0.34f)
        path.cubicTo(w * 0.16f, h * 0.36f, w * 0.19f, h * 0.52f, w * 0.35f, h * 0.61f)
        path.cubicTo(w * 0.25f, h * 0.74f, w * 0.20f, h * 0.89f, w * 0.12f, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = withAlpha(0xFF343B43.toInt(), (dim * 255).toInt())
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(w, h * 0.32f)
        path.cubicTo(w * 0.85f, h * 0.39f, w * 0.82f, h * 0.54f, w * 0.66f, h * 0.64f)
        path.cubicTo(w * 0.77f, h * 0.77f, w * 0.81f, h * 0.90f, w * 0.90f, h)
        path.lineTo(w, h)
        path.close()
        canvas.drawPath(path, paint)

        // Cool highlights trace a few rock ledges.
        paint.color = withAlpha(accent, (0.14f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.003f
        for (i in 0..2) {
            val y = h * (0.46f + i * 0.07f)
            canvas.drawLine(w * (0.02f + i * 0.025f), y, w * (0.13f + i * 0.03f), y + h * 0.012f, paint)
            canvas.drawLine(w * (0.98f - i * 0.025f), y + h * 0.01f, w * (0.87f - i * 0.03f), y + h * 0.02f, paint)
        }

        // Subtle horizontal mist feathers drift across the open water.
        for (i in 0..2) {
            val cy = horizon + h * (0.12f + i * 0.055f)
            val shift = (drift - 0.5f) * w * (0.12f + i * 0.03f)
            paint.shader = RadialGradient(
                w * 0.5f + shift, cy, minDim * (0.34f + i * 0.06f),
                intArrayOf(withAlpha(accent, ((0.08f - i * 0.015f) * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(w * 0.5f + shift - minDim * 0.34f, cy - minDim * 0.10f, w * 0.5f + shift + minDim * 0.34f, cy + minDim * 0.10f, paint)
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

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
