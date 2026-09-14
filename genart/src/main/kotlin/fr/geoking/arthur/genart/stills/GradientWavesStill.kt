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

/** Bakes one frozen frame of Gradient Waves for Auto/Ambient album art. */
internal object GradientWavesStill {
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
        paint.color = 0xFF04060B.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val layers = 7
        val time = (phase / (2f * PI.toFloat())) + (rotationDeg / 360f)
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until layers) {
            val progress = i.toFloat() / layers
            val colorA = palette.colorAt(i)
            val colorB = palette.colorAt(i + 1)

            val baseScaleY = h * (0.25f + 0.65f * progress)
            val shadowOffsetY = h * 0.025f

            // Shadow Path
            val shadowPath = Path()
            shadowPath.moveTo(0f, h)
            shadowPath.lineTo(0f, baseScaleY + shadowOffsetY)
            val steps = 40
            for (step in 0..steps) {
                val normX = step.toFloat() / steps
                val x = w * normX
                val waveOffset = sin(normX * 2f * PI.toFloat() * 1.5f + time * 2f * PI.toFloat() + i * 0.7f) * (h * 0.08f) +
                        sin(normX * 2f * PI.toFloat() * 3f - time * 2f * PI.toFloat() * 0.8f) * (h * 0.04f)
                val y = baseScaleY + shadowOffsetY + waveOffset
                shadowPath.lineTo(x, y)
            }
            shadowPath.lineTo(w, h)
            shadowPath.close()

            paint.shader = LinearGradient(
                0f, baseScaleY, 0f, baseScaleY + shadowOffsetY * 3f,
                Color.argb((0.45f * dim * 255).toInt(), 0, 0, 0),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(shadowPath, paint)

            // Wave Path
            val wavePath = Path()
            wavePath.moveTo(0f, h)
            wavePath.lineTo(0f, baseScaleY)
            for (step in 0..steps) {
                val normX = step.toFloat() / steps
                val x = w * normX
                val waveOffset = sin(normX * 2f * PI.toFloat() * 1.5f + time * 2f * PI.toFloat() + i * 0.7f) * (h * 0.08f) +
                        sin(normX * 2f * PI.toFloat() * 3f - time * 2f * PI.toFloat() * 0.8f) * (h * 0.04f)
                val y = baseScaleY + waveOffset
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, h)
            wavePath.close()

            val alphaA = (0.85f * dim * 255).toInt().coerceIn(0, 255)
            val alphaB = (0.9f * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(
                    Color.argb(alphaA, Color.red(colorA), Color.green(colorA), Color.blue(colorA)),
                    Color.argb(alphaB, Color.red(colorB), Color.green(colorB), Color.blue(colorB)),
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(wavePath, paint)
            paint.shader = null
        }
    }
}
