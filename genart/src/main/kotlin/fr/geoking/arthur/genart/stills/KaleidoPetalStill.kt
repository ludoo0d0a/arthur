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

/** Bakes one frozen frame of Kaleido Petal for Auto/Ambient album art. */
internal object KaleidoPetalStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val petal = Path()

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
        val cx = w * 0.5f
        val cy = h * 0.5f
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val motion = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = 183)
        val brightness = (0.96f + 0.04f * sin(t01 * 2f * PI.toFloat())) * pulse.coerceIn(0.75f, 1.25f)
        val dark = brightnessArgb(0xFF140C18.toInt(), brightness)
        val plum = brightnessArgb(0xFF2A1830.toInt(), brightness)
        val rose = brightnessArgb(mixArgb(0xFFFF80C0.toInt(), palette.colorAt(0), 0.22f), brightness)
        val lavender = brightnessArgb(0xFFB080D0.toInt(), brightness)
        val rotation = rotationDeg + 4f * sin(t01 * 2f * PI.toFloat())
        val outerRadius = minDim * 0.43f * (0.95f + 0.04f * sin(t01 * 2f * PI.toFloat()) + 0.02f * motion)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, 0f, h, dark, plum, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.68f,
            intArrayOf(withAlpha(rose, 0x3D), withAlpha(lavender, 0x1A), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.68f, paint)
        paint.shader = null

        petal.reset()
        petal.moveTo(cx, cy - minDim * 0.055f)
        petal.cubicTo(
            cx - minDim * 0.12f, cy - minDim * 0.20f,
            cx - minDim * 0.12f, cy - outerRadius * 0.76f,
            cx, cy - outerRadius,
        )
        petal.cubicTo(
            cx + minDim * 0.12f, cy - outerRadius * 0.76f,
            cx + minDim * 0.12f, cy - minDim * 0.20f,
            cx, cy - minDim * 0.055f,
        )
        petal.close()

        for (i in 0 until 8) {
            canvas.save()
            canvas.rotate(rotation + i * 45f, cx, cy)
            paint.shader = LinearGradient(
                cx, cy - outerRadius, cx, cy,
                intArrayOf(withAlpha(rose, 0x14), withAlpha(lavender, 0x52), withAlpha(rose, 0x8F)),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(petal, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.0025f
            paint.color = withAlpha(rose, 0x73)
            canvas.drawPath(petal, paint)
            paint.style = Paint.Style.FILL

            paint.color = withAlpha(0xFFFFC0E0.toInt(), 0x42)
            paint.strokeWidth = minDim * 0.0018f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(cx, cy - minDim * 0.09f, cx, cy - outerRadius * 0.82f, paint)
            paint.color = withAlpha(lavender, 0x2E)
            paint.strokeWidth = minDim * 0.0014f
            for (v in 1..6) {
                val fraction = v / 7f
                val y = cy - minDim * (0.12f + fraction * 0.25f)
                val spread = minDim * (0.015f + fraction * 0.045f)
                canvas.drawLine(cx, y, cx - spread, y - minDim * 0.018f, paint)
                canvas.drawLine(cx, y, cx + spread, y - minDim * 0.018f, paint)
            }
            paint.style = Paint.Style.FILL
            canvas.restore()
        }

        paint.style = Paint.Style.STROKE
        for (ring in 1..3) {
            paint.color = withAlpha(rose, ((0.20f - ring * 0.035f) * 255).toInt())
            paint.strokeWidth = minDim * 0.002f
            canvas.drawCircle(cx, cy, minDim * (0.095f + ring * 0.035f), paint)
        }
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.075f,
            intArrayOf(withAlpha(0xFFFFD0E8.toInt(), 0xE0), withAlpha(rose, 0x8F), withAlpha(plum, 0x05)),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.075f, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun brightnessArgb(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(argb), r, g, b)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
