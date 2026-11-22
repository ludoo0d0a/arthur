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
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Season Spring Wash for Auto/Ambient album art. */
internal object SeasonSpringWashStill {
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
            intArrayOf(0xFFB8E0C8.toInt(), 0xFF6FAE8A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFFF2F7C8.toInt(), palette.colorAt(0), 0.35f)
        val cx = w * (0.35f + 0.3f * loopedFbm(t01, radius = 1.2f, seedOffset = 29))
        val cy = h * (0.3f + 0.25f * loopedFbm(t01, radius = 1.4f, seedOffset = 47))
        paint.shader = RadialGradient(
            cx, cy, minOf(w, h) * 0.55f,
            intArrayOf(withAlpha(accent, (0.35f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minOf(w, h) * 0.55f, paint)
        paint.shader = null
        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.25f + i * 0.04f, seedOffset = i * 97 + 5)
            val x = ((0.1f + i * 0.12f) + 0.04f * (n * 2f - 1f) + 1f) % 1f * w
            val y = ((0.15f + (i % 5) * 0.15f) + 0.03f * sin(t01 * 2f * PI.toFloat() + i) + 1f) % 1f * h
            val r = minOf(w, h) * (0.04f + 0.06f * n)
            paint.color = withAlpha(mixArgb(palette.colorAt(i), accent, 0.4f), ((0.12f + 0.18f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, r, paint)
        }
        when ("seasons") {
            "zen" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = minOf(w, h) * 0.018f
                paint.color = withAlpha(accent, (0.35f * dim * 255).toInt())
                canvas.drawCircle(w * 0.5f, h * 0.52f, minOf(w, h) * (0.18f + 0.02f * wash), paint)
                paint.style = Paint.Style.FILL
            }
            "fire" -> {
                paint.shader = RadialGradient(
                    w * 0.5f, h * 0.72f, minOf(w, h) * 0.35f,
                    intArrayOf(withAlpha(accent, (0.55f * dim * 255).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(w * 0.5f, h * 0.72f, minOf(w, h) * 0.35f, paint)
                paint.shader = null
            }
            "kaleido" -> {
                paint.color = withAlpha(accent, (0.25f * dim * 255).toInt())
                for (i in 0 until 4) {
                    val rr = minOf(w, h) * (0.12f + i * 0.08f)
                    for (k in 0 until 6) {
                        val a = t01 * 2f * PI.toFloat() * 0.2f + k * (2f * PI.toFloat() / 6f)
                        canvas.drawCircle(w * 0.5f + cos(a) * rr, h * 0.5f + sin(a) * rr, rr * 0.12f, paint)
                    }
                }
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
