package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.BlurMaskFilter
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Avion Contrail for Auto/Ambient album art. */
internal object AvionContrailStill {
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
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF527FAF.toInt(), 0xFF8FB4D8.toInt(), 0xFFB8D4EC.toInt(), 0xFF829FBD.toInt()),
            floatArrayOf(0f, 0.36f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFFF0F6FC.toInt(), palette.colorAt(0), 0.35f)
        val cloudDrift = (loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = 37) - 0.5f) * w * 0.025f
        for (i in 0..3) {
            val y = h * (0.56f + i * 0.105f) + (loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 51 + i * 13) - 0.5f) * h * 0.025f
            paint.shader = LinearGradient(
                -w * 0.15f + cloudDrift, y, w * 1.1f + cloudDrift, y,
                intArrayOf(Color.TRANSPARENT, withAlpha(0xFFF0F6FC.toInt(), (0.10f * dim * 255).toInt()), withAlpha(0xFFE4EFF9.toInt(), (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.2f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(-w * 0.08f + cloudDrift, y, w * 1.08f + cloudDrift, y + h * (0.10f + i * 0.012f), paint)
        }
        paint.shader = null
        val horizon = h * (0.52f + 0.025f * (wash - 0.5f))
        paint.shader = LinearGradient(
            0f, horizon - h * 0.08f, 0f, horizon + h * 0.08f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.18f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.08f, w, horizon + h * 0.08f, paint)
        paint.shader = null
        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.2f + i * 0.04f, seedOffset = i * 97 + 5)
            val x = ((0.1f + i * 0.12f + t01 * 0.05f) % 1f) * w
            val y = ((0.2f + (i % 4) * 0.15f) + 0.01f * sin(t01 * 2f * PI.toFloat() + i)) * h
            paint.color = withAlpha(0xFFEAF4FC.toInt(), ((0.10f + 0.14f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, minOf(w, h) * (0.0015f + 0.002f * n), paint)
        }

        val bend = (loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 83) - 0.5f) * h * 0.035f
        val trail = Path().apply {
            moveTo(w * 0.08f, h * 0.29f + bend * 0.45f)
            cubicTo(w * 0.35f, h * 0.24f + bend, w * 0.63f, h * 0.35f - bend * 0.35f, w * 0.94f, h * 0.27f - bend * 0.15f)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(0xFFF0F6FC.toInt(), (0.14f * dim * 255).toInt())
        paint.strokeWidth = minOf(w, h) * 0.042f
        paint.maskFilter = BlurMaskFilter(minOf(w, h) * 0.018f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(trail, paint)
        paint.maskFilter = null
        paint.color = withAlpha(0xFFF0F6FC.toInt(), (0.38f * dim * 255).toInt())
        paint.strokeWidth = minOf(w, h) * 0.014f
        canvas.drawPath(trail, paint)
        paint.color = withAlpha(Color.WHITE, (0.78f * dim * 255).toInt())
        paint.strokeWidth = minOf(w, h) * 0.0035f
        canvas.drawPath(trail, paint)
        paint.style = Paint.Style.FILL
        for (i in 0 until 5) {
            val f = (i + 1f) / 6f
            val x = w * (0.12f + f * 0.74f)
            val y = h * (0.29f + 0.015f * sin(f * 2f * PI.toFloat()) + bend / h)
            paint.color = withAlpha(0xFFF0F6FC.toInt(), (0.13f * dim * 255).toInt())
            paint.strokeWidth = minOf(w, h) * 0.003f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(x, y, x + w * 0.045f, y - h * 0.018f, paint)
            paint.style = Paint.Style.FILL
        }
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
