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

/** Bakes a quiet dawn road frame for Moto Dawn Ride. */
internal object MotoDawnRideStill {
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
        val minDim = minOf(w, h)
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val cloud = loopedFbm(t, radius = 1.25f, octaves = 3, seedOffset = 31)
        val horizon = h * (0.48f + 0.018f * (cloud - 0.5f))
        val cx = w * 0.5f
        val dim = 0.85f + 0.15f * pulse
        val dawn = mixArgb(0xFFFFB080.toInt(), palette.colorAt(0), 0.18f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, horizon * 1.2f,
            intArrayOf(brightness(0xFF2A2030.toInt(), dim), brightness(0xFF4A3848.toInt(), dim)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Broad peach light bleeding softly above the horizon.
        paint.shader = RadialGradient(
            cx, horizon * 0.94f, minDim * 0.60f,
            intArrayOf(withAlpha(dawn, (0.45f * dim * 255).toInt()), withAlpha(dawn, (0.20f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, horizon * 0.94f, minDim * 0.60f, paint)
        paint.shader = null

        // Low distant ridges.
        path.reset()
        path.moveTo(0f, horizon * 0.93f)
        path.cubicTo(w * 0.18f, horizon * 0.82f, w * 0.27f, horizon * 1.04f, w * 0.45f, horizon * 0.91f)
        path.cubicTo(w * 0.64f, horizon * 0.78f, w * 0.78f, horizon * 1.05f, w, horizon * 0.88f)
        path.lineTo(w, horizon * 1.13f)
        path.lineTo(0f, horizon * 1.13f)
        path.close()
        paint.color = withAlpha(0xFF4A3848.toInt(), (0.70f * dim * 255).toInt())
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(0f, horizon * 1.04f)
        path.cubicTo(w * 0.20f, horizon * 0.96f, w * 0.34f, horizon * 1.15f, w * 0.52f, horizon * 1.02f)
        path.cubicTo(w * 0.70f, horizon * 0.91f, w * 0.84f, horizon * 1.12f, w, horizon * 0.99f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = withAlpha(0xFF0A0C10.toInt(), (0.88f * dim * 255).toInt())
        canvas.drawPath(path, paint)

        // Tapered road and warm edge reflections.
        path.reset()
        path.moveTo(cx - w * 0.018f, horizon * 1.01f)
        path.lineTo(cx + w * 0.018f, horizon * 1.01f)
        path.lineTo(w * 0.98f, h)
        path.lineTo(w * 0.02f, h)
        path.close()
        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(withAlpha(0xFF080A0C.toInt(), (0.80f * dim * 255).toInt()), 0xFF080A0C.toInt(), 0xFF111117.toInt()),
            floatArrayOf(0f, 0.34f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.006f
        paint.color = withAlpha(dawn, (0.16f * dim * 255).toInt())
        canvas.drawLine(cx - w * 0.018f, horizon, w * 0.035f, h, paint)
        canvas.drawLine(cx + w * 0.018f, horizon, w * 0.965f, h, paint)
        paint.style = Paint.Style.FILL

        // Perspective center dashes use the still phase, matching the engine's slow flow.
        for (i in 0 until 7) {
            val depth = ((i / 7f + t * 0.46f) % 1f)
            val y = horizon + (h - horizon) * (0.035f + depth * depth * 0.94f)
            val scale = 0.25f + depth * depth * 3.2f
            val dashW = minDim * 0.006f * scale
            val dashH = minDim * 0.026f * scale
            paint.color = withAlpha(dawn, ((0.17f + 0.31f * depth) * dim * 255).toInt())
            canvas.drawRoundRect(cx - dashW * 0.5f, y, cx + dashW * 0.5f, y + dashH, dashW * 0.45f, dashW * 0.45f, paint)
        }

        // Fairing silhouette and its faint dawn rim.
        path.reset()
        path.moveTo(0f, h * 0.91f)
        path.cubicTo(w * 0.18f, h * 0.88f, w * 0.28f, h * 0.95f, cx, h * 0.92f)
        path.cubicTo(w * 0.72f, h * 0.95f, w * 0.84f, h * 0.88f, w, h * 0.91f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = withAlpha(0xFF080A0C.toInt(), (0.90f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.004f
        paint.color = withAlpha(dawn, (0.13f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }

    private fun brightness(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255),
    )

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
