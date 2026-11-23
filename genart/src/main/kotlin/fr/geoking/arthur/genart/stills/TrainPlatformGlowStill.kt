package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.RectF
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of the quiet, lamp-lit station platform. */
internal object TrainPlatformGlowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val floor = Path()
    private val edge = Path()

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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val warm = mixArgb(0xFFFFE0A0.toInt(), palette.colorAt(0), 0.18f)
        val horizon = h * 0.48f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF101418.toInt(), 0xFF1A2028.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Low station silhouette dissolving into a dim haze.
        paint.shader = LinearGradient(
            0f, horizon - h * 0.11f, 0f, horizon + h * 0.08f,
            intArrayOf(Color.TRANSPARENT, 0xB8080A0C.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.11f, w, horizon + h * 0.08f, paint)
        paint.shader = null

        // Perspective platform floor.
        floor.reset()
        floor.moveTo(w * 0.39f, horizon)
        floor.lineTo(w * 0.61f, horizon)
        floor.lineTo(w * 1.14f, h)
        floor.lineTo(-w * 0.14f, h)
        floor.close()
        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(0xFF1B2229.toInt(), 0xFF12171C.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(floor, paint)
        paint.shader = null

        // Track shadow and tactile platform edge.
        edge.reset()
        edge.moveTo(w * 0.54f, horizon + h * 0.03f)
        edge.lineTo(w * 1.04f, h)
        paint.color = 0xFF050709.toInt()
        paint.strokeWidth = minDim * 0.035f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        canvas.drawPath(edge, paint)
        paint.color = withAlpha(0xFFE6C77E.toInt(), (0.34f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.009f
        canvas.drawPath(edge, paint)
        paint.style = Paint.Style.FILL

        // Warm lamp pools and the diminishing lamps above the platform.
        for (i in 0 until 7) {
            val depth = (i + 1f) / 8f
            val y = horizon - h * (0.02f + 0.35f * (1f - depth))
            val perspective = 0.18f + 0.82f * depth * depth
            val x = w * (0.24f + 0.18f * (1f - depth))
            val lampW = minDim * 0.035f * perspective
            val lampH = minDim * 0.012f * perspective
            val radius = minDim * (0.18f + 0.28f * depth)
            val breathe = 0.93f + 0.07f * sin(t01 * 2f * PI.toFloat() + i * 0.8f)

            paint.shader = RadialGradient(
                x + w * 0.23f * depth,
                y + h * 0.28f * perspective,
                radius,
                intArrayOf(
                    withAlpha(warm, (0.18f * dim * breathe * 255).toInt()),
                    withAlpha(warm, (0.055f * dim * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
            rect.set(
                x + w * 0.23f * depth - radius,
                y + h * 0.28f * perspective - radius * 0.38f,
                x + w * 0.23f * depth + radius,
                y + h * 0.28f * perspective + radius * 0.38f,
            )
            canvas.drawOval(rect, paint)
            paint.shader = null

            paint.color = 0xC030343A.toInt()
            rect.set(x - lampW * 0.7f, y, x + lampW * 0.7f, y + lampH * 2.2f)
            canvas.drawRoundRect(rect, lampH, lampH, paint)
            paint.shader = RadialGradient(
                x, y + lampH, lampW * 2.8f,
                intArrayOf(0xD9FFF2D4.toInt(), withAlpha(warm, (0.05f * dim * 255).toInt())),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y + lampH, lampW * 2.8f, paint)
            paint.shader = null
        }

        // Roof edge and a pair of dark support posts.
        paint.color = withAlpha(0xFF07090C.toInt(), (0.68f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.018f
        paint.strokeCap = Paint.Cap.SQUARE
        paint.style = Paint.Style.STROKE
        canvas.drawLine(w * 0.08f, horizon - h * 0.23f, w * 0.48f, horizon - h * 0.04f, paint)
        canvas.drawLine(w * 0.08f, horizon - h * 0.23f, w * 0.08f, h * 0.77f, paint)
        paint.strokeWidth = minDim * 0.008f
        canvas.drawLine(w * 0.45f, horizon - h * 0.035f, w * 0.45f, h * 0.56f, paint)
        paint.style = Paint.Style.FILL

        // Gentle amber haze pools at the far end.
        paint.shader = LinearGradient(
            0f, horizon - h * 0.05f, 0f, horizon + h * 0.19f,
            intArrayOf(Color.TRANSPARENT, withAlpha(warm, (0.06f * wash * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.05f, w, horizon + h * 0.19f, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, blue)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
