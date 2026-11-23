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
import kotlin.math.abs
import kotlin.math.sin

/** Bakes one frozen frame of US Retrowave Horizon for Auto/Ambient album art. */
internal object UsRetrowaveStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val ridge = Path()

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
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 3, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val accent = mixArgb(0xFFFF60C0.toInt(), palette.colorAt(0), 0.28f)
        val orange = 0xFFFF985E.toInt()
        val horizon = h * (0.53f + (wash - 0.5f) * 0.025f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080A14.toInt(), 0xFF180828.toInt(), 0xFF2A1040.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.5f, horizon, minDim * 0.62f,
            intArrayOf(withAlpha(orange, (0.30f * dim * 255).toInt()), withAlpha(accent, (0.18f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, horizon, minDim * 0.62f, paint)
        paint.shader = null

        // Soft striped sun.
        val sunRadius = minDim * 0.16f
        val sunY = horizon - minDim * 0.045f
        paint.shader = LinearGradient(
            0f, sunY - sunRadius, 0f, sunY + sunRadius,
            intArrayOf(withAlpha(0xFFFFBD78.toInt(), (0.76f * dim * 255).toInt()), withAlpha(accent, (0.68f * dim * 255).toInt()), withAlpha(0xFFFF4CA8.toInt(), (0.08f * dim * 255).toInt())),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, sunY, sunRadius, paint)
        paint.shader = null
        for (i in 0 until 5) {
            val stripeY = sunY + sunRadius * (0.16f + i * 0.14f)
            paint.color = withAlpha(0xFF281038.toInt(), ((0.30f + i * 0.035f) * dim * 255).toInt())
            canvas.drawRect(w * 0.5f - sunRadius, stripeY, w * 0.5f + sunRadius, stripeY + minDim * 0.009f, paint)
        }

        ridge.reset()
        ridge.moveTo(0f, horizon + minDim * 0.035f)
        ridge.lineTo(w * 0.13f, horizon - minDim * 0.01f)
        ridge.lineTo(w * 0.25f, horizon + minDim * 0.04f)
        ridge.lineTo(w * 0.39f, horizon - minDim * 0.025f)
        ridge.lineTo(w * 0.48f, horizon + minDim * 0.02f)
        ridge.lineTo(w * 0.62f, horizon - minDim * 0.03f)
        ridge.lineTo(w * 0.76f, horizon + minDim * 0.035f)
        ridge.lineTo(w * 0.88f, horizon - minDim * 0.015f)
        ridge.lineTo(w, horizon + minDim * 0.04f)
        ridge.lineTo(w, h)
        ridge.lineTo(0f, h)
        ridge.close()
        paint.color = withAlpha(0xFF130B25.toInt(), (0.88f * dim * 255).toInt())
        canvas.drawPath(ridge, paint)

        // Perspective grid / road, with horizontal lines fading as they wrap.
        val gridTop = horizon + minDim * 0.055f
        val gridHeight = h - gridTop
        val rows = 13
        for (i in 0 until rows) {
            val u = ((i + 1f) / rows + t01) % 1f
            val y = gridTop + gridHeight * u * u
            val half = w * (0.06f + 0.48f * u)
            paint.color = withAlpha(accent, ((0.48f * sin(u * PI.toFloat())).coerceAtLeast(0f) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.0015f + 0.0025f * u)
            canvas.drawLine(w * 0.5f - half, y, w * 0.5f + half, y, paint)
        }
        for (i in -6..6) {
            val bottomX = w * 0.5f + i * w * 0.105f
            paint.color = withAlpha(accent, ((0.13f + 0.12f * (1f - abs(i) / 6f)) * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.0018f
            canvas.drawLine(w * 0.5f + i * minDim * 0.003f, gridTop, bottomX, h, paint)
        }

        paint.shader = LinearGradient(
            0f, horizon - minDim * 0.07f, 0f, horizon + minDim * 0.09f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.14f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - minDim * 0.07f, w, horizon + minDim * 0.09f, paint)
        paint.shader = null

        for (i in 0 until 14) {
            val y = h * (i + 1f) / 15f
            val shimmer = 0.035f + 0.018f * sin(t01 * 2f * PI.toFloat() + i)
            paint.color = withAlpha(accent, (shimmer * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.0008f
            canvas.drawLine(0f, y, w, y, paint)
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
