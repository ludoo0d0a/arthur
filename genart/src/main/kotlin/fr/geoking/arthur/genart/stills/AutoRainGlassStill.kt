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
import kotlin.random.Random

/** Bakes one frozen rainy-night windshield frame for Auto/Ambient album art. */
internal object AutoRainGlassStill {
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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val drift = t + rotationDeg / 360f
        val dim = 0.82f + 0.18f * pulse
        val wash = loopedFbm(t, radius = 1.4f, octaves = 3, seedOffset = 11)
        val rnd = Random(generation xor 0x4155544F5241494EL)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0C1014.toInt(), 0xFF111820.toInt(), 0xFF182028.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Outside city bokeh and soft colored reflections.
        for (i in 0 until 9) {
            val x = (0.06f + rnd.nextFloat() * 0.88f) * w
            val y = (0.24f + rnd.nextFloat() * 0.38f) * h
            val baseR = minDim * (0.018f + rnd.nextFloat() * 0.032f)
            val wobble = loopedFbm(t, radius = 1.15f + i * 0.03f, octaves = 2, seedOffset = i * 61 + 47)
            val r = baseR * (0.9f + wobble * 0.18f)
            val warm = i % 3 != 0
            val base = if (warm) 0xFFFFC98A.toInt() else 0xFFA0C8E0.toInt()
            val tint = mixArgb(base, palette.colorAt(i), 0.14f)
            paint.shader = RadialGradient(
                x, y, r * 3.2f,
                intArrayOf(
                    withAlpha(tint, (0.34f * dim * 255).toInt()),
                    withAlpha(tint, (0.15f * dim * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 3.2f, paint)
            paint.shader = null
            paint.color = withAlpha(tint, ((0.18f + 0.12f * wobble) * dim * 255).toInt())
            canvas.drawCircle(x, y, r, paint)
        }

        val horizon = h * (0.54f + (wash - 0.5f) * 0.025f)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.09f, 0f, horizon + h * 0.09f,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFFA0C8E0.toInt(), (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.09f, w, horizon + h * 0.09f, paint)
        paint.shader = null

        // Long, gently bending rivulets crossing the glass.
        for (i in 0 until 18) {
            val x0 = (0.035f + rnd.nextFloat() * 0.93f) * w
            val seedY = rnd.nextFloat()
            val y0 = ((seedY + drift * (0.18f + rnd.nextFloat() * 0.26f)) % 1f) * h
            val len = (0.10f + rnd.nextFloat() * 0.28f) * h
            val sway = (0.004f + rnd.nextFloat() * 0.02f) * w * sin(drift * 2f * PI.toFloat() + i)
            val alpha = (0.10f + 0.11f * loopedFbm(t, radius = 1.2f, octaves = 2, seedOffset = i * 19 + 7)) * dim
            val color = mixArgb(0xFFA0C8E0.toInt(), palette.colorAt(i), 0.14f)
            path.reset()
            path.moveTo(x0, y0)
            path.cubicTo(x0 + sway * 0.4f, y0 + len * 0.32f, x0 - sway * 0.2f, y0 + len * 0.68f, x0 + sway * 0.25f, y0 + len)
            paint.shader = LinearGradient(
                x0, y0, x0 + sway * 0.25f, y0 + len,
                intArrayOf(Color.TRANSPARENT, withAlpha(color, (alpha * 255).toInt()), withAlpha(color, (alpha * 0.35f * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.18f, 0.76f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f + rnd.nextFloat() * 1.3f
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(path, paint)
            paint.shader = null
            paint.style = Paint.Style.FILL

            val beadX = x0 + sway * 0.25f
            val beadY = y0 + len
            val beadR = minDim * (0.004f + rnd.nextFloat() * 0.006f)
            paint.shader = RadialGradient(
                beadX, beadY, beadR,
                intArrayOf(withAlpha(0xFFDDF2FF.toInt(), (0.35f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(beadX, beadY, beadR, paint)
            paint.shader = null
        }

        // Dashboard shadow and faint cool reflection at the bottom of the windshield.
        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.9f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.86f, minDim * 0.42f,
            intArrayOf(withAlpha(0xFFA0C8E0.toInt(), (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.86f, minDim * 0.42f, paint)
        paint.shader = null
        paint.alpha = 255
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
