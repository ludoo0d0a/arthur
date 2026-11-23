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
import kotlin.math.cos
import kotlin.math.sin

internal object PhysarumLatticeStill {
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
        val magenta = palette.colorAt(1)
        val minDim = minOf(w, h)
        val energy = pulse.coerceIn(0f, 1.4f)

        // Deep field with a subtle center glow behind the branching network.
        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.48f,
            minDim * 0.78f,
            intArrayOf(0xFF102A2C.toInt(), 0xFF071113.toInt(), 0xFF05080A.toInt()),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Curved teal filaments and finer magenta branches form an irregular
        // network rather than a regular geometric grid.
        val count = 5
        for (row in 0 until count) {
            for (col in 0 until count) {
                val i = row * count + col
                val n = loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = i * 17 + 7)
                val drift = sin(t01 * 2f * PI.toFloat() + i * 0.71f) * minDim * 0.012f
                val x = w * (0.12f + col * 0.19f) + drift
                val y = h * (0.12f + row * 0.19f) + cos(t01 * 2f * PI.toFloat() + i * 0.63f) * minDim * 0.012f
                val xNext = w * (0.12f + (col + 1) * 0.19f) + drift * 0.7f
                val yNext = h * (0.12f + row * 0.19f) + sin(t01 * 2f * PI.toFloat() + i * 0.4f) * minDim * 0.01f
                val xDown = w * (0.12f + col * 0.19f) - drift * 0.5f
                val yDown = h * (0.12f + (row + 1) * 0.19f)
                val bend = minDim * (0.026f + 0.016f * n)

                if (col < count - 1) {
                    val path = Path().apply {
                        moveTo(x, y)
                        quadTo((x + xNext) * 0.5f + bend, (y + yNext) * 0.5f - bend, xNext, yNext)
                    }
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeWidth = minDim * 0.004f
                    paint.color = withAlpha(accent, 0.38f * energy)
                    canvas.drawPath(path, paint)
                }
                if (row < count - 1) {
                    val path = Path().apply {
                        moveTo(x, y)
                        quadTo((x + xDown) * 0.5f - bend, (y + yDown) * 0.5f + bend, xDown, yDown)
                    }
                    paint.strokeWidth = minDim * 0.003f
                    paint.color = withAlpha(magenta, 0.21f * energy)
                    canvas.drawPath(path, paint)
                }

                // Sparse diagonal branches join neighboring cells and make the
                // lattice read as a living transport network.
                if (row < count - 1 && col < count - 1 && (row + col) % 2 == 0) {
                    paint.strokeWidth = minDim * 0.0022f
                    paint.color = withAlpha(magenta, 0.18f * energy)
                    canvas.drawLine(x, y, xNext + minDim * 0.01f, yDown, paint)
                }

                paint.style = Paint.Style.FILL
                val r = minDim * (0.006f + 0.005f * n)
                paint.shader = RadialGradient(
                    x, y, r * 3.5f,
                    intArrayOf(withAlpha(accent, 0.52f * energy), withAlpha(magenta, 0.16f * energy), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.42f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(x, y, r * 3.5f, paint)
                paint.shader = null
                paint.color = withAlpha(accent, 0.75f * energy)
                canvas.drawCircle(x, y, r * 0.42f, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.shader = null
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255f).toInt(), Color.red(color), Color.green(color), Color.blue(color))
}
