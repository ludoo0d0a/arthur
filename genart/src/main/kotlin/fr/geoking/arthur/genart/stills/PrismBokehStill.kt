package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Prism Bokeh for Auto/Ambient album art. */
internal object PrismBokehStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        paint.color = 0xFF030509.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val count = 18
        val time = (phase / (2f * PI.toFloat())) + (rotationDeg / 360f)
        val dim = 0.65f + 0.35f * pulse

        // Pre-generate Disc Attributes
        val discs = List(count) { i ->
            DiscStillData(
                x0 = rnd.nextFloat(),
                y0 = rnd.nextFloat(),
                speedX = -0.15f + rnd.nextFloat() * 0.3f,
                speedY = -0.15f + rnd.nextFloat() * 0.3f,
                radiusFrac = 0.08f + rnd.nextFloat() * 0.14f,
                phaseShift = rnd.nextFloat() * 2f * PI.toFloat(),
            )
        }

        // Draw Ambient Drop Shadows
        discs.forEachIndexed { i, disc ->
            val currX = (((disc.x0 + disc.speedX * time) % 1f) + 1f) % 1f * w
            val currY = (((disc.y0 + disc.speedY * time) % 1f) + 1f) % 1f * h
            val discPulse = 0.5f + 0.5f * sin(time * 2f * PI.toFloat() * 2f + disc.phaseShift)
            val r = disc.radiusFrac * minDim * (0.85f + 0.3f * discPulse)

            val shadowX = currX + r * 0.12f
            val shadowY = currY + r * 0.15f

            paint.shader = RadialGradient(
                shadowX, shadowY, r * 1.3f,
                intArrayOf(
                    Color.argb((0.4f * dim * 255).toInt(), 0, 0, 0),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(shadowX, shadowY, r * 1.3f, paint)
        }

        // Draw Bokeh Discs
        discs.forEachIndexed { i, disc ->
            val currX = (((disc.x0 + disc.speedX * time) % 1f) + 1f) % 1f * w
            val currY = (((disc.y0 + disc.speedY * time) % 1f) + 1f) % 1f * h
            val discPulse = 0.5f + 0.5f * sin(time * 2f * PI.toFloat() * 2f + disc.phaseShift)
            val r = disc.radiusFrac * minDim * (0.85f + 0.3f * discPulse)

            val color = palette.colorAt(i)
            val alpha1 = (0.65f * dim * 255).toInt().coerceIn(0, 255)
            val alpha2 = (0.35f * dim * 255).toInt().coerceIn(0, 255)
            val alpha3 = (0.08f * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                currX, currY, r,
                intArrayOf(
                    Color.argb(alpha1, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha2, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha3, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(currX, currY, r, paint)
            paint.shader = null
        }
    }

    private data class DiscStillData(
        val x0: Float,
        val y0: Float,
        val speedX: Float,
        val speedY: Float,
        val radiusFrac: Float,
        val phaseShift: Float,
    )
}
