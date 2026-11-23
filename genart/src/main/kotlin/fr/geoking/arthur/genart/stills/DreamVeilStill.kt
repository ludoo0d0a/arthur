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
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Dream Veil for Auto/Ambient album art. */
internal object DreamVeilStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = 0.85f + 0.15f * pulse
        val drift = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 13)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A1430.toInt(), 0xFF2A2048.toInt(), 0xFF17142A.toInt()),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val accent = mixArgb(0xFFC0A0E8.toInt(), palette.colorAt(0), 0.22f)
        val focusX = w * (0.48f + 0.10f * (drift - 0.5f))
        val focusY = h * (0.43f + 0.06f * (loopedFbm(t01, radius = 1.3f, seedOffset = 53) - 0.5f))
        val pulseGlow = 0.88f + 0.12f * loopedFbm(t01, radius = 1.5f, octaves = 2, seedOffset = 37)
        paint.shader = RadialGradient(
            focusX, focusY, minDim * 0.62f,
            intArrayOf(withAlpha(accent, (0.25f * pulseGlow * dim * 255).toInt()), withAlpha(0xFF8670B8.toInt(), (0.11f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(focusX, focusY, minDim * 0.62f, paint)
        paint.shader = null
        for (i in 0..2) {
            val x = w * (0.22f + i * 0.28f + 0.025f * sin(t01 * 2f * PI.toFloat() + i))
            val y = h * (0.28f + i * 0.20f)
            val radius = minDim * (0.23f + 0.04f * i)
            val glow = if (i % 2 == 0) accent else 0xFF8870C0.toInt()
            paint.shader = RadialGradient(
                x, y, radius,
                intArrayOf(withAlpha(glow, (0.12f * pulseGlow * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }

        val veilCount = 7
        for (i in 0 until veilCount) {
            val layerTime = t01 * 2f * PI.toFloat() + i * 2.399f
            val y = h * (0.16f + 0.68f * ((i + 0.5f) / veilCount) + 0.04f * sin(layerTime))
            val amplitude = h * (0.045f + 0.025f * ((i % 3) / 2f))
            path.reset()
            path.moveTo(-w * 0.08f, y)
            path.cubicTo(
                w * 0.28f, y - amplitude * (0.45f + 0.2f * drift),
                w * 0.68f, y + amplitude * (0.45f + 0.2f * drift),
                w * 1.08f, y + amplitude * 0.15f,
            )
            val veilColor = mixArgb(accent, palette.colorAt(i), 0.18f)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(veilColor, (0.035f * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.34f + 0.04f * (i % 4))
            canvas.drawPath(path, paint)
            paint.color = withAlpha(veilColor, (0.065f * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.15f + 0.025f * (i % 4))
            canvas.drawPath(path, paint)
            paint.color = withAlpha(0xFFE8DCFF.toInt(), (0.08f * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.012f
            canvas.drawPath(path, paint)
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
