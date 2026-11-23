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
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Micro Bubble Skin for Auto/Ambient album art. */
internal object MicroBubbleSkinStill {
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
        val drift = loopedFbm(t, radius = 1.2f, octaves = 3, seedOffset = 11)
        val warp = loopedFbm(t, radius = 1.45f, octaves = 2, seedOffset = 37)
        val dim = (0.88f + 0.12f * pulse).coerceIn(0.55f, 1f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF102028.toInt(), 0xFF081018.toInt(), 0xFF0B1B24.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cyan = mixArgb(0xFF80E0FF.toInt(), palette.colorAt(0), 0.22f)
        paint.shader = RadialGradient(
            w * (0.43f + 0.08f * (warp - 0.5f)), h * 0.47f, minDim * 0.82f,
            intArrayOf(withAlpha(cyan, (0.16f * dim * 255).toInt()), withAlpha(0xFF256078.toInt(), (0.08f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * (0.43f + 0.08f * (warp - 0.5f)), h * 0.47f, minDim * 0.82f, paint)
        paint.shader = null

        for (i in 0 until 30) {
            val col = i % 6
            val row = i / 6
            val phaseOffset = seededUnit(i * 53 + 13) * 2f * PI.toFloat()
            val breathe = 0.96f + 0.04f * sin(t * 2f * PI.toFloat() + phaseOffset)
            val r = minDim * seededRange(i * 41 + 11, 0.082f, 0.115f) * breathe
            val x = w * ((col + 0.42f + seededRange(i * 17 + 3, -0.16f, 0.16f)) / 6f) + minDim * 0.006f * sin(t * 2f * PI.toFloat() + phaseOffset)
            val y = h * ((row + 0.52f + seededRange(i * 29 + 7, -0.14f, 0.14f)) / 5f) + minDim * 0.004f * sin(t * 2f * PI.toFloat() + phaseOffset + 1f)
            val hue = when (i % 3) {
                0 -> 0xFF55D8E8.toInt()
                1 -> 0xFF799AFF.toInt()
                else -> 0xFF8CE8D2.toInt()
            }

            paint.shader = RadialGradient(
                x, y, r * 1.18f,
                intArrayOf(withAlpha(hue, (0.22f * dim * 255).toInt()), withAlpha(0xFF173D4B.toInt(), (0.13f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 1.18f, paint)

            paint.shader = RadialGradient(
                x - r * 0.16f, y - r * 0.12f, r * 1.05f,
                intArrayOf(withAlpha(0xFF8EEBFF.toInt(), (0.13f * dim * 255).toInt()), withAlpha(hue, (0.09f * dim * 255).toInt()), withAlpha(0xFF07131B.toInt(), (0.28f * dim * 255).toInt())),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.0032f
            paint.color = withAlpha(cyan, ((0.32f + 0.12f * warp) * dim * 255).toInt())
            canvas.drawCircle(x, y, r, paint)

            rect.set(x - r * 0.82f, y - r * 0.82f, x + r * 0.82f, y + r * 0.82f)
            paint.strokeWidth = minDim * 0.006f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(0xFFC7F8FF.toInt(), ((0.50f + 0.22f * warp) * dim * 255).toInt())
            canvas.drawArc(rect, 202f + (i % 3) * 13f + rotationDeg * 0.04f, 88f, false, paint)
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(Color.WHITE, (0.42f * dim * 255).toInt())
            canvas.drawCircle(x - r * 0.38f, y - r * 0.42f, minDim * 0.004f, paint)
        }

        for (i in 0 until 7) {
            val x = w * (0.08f + seededUnit(i * 83 + 19) * 0.84f)
            val y = h * (0.08f + seededUnit(i * 97 + 31) * 0.84f)
            val twinkle = 0.45f + 0.55f * loopedFbm(t, radius = 1.1f, seedOffset = i * 109 + 47)
            paint.color = withAlpha(0xFFA8F2FF.toInt(), (0.42f * twinkle * dim * 255).toInt())
            canvas.drawCircle(x, y, minDim * 0.0025f, paint)
        }
        paint.shader = null
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
