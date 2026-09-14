package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Topo Gradients for Auto/Ambient album art. */
internal object TopoGradientsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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

        paint.color = 0xFF030508.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val bands = 8
        val time = (phase / (2f * PI.toFloat())) + (rotationDeg / 360f)
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until bands) {
            val normI = i.toFloat() / bands
            val baseY = h * (0.15f + 0.75f * normI)
            val shadowYOffset = h * 0.03f

            // Shadow Path
            val shadowPath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY + shadowYOffset)
                val steps = 50
                for (s in 0..steps) {
                    val normX = s.toFloat() / steps
                    val x = w * normX
                    val wave = sin(normX * 2f * PI.toFloat() * 1.8f + time * 2f * PI.toFloat() + i * 0.5f) * (h * 0.07f) +
                            sin(normX * 2f * PI.toFloat() * 3.5f - time * 2f * PI.toFloat() * 0.7f) * (h * 0.03f)
                    val y = baseY + shadowYOffset + wave
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }

            paint.shader = LinearGradient(
                0f, baseY, 0f, baseY + shadowYOffset * 3f,
                Color.argb((0.5f * dim * 255).toInt(), 0, 0, 0),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(shadowPath, paint)

            // Topo Path
            val topoPath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY)
                val steps = 50
                for (s in 0..steps) {
                    val normX = s.toFloat() / steps
                    val x = w * normX
                    val wave = sin(normX * 2f * PI.toFloat() * 1.8f + time * 2f * PI.toFloat() + i * 0.5f) * (h * 0.07f) +
                            sin(normX * 2f * PI.toFloat() * 3.5f - time * 2f * PI.toFloat() * 0.7f) * (h * 0.03f)
                    val y = baseY + wave
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }

            val colorA = palette.colorAt(i)
            val colorB = palette.colorAt(i + 1)
            val alphaA = (0.88f * dim * 255).toInt().coerceIn(0, 255)
            val alphaB = (0.92f * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(
                    Color.argb(alphaA, Color.red(colorA), Color.green(colorA), Color.blue(colorA)),
                    Color.argb(alphaB, Color.red(colorB), Color.green(colorB), Color.blue(colorB)),
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(topoPath, paint)

            // Top Edge Ridge Highlight
            paint.shader = null
            paint.color = Color.argb((0.25f * dim * 255).toInt(), 255, 255, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            canvas.drawPath(topoPath, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
