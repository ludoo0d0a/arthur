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

/** Bakes one frozen frame of Velo Golden Hour for Auto/Ambient album art. */
internal object VeloGoldenHourStill {
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
        val wash = loopedFbm(t, radius = 1.2f, octaves = 3, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val horizon = h * (0.49f + 0.025f * (wash - 0.5f))

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFAA6030.toInt(), 0xFFC87830.toInt(), 0xFF6A4018.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val sun = mixArgb(0xFFFFD080.toInt(), palette.colorAt(0), 0.15f)
        paint.shader = RadialGradient(
            w * 0.72f, horizon + h * 0.015f, minDim * 0.48f,
            intArrayOf(withAlpha(sun, (0x66 * dim).toInt()), withAlpha(sun, (0x22 * dim).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.36f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.72f, horizon + h * 0.015f, minDim * 0.48f, paint)
        paint.shader = null

        // Three gently rolling amber field layers.
        val fieldColors = intArrayOf(0x77B97835, 0xCC8B5422.toInt(), 0xF2503117.toInt())
        val rises = floatArrayOf(0.04f, 0.10f, 0.17f)
        for (i in rises.indices) {
            val y = horizon + h * rises[i]
            path.reset()
            path.moveTo(0f, y)
            path.cubicTo(w * 0.28f, y - h * 0.04f, w * 0.66f, y + h * 0.035f, w, y - h * 0.012f)
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            paint.color = fieldColors[i]
            canvas.drawPath(path, paint)
        }

        // Perspective road, narrowing into the golden haze.
        path.reset()
        path.moveTo(w * (0.47f + 0.012f * (wash - 0.5f)), horizon + h * 0.012f)
        path.cubicTo(w * 0.40f, h * 0.67f, w * 0.20f, h * 0.82f, w * 0.08f, h)
        path.lineTo(w * 0.92f, h)
        path.cubicTo(w * 0.80f, h * 0.82f, w * 0.60f, h * 0.67f, w * (0.53f + 0.012f * (wash - 0.5f)), horizon + h * 0.012f)
        path.close()
        paint.color = 0xFF251B14.toInt()
        canvas.drawPath(path, paint)

        // Warm edge lights and broken center markings.
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.004f
        paint.color = withAlpha(sun, (0x70 * dim).toInt())
        path.reset()
        path.moveTo(w * 0.465f, horizon + h * 0.018f)
        path.cubicTo(w * 0.38f, h * 0.65f, w * 0.17f, h * 0.84f, w * 0.075f, h)
        canvas.drawPath(path, paint)
        paint.strokeWidth = minDim * 0.003f
        paint.color = withAlpha(sun, (0x55 * dim).toInt())
        path.reset()
        path.moveTo(w * 0.535f, horizon + h * 0.018f)
        path.cubicTo(w * 0.62f, h * 0.65f, w * 0.83f, h * 0.84f, w * 0.925f, h)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        for (i in 0 until 7) {
            val p = (i + 1) / 8f
            val y = horizon + (h - horizon) * p * p
            val halfWidth = minDim * (0.002f + 0.009f * p)
            paint.color = withAlpha(sun, ((0.46f - 0.20f * p) * dim * 255).toInt())
            canvas.drawRect(w * 0.5f - halfWidth, y, w * 0.5f + halfWidth, y + minDim * (0.003f + p * 0.004f), paint)
        }

        // Sparse foreground grass catches the low sun.
        for (i in 0 until 15) {
            val u = (i + 0.5f) / 15f
            val side = if (i % 2 == 0) -1f else 1f
            val x = if (side < 0f) u * w * 0.28f else w * (0.72f + u * 0.28f)
            val y = horizon + h * (0.19f + u * 0.30f)
            paint.color = withAlpha(sun, (0x42 * dim).toInt())
            paint.strokeWidth = minDim * 0.0018f
            canvas.drawLine(x, y, x + side * minDim * 0.008f, y - minDim * (0.018f + (i % 4) * 0.012f), paint)
        }
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
