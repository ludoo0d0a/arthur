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

/** Bakes one frozen frame of Lucid Orbs for Auto/Ambient album art. */
internal object LucidOrbsStill {
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
        val dim = 0.85f + 0.15f * pulse
        val minDim = minOf(w, h)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0B1020.toInt(), 0xFF121828.toInt(), 0xFF1C2840.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFF80D0FF.toInt(), palette.colorAt(0), 0.18f)
        val violet = 0xFF9F8CFF.toInt()
        val cx = w * (0.35f + 0.3f * loopedFbm(t01, radius = 1.2f, seedOffset = 29))
        val cy = h * (0.3f + 0.25f * loopedFbm(t01, radius = 1.4f, seedOffset = 47))
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.7f,
            intArrayOf(withAlpha(accent, (0.22f * dim * 255).toInt()), withAlpha(violet, (0.08f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.7f, paint)
        paint.shader = null
        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.25f + i * 0.04f, octaves = 3, seedOffset = i * 97 + 5)
            val angle = t01 * 2f * PI.toFloat() + i * (2f * PI.toFloat() / 7f)
            val x = w * (0.12f + i * 0.12f + 0.025f * sin(angle) + 0.018f * (n - 0.5f))
            val y = h * (0.18f + (i % 5) * 0.15f + 0.025f * cos(angle * 0.72f))
            val r = minDim * (0.045f + 0.055f * n)
            val orbColor = if (i % 2 == 0) accent else violet
            val glowRadius = r * (2.5f + 0.8f * n)

            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                x, y, glowRadius,
                intArrayOf(withAlpha(orbColor, (0.16f * dim * 255).toInt()), withAlpha(orbColor, (0.06f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, glowRadius, paint)

            paint.shader = RadialGradient(
                x - r * 0.32f, y - r * 0.35f, r * 1.7f,
                intArrayOf(
                    withAlpha(0xFFE5F5FF.toInt(), ((0.52f + 0.12f * n) * dim * 255).toInt()),
                    withAlpha(orbColor, (0.32f * dim * 255).toInt()),
                    withAlpha(orbColor, (0.13f * dim * 255).toInt()),
                    withAlpha(orbColor, (0.38f * dim * 255).toInt()),
                ),
                floatArrayOf(0f, 0.22f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r, paint)
            paint.shader = null

            paint.color = withAlpha(0xFFDFF7FF.toInt(), ((0.36f + 0.2f * n) * dim * 255).toInt())
            canvas.drawCircle(x - r * 0.32f, y - r * 0.36f, r * 0.11f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.0015f
            paint.color = withAlpha(0xFFD7F3FF.toInt(), (0.17f * dim * 255).toInt())
            canvas.drawCircle(x, y, r * 0.97f, paint)
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

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
