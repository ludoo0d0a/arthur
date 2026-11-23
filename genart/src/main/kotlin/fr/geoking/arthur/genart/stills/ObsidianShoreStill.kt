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

/** Bakes one frozen frame of Obsidian Shore for Auto/Ambient album art. */
internal object ObsidianShoreStill {
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
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val wash = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val horizon = h * 0.57f
        val shoreLine = h * (0.79f + 0.012f * (wash - 0.5f))
        val moon = mixArgb(0xFF6080A0.toInt(), palette.colorAt(0), 0.22f)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF07090D.toInt(), 0xFF0A0C10.toInt(), 0xFF11151D.toInt(), 0xFF080A0E.toInt()),
            floatArrayOf(0f, 0.52f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.72f, horizon + h * 0.01f, minDim * 0.47f,
            intArrayOf(withAlpha(moon, 0.2f * dim), withAlpha(moon, 0.08f * dim), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.72f, horizon + h * 0.01f, minDim * 0.47f, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(0xFF101722.toInt(), 0xFF0B1119.toInt(), 0xFF080B10.toInt()),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon, w, h, paint)
        paint.shader = null

        // Broken silver-blue reflection across the dark water.
        for (i in 0 until 8) {
            val y = horizon + h * (0.05f + i * 0.035f) + h * 0.006f * sin(t01 * 2f * PI.toFloat() + i)
            val halfWidth = minDim * (0.06f + 0.045f * (1f - i / 8f))
            val shift = minDim * 0.035f * sin(t01 * 2f * PI.toFloat() + i * 0.7f)
            paint.color = withAlpha(moon, (0.16f - i * 0.012f) * dim)
            paint.strokeWidth = minDim * 0.003f
            canvas.drawLine(w * 0.72f + shift - halfWidth, y, w * 0.72f + shift + halfWidth, y, paint)
        }

        // Irregular shoreline transitions into a deep charcoal beach.
        val beach = Path().apply {
            moveTo(0f, shoreLine)
            for (i in 0..12) {
                val x = w * i / 12f
                val n = loopedFbm(t01, radius = 1.6f, octaves = 2, seedOffset = 30 + i * 17)
                lineTo(x, shoreLine - h * (0.018f + 0.024f * n))
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        paint.shader = LinearGradient(
            0f, shoreLine - h * 0.04f, 0f, h,
            intArrayOf(0xFF141923.toInt(), 0xFF1A1E28.toInt(), 0xFF0A0C10.toInt()),
            floatArrayOf(0f, 0.18f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(beach, paint)
        paint.shader = null
        paint.color = withAlpha(moon, 0.2f * dim)
        paint.strokeWidth = minDim * 0.0025f
        canvas.drawLine(0f, shoreLine - h * 0.025f, w, shoreLine - h * 0.025f, paint)

        // Glossy, angular volcanic stones catch a faint rim of moonlight.
        for (i in 0 until 8) {
            val x = w * (0.04f + i * 0.13f)
            val rw = minDim * (0.07f + 0.035f * loopedFbm(t01, radius = 1.2f, seedOffset = i * 23 + 5))
            val rh = minDim * (0.045f + 0.045f * loopedFbm(t01, radius = 1.4f, seedOffset = i * 31 + 9))
            val baseY = shoreLine + h * (0.11f + (i % 3) * 0.025f)
            val rock = Path().apply {
                moveTo(x - rw * 0.5f, baseY)
                lineTo(x - rw * 0.36f, baseY - rh * 0.62f)
                lineTo(x - rw * 0.05f, baseY - rh)
                lineTo(x + rw * 0.32f, baseY - rh * 0.78f)
                lineTo(x + rw * 0.5f, baseY - rh * 0.22f)
                lineTo(x + rw * 0.28f, baseY + rh * 0.08f)
                close()
            }
            paint.color = if (i % 2 == 0) 0xFF080A0E.toInt() else 0xFF11151C.toInt()
            canvas.drawPath(rock, paint)

            val facet = Path().apply {
                moveTo(x - rw * 0.05f, baseY - rh)
                lineTo(x + rw * 0.32f, baseY - rh * 0.78f)
                lineTo(x + rw * 0.5f, baseY - rh * 0.22f)
                lineTo(x + rw * 0.1f, baseY - rh * 0.12f)
                close()
            }
            paint.color = withAlpha(moon, 0.14f * dim)
            canvas.drawPath(facet, paint)
            paint.color = 0xFF252C38.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.002f
            canvas.drawPath(rock, paint)
            paint.style = Paint.Style.FILL
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

    private fun withAlpha(rgb: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
