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

/** Bakes one frozen frame of Season Winter Wash for Auto/Ambient album art. */
internal object SeasonWinterWashStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 3, seedOffset = 11)
        val warp = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 29)
        val dim = 0.85f + 0.15f * pulse
        val pale = brightnessArgb(0xFFDDE6F0.toInt(), 0.94f + 0.06f * wash)
        val deep = brightnessArgb(0xFF7A90A8.toInt(), 1f)
        val snow = brightnessArgb(0xFFF5FBFF.toInt(), 1f)
        val accent = mixArgb(snow, palette.colorAt(0), 0.28f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(pale, mixArgb(pale, deep, 0.48f), deep),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val glowX = w * (0.28f + 0.44f * warp)
        val glowY = h * (0.24f + 0.12f * loopedFbm(t01, radius = 1.45f, seedOffset = 47))
        val glowRadius = minDim * 0.68f
        paint.shader = RadialGradient(
            glowX, glowY, glowRadius,
            intArrayOf(withAlpha(snow, 0.32f * dim), withAlpha(accent, 0.12f * dim), Color.TRANSPARENT),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)
        paint.shader = RadialGradient(
            w * 0.82f, h * 0.70f, minDim * 0.55f,
            intArrayOf(withAlpha(0xFFB8D4EC.toInt(), 0.20f * dim), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.82f, h * 0.70f, minDim * 0.55f, paint)
        paint.shader = null

        // Snow motes have staggered phases so the still feels like a moment in the loop.
        for (i in 0 until 26) {
            val seed = i * 97 + 5
            val n = loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = seed)
            val baseX = fract(i * 0.618034f + 0.13f)
            val baseY = fract(i * 0.381966f + 0.07f)
            val localPhase = t01 + baseY
            val x = (baseX + sin(t01 * 2f * PI.toFloat() + i) * 0.025f + (n - 0.5f) * 0.02f + 1f) % 1f * w
            val y = ((localPhase % 1f) * h)
            val radius = minDim * (0.0025f + 0.0065f * (i % 4) / 3f) * (0.75f + 0.4f * n)
            val alpha = (0.17f + 0.34f * n) * dim
            paint.shader = RadialGradient(
                x, y, radius * 3f,
                intArrayOf(withAlpha(accent, alpha), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius * 3f, paint)
            paint.shader = null
            paint.color = withAlpha(snow, alpha)
            canvas.drawCircle(x, y, radius, paint)
        }

        // Sparse cross-shaped ice glints.
        paint.color = withAlpha(snow, 0.22f * dim)
        paint.strokeWidth = minDim * 0.0015f
        for (i in 0 until 7) {
            val angle = t01 * 2f * PI.toFloat() + i * 1.7f
            val x = w * (0.12f + 0.76f * fract(i * 0.754877f + 0.21f)) + minDim * 0.012f * sin(angle)
            val y = h * (0.16f + 0.72f * fract(i * 0.56984f + 0.39f)) + minDim * 0.010f * cos(angle)
            val r = minDim * 0.010f
            canvas.drawLine(x - r, y, x + r, y, paint)
            canvas.drawLine(x, y - r, x, y + r, paint)
        }
        paint.color = Color.WHITE
        paint.shader = null
    }

    private fun fract(value: Float): Float = value - kotlin.math.floor(value)

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255f).toInt(), Color.red(rgb), Color.green(rgb), Color.blue(rgb))

    private fun brightnessArgb(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(argb), r, g, b)
    }
}
