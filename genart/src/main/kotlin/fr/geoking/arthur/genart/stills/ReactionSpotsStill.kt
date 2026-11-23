package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

internal object ReactionSpotsStill {
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
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        val shortSide = minOf(w, h)
        val cols = 6
        val rows = 8
        val cellW = w / cols
        val cellH = h / rows
        for (row in -1..rows) {
            for (col in -1..cols) {
                val index = (row + 2) * 37 + (col + 2) * 19
                val drift = loopedFbm(t01, radius = 1.15f, seedOffset = index)
                val x = (col + 0.5f + 0.12f * (drift - 0.5f) + 0.012f * sin(t01 * 2f * PI.toFloat() + index)) * cellW
                val y = (row + 0.5f + 0.10f * (loopedFbm(t01, radius = 1.3f, seedOffset = index + 101) - 0.5f)) * cellH
                val radius = shortSide * (0.045f + 0.018f * loopedFbm(t01, radius = 0.8f, seedOffset = index + 211))
                val accent = palette.colorAt((row * 3 + col).let { if (it < 0) -it else it })
                val phase = 0.92f + 0.08f * pulse

                paint.shader = RadialGradient(
                    x, y, radius,
                    intArrayOf(
                        withAlpha(accent, (0.44f * phase * 255).toInt()),
                        withAlpha(accent, (0.32f * phase * 255).toInt()),
                        withAlpha(accent, (0.16f * phase * 255).toInt()),
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.72f, 0.92f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(x, y, radius, paint)
                paint.shader = null

                // Delicate darker boundary gives the spots a reaction-diffusion cell edge.
                paint.color = withAlpha(0xFF08060C.toInt(), 60)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = shortSide * 0.0017f
                canvas.drawCircle(x, y, radius * (0.80f + 0.04f * drift), paint)
                paint.style = Paint.Style.FILL
            }
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
