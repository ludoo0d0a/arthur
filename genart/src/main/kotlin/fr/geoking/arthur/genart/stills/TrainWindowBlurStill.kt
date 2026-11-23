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
import kotlin.math.cos

/** Bakes one frozen frame of Train Window Blur for Auto/Ambient album art. */
internal object TrainWindowBlurStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

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
        val wash = loopedFbm(t, radius = 1.35f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val accent = mixArgb(0xFF80A0C0.toInt(), palette.colorAt(0), 0.35f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A1E28.toInt(), 0xFF0E1018.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val horizon = h * (0.53f + (wash - 0.5f) * 0.035f)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.18f, 0f, horizon + h * 0.18f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.16f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.18f, w, horizon + h * 0.18f, paint)
        paint.shader = null

        // Blurred horizontal landscape bands beyond the pane.
        for (i in 0 until 8) {
            val n = loopedFbm(t, radius = 1.15f + i * 0.08f, octaves = 2, seedOffset = 73 + i * 31)
            val y = h * (0.17f + i * 0.096f) + (n - 0.5f) * h * 0.035f
            val bandH = h * (0.014f + 0.024f * n)
            val tint = if (i % 3 == 0) 0xFF657080.toInt() else 0xFF343A45.toInt()
            paint.shader = LinearGradient(
                0f, y - bandH, 0f, y + bandH,
                intArrayOf(Color.TRANSPARENT, withAlpha(tint, (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, y - bandH, w, y + bandH, paint)
            paint.shader = null
        }

        // Receding lights smear laterally; their positions wrap with phase.
        for (i in 0 until 9) {
            val yBase = 0.13f + i * 0.083f
            val widthFrac = 0.22f + ((i * 37 % 100) / 100f) * 0.25f
            val speed = 0.75f + ((i * 53 % 100) / 100f) * 0.75f
            val offset = (i * 29 % 100) / 100f
            val travel = (t * speed + offset) % 1f
            val lightW = widthFrac * w
            val x = travel * (w + lightW) - lightW
            val y = h * yBase + (loopedFbm(t, radius = 1.2f, octaves = 2, seedOffset = 151 + i * 23) - 0.5f) * h * 0.025f
            val color = if (i % 3 == 0) 0xFFB5A28B.toInt() else palette.colorAt(i)
            val radius = maxOf(lightW * 0.62f, 1f)
            paint.shader = RadialGradient(
                x + lightW * 0.5f, y, radius,
                intArrayOf(
                    withAlpha(color, (0.32f * dim * 255).toInt()),
                    withAlpha(color, (0.10f * dim * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.54f, 1f),
                Shader.TileMode.CLAMP,
            )
            rect.set(x, y - h * 0.04f, x + lightW, y + h * 0.04f)
            canvas.drawOval(rect, paint)
            paint.shader = null

            val glint = (0.05f + 0.07f * (0.5f + 0.5f * cos(t * 2f * PI.toFloat() + i))) * dim
            paint.color = withAlpha(color, (glint * 255).toInt())
            rect.set(x + lightW * 0.2f, y - h * 0.006f, x + lightW * 0.8f, y + h * 0.006f)
            canvas.drawRoundRect(rect, h * 0.006f, h * 0.006f, paint)
        }

        // Shadowed cabin edges and faint glass reflections.
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(withAlpha(0xFF080A0C.toInt(), (0.44f * dim * 255).toInt()), Color.TRANSPARENT, Color.TRANSPARENT,
                withAlpha(0xFF080A0C.toInt(), (0.44f * dim * 255).toInt())),
            floatArrayOf(0f, 0.07f, 0.93f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.018f
        paint.color = withAlpha(0xFF080A0C.toInt(), (0.30f * dim * 255).toInt())
        rect.set(w * 0.035f, h * 0.025f, w * 0.965f, h * 0.975f)
        canvas.drawRoundRect(rect, minDim * 0.04f, minDim * 0.04f, paint)
        paint.style = Paint.Style.FILL

        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.36f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
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
