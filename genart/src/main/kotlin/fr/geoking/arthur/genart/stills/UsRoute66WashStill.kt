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

/** Bakes one frozen frame of US Route 66 Desert Wash for Auto/Ambient album art. */
internal object UsRoute66WashStill {
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
        val m = minOf(w, h)
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = loopedFbm(t, radius = 1.1f, octaves = 2, seedOffset = 23) - 0.5f
        val dim = 0.82f + 0.18f * pulse
        val dawn = mixArgb(0xFFFFD080.toInt(), palette.colorAt(0), 0.22f)
        val horizon = h * (0.52f + 0.012f * drift)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(mixArgb(0xFF342B32.toInt(), 0xFFC89050.toInt(), 0.36f + 0.04f * wash), 0xFFC89050.toInt(), 0xFF6A4020.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * (0.51f + drift * 0.08f), horizon - h * 0.07f, m * 0.48f,
            intArrayOf(withAlpha(dawn, (0.30f * dim * 255).toInt()), withAlpha(0xFFC89050.toInt(), (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * (0.51f + drift * 0.08f), horizon - h * 0.07f, m * 0.48f, paint)
        paint.shader = null

        path.reset()
        path.moveTo(0f, horizon + h * 0.04f)
        path.lineTo(0f, horizon - h * 0.05f)
        path.lineTo(w * 0.10f, horizon - h * 0.06f)
        path.lineTo(w * 0.17f, horizon - h * 0.12f)
        path.lineTo(w * 0.26f, horizon - h * 0.12f)
        path.lineTo(w * 0.34f, horizon - h * 0.04f)
        path.lineTo(w * 0.47f, horizon - h * 0.08f)
        path.lineTo(w * 0.57f, horizon - h * 0.14f)
        path.lineTo(w * 0.68f, horizon - h * 0.13f)
        path.lineTo(w * 0.78f, horizon - h * 0.04f)
        path.lineTo(w * 0.88f, horizon - h * 0.10f)
        path.lineTo(w, horizon - h * 0.07f)
        path.lineTo(w, horizon + h * 0.05f)
        path.close()
        paint.color = withAlpha(0xFF815234.toInt(), (0.74f * dim * 255).toInt())
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(0f, horizon + h * 0.17f)
        path.lineTo(0f, horizon + h * 0.02f)
        path.lineTo(w * 0.14f, horizon + h * 0.06f)
        path.lineTo(w * 0.25f, horizon)
        path.lineTo(w * 0.36f, horizon + h * 0.08f)
        path.lineTo(w * 0.52f, horizon + h * 0.03f)
        path.lineTo(w * 0.64f, horizon + h * 0.10f)
        path.lineTo(w * 0.79f, horizon + h * 0.01f)
        path.lineTo(w, horizon + h * 0.04f)
        path.lineTo(w, horizon + h * 0.19f)
        path.close()
        paint.color = withAlpha(0xFF6A4020.toInt(), (0.94f * dim * 255).toInt())
        canvas.drawPath(path, paint)

        paint.shader = LinearGradient(0f, horizon + h * 0.1f, 0f, h, intArrayOf(0x336A4020, 0xFF4A362E.toInt(), 0xFF080A0C.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, horizon + h * 0.1f, w, h, paint)
        paint.shader = null

        // Perspective road.
        path.reset()
        path.moveTo(w * 0.47f, horizon + h * 0.05f)
        path.lineTo(w * 0.53f, horizon + h * 0.05f)
        path.lineTo(w * 0.94f, h)
        path.lineTo(w * 0.06f, h)
        path.close()
        paint.shader = LinearGradient(0f, horizon, 0f, h, intArrayOf(0xFF403934.toInt(), 0xFF080A0C.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.color = withAlpha(dawn, (0.42f * dim * 255).toInt())
        paint.strokeWidth = m * 0.004f
        canvas.drawLine(w * 0.47f, horizon + h * 0.05f, w * 0.06f, h, paint)
        canvas.drawLine(w * 0.53f, horizon + h * 0.05f, w * 0.94f, h, paint)
        for (i in 0..5) {
            val depth = ((i / 6f + t * 0.16f) % 1f)
            val y = horizon + h * 0.07f + depth * depth * (h - horizon)
            val halfW = m * (0.002f + 0.009f * depth)
            paint.color = withAlpha(dawn, ((0.48f + 0.18f * depth) * dim * 255).toInt())
            paint.strokeWidth = h * (0.012f + 0.05f * depth)
            canvas.drawLine(w * 0.5f - halfW, y, w * 0.5f + halfW, y, paint)
        }

        // Small graphic roadside route shield.
        val cx = w * 0.84f
        val cy = horizon - h * 0.015f + drift * m * 0.01f
        val sw = m * 0.075f
        val sh = m * 0.09f
        path.reset()
        path.moveTo(cx - sw * 0.5f, cy - sh * 0.5f)
        path.lineTo(cx + sw * 0.5f, cy - sh * 0.5f)
        path.lineTo(cx + sw * 0.45f, cy + sh * 0.12f)
        path.lineTo(cx, cy + sh * 0.58f)
        path.lineTo(cx - sw * 0.45f, cy + sh * 0.12f)
        path.close()
        paint.color = withAlpha(0xFF201A18.toInt(), (0.84f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = m * 0.002f
        paint.color = withAlpha(dawn, (0.55f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.strokeWidth = m * 0.003f
        canvas.drawLine(cx - sw * 0.22f, cy, cx + sw * 0.22f, cy, paint)
        paint.style = Paint.Style.FILL
        for (i in 0 until 7) {
            val n = loopedFbm(t, radius = 1.2f + i * 0.04f, seedOffset = 91 + i)
            val x = ((i + 1f) / 8f + 0.015f * sin(t * 2f * PI.toFloat() + i)) * w
            val y = horizon + h * (0.08f + (i % 3) * 0.018f)
            paint.color = withAlpha(dawn, ((0.07f + 0.07f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, m * 0.002f, paint)
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
