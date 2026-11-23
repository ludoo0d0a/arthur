package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes a quiet high-altitude cloud horizon for album art. */
internal object AvionHorizonCloudsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val oval = RectF()

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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 31)
        val dim = 0.85f + 0.15f * pulse
        val minDim = minOf(w, h)
        val horizon = h * (0.49f + 0.012f * (wash - 0.5f))
        val skyTop = 0xFF080A0C.toInt()
        val skyBlue = 0xFF7AB0E0.toInt()
        val cloudWhite = 0xFFD8E8F8.toInt()
        val white = 0xFFFFFFFF.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(skyTop, mixArgb(skyTop, skyBlue, 0.74f), skyBlue, cloudWhite, skyBlue, mixArgb(skyBlue, skyTop, 0.34f)),
            floatArrayOf(0f, 0.34f, 0.49f, 0.54f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Diffuse light across the distant horizon.
        val glowX = w * (0.51f + (wash - 0.5f) * 0.05f)
        val glowRadius = minDim * 0.48f
        paint.shader = RadialGradient(
            glowX, horizon, glowRadius,
            intArrayOf(withAlpha(white, (0.42f * dim * 255).toInt()), withAlpha(cloudWhite, (0.22f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.32f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, horizon, glowRadius, paint)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.09f, 0f, horizon + h * 0.10f,
            intArrayOf(Color.TRANSPARENT, withAlpha(white, (0.17f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.09f, w, horizon + h * 0.10f, paint)
        paint.shader = null

        // A thin distant cloud shelf separates sky from the cloud ocean below.
        oval.set(-w * 0.08f, horizon - h * 0.025f, w * 1.08f, horizon + h * 0.095f)
        paint.shader = LinearGradient(
            0f, oval.top, 0f, oval.bottom,
            intArrayOf(withAlpha(cloudWhite, (0.04f * dim * 255).toInt()), withAlpha(cloudWhite, (0.34f * dim * 255).toInt()), withAlpha(skyBlue, (0.08f * dim * 255).toInt())),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(oval, paint)
        paint.shader = null

        // Layered soft cloud banks, with deterministic offsets matching the animated view.
        for (i in 0 until 10) {
            val n = loopedFbm(t01, radius = 1.1f + i * 0.03f, octaves = 2, seedOffset = i * 97 + 5)
            val x0 = seededUnit(i * 17 + 3)
            val yFrac = seededRange(i * 29 + 7, 0.52f, 0.91f)
            val cloudW = seededRange(i * 41 + 11, 0.18f, 0.42f)
            val cloudH = seededRange(i * 53 + 13, 0.045f, 0.13f)
            val drift = seededRange(i * 67 + 19, 0.012f, 0.035f)
            val travel = (t01 * drift + (n - 0.5f) * 0.018f + 1f) % 1f
            val x = ((x0 + travel) % 1.35f - 0.16f) * w
            val y = h * yFrac + sin(t01 * 2f * PI.toFloat() + i) * h * 0.006f
            val cw = w * cloudW * (0.9f + n * 0.18f)
            val ch = h * cloudH * (0.88f + n * 0.18f)
            val centerX = x + cw * 0.5f
            val centerY = y + ch * 0.5f
            val radius = cw * 0.62f
            val color = if (i % 3 == 0) cloudWhite else skyBlue
            val alpha = (0.13f + n * 0.14f) * dim
            paint.shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(withAlpha(color, (alpha * 255).toInt()), withAlpha(cloudWhite, (alpha * 0.62f * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
            oval.set(x, y, x + cw, y + ch)
            canvas.drawOval(oval, paint)
        }
        paint.shader = null

        // Near cloud tops add depth at the lower edge without creating a hard horizon.
        for (i in 0 until 4) {
            val cx = w * (0.12f + i * 0.27f) + sin(t01 * 2f * PI.toFloat() + i * 1.7f) * w * 0.025f
            val cy = h * (0.83f + (i % 2) * 0.035f)
            val radius = minDim * (0.30f + i * 0.025f)
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(withAlpha(cloudWhite, (0.16f * dim * 255).toInt()), withAlpha(skyBlue, (0.10f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.shader = null
    }

    private fun seededUnit(seed: Int): Float = fr.geoking.arthur.genart.seededUnit(seed)

    private fun seededRange(seed: Int, min: Float, max: Float): Float =
        fr.geoking.arthur.genart.seededRange(seed, min, max)

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, blue)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
