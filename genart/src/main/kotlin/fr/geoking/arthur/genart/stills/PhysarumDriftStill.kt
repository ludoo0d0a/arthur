package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI

internal object PhysarumDriftStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        paint.style = Paint.Style.FILL
        paint.color = 0xFF05080A.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        val accent = palette.colorAt(0)
        val shortSide = minOf(w, h)
        val time = t01 * 2f * PI.toFloat()

        // Soft teal pools beneath the drifting filaments.
        listOf(0.27f to 0.38f, 0.74f to 0.66f).forEachIndexed { i, (x, y) ->
            val cx = w * x + shortSide * 0.018f * kotlin.math.sin(time + i)
            val cy = h * y
            val radius = shortSide * 0.25f
            paint.shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(withAlpha(accent, (0.16f * pulse * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        // Curved vein branches carry small light points along their length.
        for (i in 0 until 14) {
            val seed = i * 23 + 9
            val x0 = w * (0.08f + (seed * 37 % 83) / 100f)
            val y0 = h * (-0.06f + (seed * 29 % 112) / 100f)
            val drift = loopedFbm(t01, radius = 1.1f + i * 0.01f, seedOffset = seed)
            val sway = (drift - 0.5f) * shortSide * 0.07f
            val reach = shortSide * (0.10f + (seed * 13 % 13) / 100f)
            val direction = if (i % 2 == 0) 1f else -1f
            val endX = x0 + sway + direction * reach * 0.25f
            val endY = y0 + reach * direction
            val path = Path().apply {
                moveTo(x0, y0)
                cubicTo(
                    x0 + sway * 0.6f - direction * reach * 0.30f,
                    y0 + reach * direction * 0.34f,
                    endX - sway * 0.5f + direction * reach * 0.28f,
                    endY - reach * direction * 0.28f,
                    endX,
                    endY,
                )
            }
            val alpha = ((0.18f + 0.14f * drift) * pulse * 255).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(accent, alpha / 3)
            paint.strokeWidth = shortSide * 0.018f
            canvas.drawPath(path, paint)
            paint.color = withAlpha(accent, alpha)
            paint.strokeWidth = shortSide * 0.004f
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL

            val travel = 0.5f + 0.5f * kotlin.math.sin(time + i * 0.137f)
            val pointX = x0 + (endX - x0) * travel
            val pointY = y0 + (endY - y0) * travel
            paint.color = withAlpha(accent, (0.34f * pulse * 255).toInt())
            canvas.drawCircle(pointX, pointY, shortSide * 0.009f, paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
