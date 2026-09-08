package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of "Solar System" for Auto/Ambient album art, over the star field backdrop. */
internal object SolarSystemStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val revolutionPool = floatArrayOf(11f, 7f, 5f, 3f, 2f)

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        StarFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)

        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.52f
        val planetCount = 4
        val orbitT = (generation % 1000L).toFloat() / 1000f

        paint.maskFilter = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (i in 0 until planetCount) {
            val a = (0.16f + i * 0.085f) * minDim
            val b = a * (0.32f + (i % 3) * 0.03f)
            paint.color = Color.argb(16, 255, 255, 255)
            canvas.drawOval(cx - a, cy - b, cx + a, cy + b, paint)
        }

        val sunRadius = minDim * 0.05f
        paint.style = Paint.Style.FILL
        paint.maskFilter = BlurMaskFilter(minDim * 0.05f, BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, sunRadius * 3.2f,
            intArrayOf(Color.argb((140 * (0.6f + 0.4f * pulse)).toInt().coerceIn(0, 255), 255, 236, 194), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, sunRadius * 3.2f, paint)
        paint.shader = null
        paint.maskFilter = null
        paint.shader = RadialGradient(
            cx, cy, sunRadius,
            intArrayOf(Color.rgb(255, 243, 214), Color.rgb(255, 201, 120)),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, sunRadius, paint)
        paint.shader = null

        for (i in 0 until planetCount) {
            val a = (0.16f + i * 0.085f) * minDim
            val b = a * (0.32f + (i % 3) * 0.03f)
            val n = revolutionPool[i.coerceIn(0, revolutionPool.lastIndex)]
            val initialPhase = ((i * 149 + 11) % 97).toFloat() / 97f
            val angle = 2f * PI.toFloat() * (orbitT * n + initialPhase + rotationDeg / 360f)
            val px = cx + cos(angle) * a
            val py = cy + sin(angle) * b
            val radius = (0.028f + ((i * 163 + 13) % 23).toFloat() / 23f * 0.022f) * minDim
            val tint = palette.colorAt(i)

            if (i == 1) {
                paint.maskFilter = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = radius * 0.22f
                paint.color = Color.argb(90, Color.red(tint), Color.green(tint), Color.blue(tint))
                canvas.drawOval(px - radius * 2.1f, py - radius * 0.55f, px + radius * 2.1f, py + radius * 0.55f, paint)
            }

            paint.style = Paint.Style.FILL
            paint.maskFilter = null
            paint.color = Color.rgb(16, 19, 26)
            canvas.drawCircle(px, py, radius, paint)

            paint.shader = RadialGradient(
                px - radius * 0.4f, py - radius * 0.4f, radius * 1.3f,
                intArrayOf(Color.argb(230, Color.red(tint), Color.green(tint), Color.blue(tint)), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(px, py, radius, paint)
            paint.shader = null

            paint.shader = RadialGradient(
                px + radius * 0.5f, py + radius * 0.5f, radius * 1.4f,
                intArrayOf(Color.TRANSPARENT, Color.argb(210, 3, 4, 10)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(px, py, radius, paint)
            paint.shader = null

            paint.maskFilter = BlurMaskFilter((radius * 0.3f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = radius * 0.2f
            paint.color = Color.argb(80, Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawCircle(px, py, radius * 1.18f, paint)
            paint.maskFilter = null
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
