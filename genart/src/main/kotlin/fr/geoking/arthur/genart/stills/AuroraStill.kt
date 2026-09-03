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
import kotlin.random.Random

/** Bakes one frozen frame of the "Aurora Ribbons" engine for Auto/Ambient album art. */
internal object AuroraStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF040A1A.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val starCount = 40
        for (i in 0 until starCount) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val alpha = (0.15f + rnd.nextFloat() * 0.45f)
            val radius = size * (0.001f + rnd.nextFloat() * 0.0025f)
            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(x, y, radius, paint)
        }

        val ribbonCount = 5
        val timeAngle = phase + rotationDeg * PI.toFloat() / 180f
        for (i in 0 until ribbonCount) {
            val baseYFrac = 0.12f + rnd.nextFloat() * 0.38f
            val amplitudeFrac = 0.02f + rnd.nextFloat() * 0.04f * (0.7f + pulse * 0.3f)
            val cycles = 1f + rnd.nextFloat() * 1.2f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val thicknessFrac = 0.07f + rnd.nextFloat() * 0.07f
            val bottomPhaseShift = 0.3f + rnd.nextFloat() * 0.6f
            val speedMul = 0.6f + rnd.nextFloat() * 0.7f
            val color = palette.colorAt(i)

            val baseY = baseYFrac * h
            val amplitude = amplitudeFrac * h
            val thickness = thicknessFrac * h
            val freqPerPixel = cycles * 2f * PI.toFloat() / w
            val ribbonTime = timeAngle * speedMul

            path.reset()
            val segments = 32
            val topYs = FloatArray(segments + 1)
            val bottomYs = FloatArray(segments + 1)
            for (k in 0..segments) {
                val x = (k / segments.toFloat()) * w
                topYs[k] = baseY + amplitude * sin(x * freqPerPixel + ribbonTime + phaseOffset)
                bottomYs[k] = baseY + thickness +
                    amplitude * sin(x * freqPerPixel + ribbonTime + phaseOffset + bottomPhaseShift)
            }
            path.moveTo(0f, topYs[0])
            for (k in 1..segments) {
                val x = (k / segments.toFloat()) * w
                path.lineTo(x, topYs[k])
            }
            path.lineTo(w, bottomYs[segments])
            for (k in segments - 1 downTo 0) {
                val x = (k / segments.toFloat()) * w
                path.lineTo(x, bottomYs[k])
            }
            path.close()

            val alpha = ((0.15f + rnd.nextFloat() * 0.2f) * 255).toInt().coerceIn(0, 255)
            val topExtent = (baseY - amplitude).coerceAtLeast(0f)
            val bottomExtent = (baseY + thickness + amplitude).coerceAtMost(h)
            paint.shader = LinearGradient(
                0f,
                topExtent,
                0f,
                bottomExtent,
                intArrayOf(
                    Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
