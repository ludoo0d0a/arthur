package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Micro Dust for Auto/Ambient album art. */
internal object MicroDustStill {
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
        val dim = 0.85f + 0.15f * pulse
        val haze = loopedFbm(t01, radius = 1.35f, octaves = 3, seedOffset = 11)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                brightness(0xFF18161A.toInt(), 0.92f + 0.08f * haze),
                0xFF111014.toInt(),
                0xFF0C0A10.toInt(),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val accent = mixArgb(0xFFD0C8B0.toInt(), palette.colorAt(0), 0.28f)
        val glowX = w * (0.22f + 0.08f * loopedFbm(t01, radius = 1.1f, seedOffset = 29))
        val glowY = h * (0.28f + 0.08f * loopedFbm(t01, radius = 1.3f, seedOffset = 47))
        val glowRadius = minDim * 0.68f
        paint.shader = RadialGradient(
            glowX, glowY, glowRadius,
            intArrayOf(withAlpha(accent, (0.12f * dim * 255).toInt()), withAlpha(0xFF8C8068.toInt(), (0.045f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)
        paint.shader = null

        val grainCount = 90
        for (i in 0 until grainCount) {
            val x0 = seededUnit(i * 17 + 3)
            val y0 = seededUnit(i * 29 + 7)
            val depth = seededRange(i * 53 + 13, 0.25f, 1f)
            val drift = seededRange(i * 67 + 19, 0.004f, 0.022f)
            val phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat())
            val noise = loopedFbm(t01, radius = 1.15f + depth, octaves = 2, seedOffset = i * 97 + 5)
            val angle = t01 * (2f * PI.toFloat()) + phaseOffset
            val x = (x0 + sin(angle) * drift + (noise - 0.5f) * drift * 0.35f + 1f) % 1f * w
            val y = (y0 + sin(angle * 0.73f) * drift * 0.7f + 1f) % 1f * h
            val baseSize = seededRange(i * 41 + 11, 0.0008f, 0.0032f)
            val radius = (minDim * baseSize * (0.8f + 0.35f * noise) * depth).coerceAtLeast(0.45f)
            val grainColor = mixArgb(accent, palette.colorAt(i + 1), 0.3f)
            val alpha = (0.08f + 0.16f * noise) * depth * dim

            if (depth > 0.62f) {
                paint.color = withAlpha(grainColor, (alpha * 0.26f * 255).toInt())
                canvas.drawCircle(x, y, radius * 3.2f, paint)
            }
            paint.color = withAlpha(grainColor, (alpha * 255).toInt())
            canvas.drawCircle(x, y, radius, paint)
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

    private fun brightness(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
