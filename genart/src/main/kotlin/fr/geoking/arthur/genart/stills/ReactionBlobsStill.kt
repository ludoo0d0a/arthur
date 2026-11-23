package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object ReactionBlobsStill {
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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val rose = palette.colorAt(0)
        val cyan = palette.colorAt(1)
        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.52f,
            minDim * 0.7f,
            intArrayOf(withAlpha(cyan, 30), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.52f, minDim * 0.7f, paint)
        paint.shader = null

        for (i in 0 until 9) {
            val n = loopedFbm(t, radius = 1.25f, seedOffset = i * 23)
            val angle = t * 2f * PI.toFloat() + i * 0.85f
            val orbit = minDim * (0.02f + (i % 4) * 0.012f)
            val x = w * (0.18f + (i % 3) * 0.32f) + cos(angle) * orbit
            val y = h * (0.22f + (i / 3) * 0.28f) + sin(angle * 0.85f) * orbit
            val r = minDim * (0.10f + 0.06f * n) * (0.9f + 0.1f * pulse)
            val accent = if (i % 2 == 0) rose else cyan

            paint.shader = RadialGradient(
                x,
                y,
                r,
                intArrayOf(
                    withAlpha(accent, (0.40f * 255).toInt()),
                    withAlpha(accent, (0.22f * 255).toInt()),
                    withAlpha(accent, (0.06f * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 0.82f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r, paint)
            paint.shader = null
            paint.color = withAlpha(accent, 46)
            canvas.drawCircle(x, y, r * 0.42f, paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
