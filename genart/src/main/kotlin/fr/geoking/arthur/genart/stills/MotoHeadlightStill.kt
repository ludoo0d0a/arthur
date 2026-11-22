package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Moto Single Headlight for Auto/Ambient album art. */
internal object MotoHeadlightStill {
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
            intArrayOf(0xFF0A0C10.toInt(), 0xFF141820.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFFFFF0C0.toInt(), palette.colorAt(0), 0.35f)
        val horizon = h * (0.55f + 0.04f * (wash - 0.5f))
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
            val x = ((0.1f + i * 0.12f + t01 * 0.15f) % 1f) * w
            val y = ((0.2f + (i % 4) * 0.15f) + 0.03f * sin(t01 * 2f * PI.toFloat() + i)) * h
            paint.color = withAlpha(mixArgb(palette.colorAt(i), accent, 0.4f), ((0.10f + 0.16f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, minOf(w, h) * (0.03f + 0.05f * n), paint)
        }
        when ("moto") {
            "auto", "moto" -> {
                paint.color = withAlpha(0xFF080A0C.toInt(), (0.45f * dim * 255).toInt())
                canvas.drawRect(0f, h * 0.72f, w, h, paint)
                paint.shader = RadialGradient(
                    w * 0.5f, h * 0.62f, minOf(w, h) * 0.25f,
                    intArrayOf(withAlpha(accent, (0.4f * dim * 255).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(w * 0.5f, h * 0.62f, minOf(w, h) * 0.25f, paint)
                paint.shader = null
            }
            "us_vintage" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = minOf(w, h) * 0.02f
                paint.color = withAlpha(accent, (0.4f * dim * 255).toInt())
                canvas.drawCircle(w * 0.5f, h * 0.38f, minOf(w, h) * 0.22f, paint)
                paint.style = Paint.Style.FILL
            }
            else -> Unit
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
