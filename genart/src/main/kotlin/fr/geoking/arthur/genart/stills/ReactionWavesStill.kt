package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.LinearGradient
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

internal object ReactionWavesStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val bg = Color.rgb(8, 11, 16)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, 0f, h, bg, Color.rgb(12, 21, 25), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val waveCount = 9
        for (i in 0 until waveCount) {
            val centerY = h * (i + 0.5f) / waveCount + h * 0.018f * sin(t01 * 2f * PI.toFloat() + i * 0.71f)
            val amplitude = h * (0.055f + 0.018f * loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = i * 19 + 3))
            val frequency = 1.8f + (i % 4) * 0.38f
            val color = when (i % 3) {
                0 -> palette.colorAt(0)
                1 -> palette.colorAt(1)
                else -> Color.rgb(181, 217, 106)
            }

            path.reset()
            val steps = 56
            for (step in 0..steps) {
                val x = w * step / steps
                val nx = step.toFloat() / steps
                val localWarp = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = i * 37 + step * 5)
                val y = centerY + sin(nx * frequency * 2f * PI.toFloat() + t01 * 2f * PI.toFloat() + i * 0.63f) * amplitude * (0.72f + localWarp * 0.46f)
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = Color.argb(34, Color.red(color), Color.green(color), Color.blue(color))
            paint.strokeWidth = h * (0.04f + 0.01f * pulse)
            canvas.drawPath(path, paint)
            paint.color = Color.argb(72, Color.red(color), Color.green(color), Color.blue(color))
            paint.strokeWidth = h * 0.013f
            canvas.drawPath(path, paint)
            paint.color = Color.argb(150, Color.red(color), Color.green(color), Color.blue(color))
            paint.strokeWidth = h * 0.0028f
            canvas.drawPath(path, paint)
        }

        paint.style = Paint.Style.FILL
    }
}
