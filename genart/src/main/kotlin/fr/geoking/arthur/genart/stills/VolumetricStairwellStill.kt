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

/** Bakes one softly lit stairwell frame for Auto/Ambient album art. */
internal object VolumetricStairwellStill {
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
        val unit = size.toFloat()
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val haze = loopedFbm(t, radius = 1.35f, octaves = 3, seedOffset = 11)
        val sway = loopedFbm(t, radius = 1.15f, octaves = 2, seedOffset = 37) - 0.5f
        val dim = 0.85f + 0.15f * pulse
        val warm = mixArgb(0xFFE8D8B0.toInt(), palette.colorAt(0), 0.22f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(withAlpha(0xFF12141A.toInt(), dim), withAlpha(0xFF2A2E38.toInt(), dim), withAlpha(0xFF17191F.toInt(), dim)),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cx = w * (0.52f + sway * 0.035f)
        val openingY = h * 0.19f
        val openingW = w * 0.28f
        val openingH = h * 0.35f
        val opening = Path().apply {
            moveTo(cx - openingW / 2f, openingY + openingH)
            lineTo(cx - openingW / 2f, openingY + openingH * 0.32f)
            quadTo(cx - openingW / 2f, openingY, cx, openingY)
            quadTo(cx + openingW / 2f, openingY, cx + openingW / 2f, openingY + openingH * 0.32f)
            lineTo(cx + openingW / 2f, openingY + openingH)
            close()
        }
        paint.shader = LinearGradient(
            0f, openingY, 0f, openingY + openingH,
            intArrayOf(withAlpha(warm, 0.48f * dim), withAlpha(warm, 0.16f * dim), withAlpha(0xFF17191F.toInt(), dim)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(opening, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * 0.012f
        paint.color = withAlpha(0xFF080A10.toInt(), 0.72f * dim)
        canvas.drawPath(opening, paint)
        paint.style = Paint.Style.FILL

        // Warm bloom around the landing and broad, translucent shafts of light.
        paint.shader = RadialGradient(
            cx, openingY + openingH * 0.45f, unit * 0.72f,
            intArrayOf(withAlpha(warm, (0.22f + haze * 0.06f) * dim), withAlpha(warm, 0.055f * dim), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, openingY + openingH * 0.45f, unit * 0.72f, paint)
        paint.shader = null
        for (beam in 0..2) {
            val beamX = cx + (beam - 1) * openingW * 0.18f + sway * unit * 0.06f
            val beamPath = Path().apply {
                moveTo(beamX - openingW * 0.12f, openingY + openingH * 0.38f)
                lineTo(beamX + openingW * 0.12f, openingY + openingH * 0.38f)
                lineTo(beamX + openingW * 0.34f, h * 0.78f)
                lineTo(beamX - openingW * 0.34f, h * 0.78f)
                close()
            }
            paint.shader = LinearGradient(
                beamX - openingW * 0.4f, 0f, beamX + openingW * 0.4f, 0f,
                intArrayOf(Color.TRANSPARENT, withAlpha(warm, (0.045f + haze * 0.012f) * dim), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawPath(beamPath, paint)
            paint.shader = null
        }

        // Receding treads catch only a fine rim of light.
        for (i in 0 until 9) {
            val depth = i / 8f
            val y = h * (0.48f + depth * 0.49f)
            val topY = y - h * (0.018f + depth * 0.038f)
            val halfWidth = w * (0.10f + depth * 0.32f)
            val left = cx - halfWidth
            val right = cx + halfWidth
            val step = Path().apply {
                moveTo(left, topY)
                lineTo(right, topY)
                lineTo(right + w * 0.025f * depth, y)
                lineTo(left - w * 0.025f * depth, y)
                close()
            }
            paint.color = withAlpha(0xFF080A10.toInt(), (0.18f + depth * 0.38f) * dim)
            canvas.drawPath(step, paint)
            paint.style = Paint.Style.STROKE
            paint.color = withAlpha(warm, (0.07f + 0.11f * (1f - depth) * (0.85f + haze * 0.15f)) * dim)
            paint.strokeWidth = unit * (0.002f + depth * 0.003f)
            canvas.drawLine(left, topY, right, topY, paint)
            paint.style = Paint.Style.FILL
        }

        // Dust in the shafts, moving gently with the loop phase.
        for (i in 0 until 7) {
            val n = loopedFbm(t, radius = 1.2f, octaves = 2, seedOffset = i * 97 + 5)
            val x0 = 0.14f + (i % 4) * 0.22f
            val y0 = 0.20f + (i * 0.137f) % 0.62f
            val drift = 0.018f + 0.004f * (i % 3)
            val x = (x0 + sway * drift + (n - 0.5f) * drift * 0.5f) * w
            val y = ((y0 - t * drift + drift * sin(t * 2f * PI.toFloat() + i) * 0.18f + 1f) % 1f) * h
            paint.color = withAlpha(warm, (0.20f + 0.20f * n) * dim)
            canvas.drawCircle(x, y, unit * (0.0018f + 0.0018f * n), paint)
        }

        // Soft vignette keeps the illuminated landing as the focal point.
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.46f, unit * 0.82f,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(0xFF080A10.toInt(), 0.45f * dim)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255f).toInt(), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
