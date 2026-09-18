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

/** Bakes one frozen frame of the "Breath Circles" engine for Auto/Ambient album art. */
internal object BreathCirclesStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val centerX = w * 0.5f
        val centerY = h * 0.5f
        val minDim = w.coerceAtMost(h)

        paint.shader = RadialGradient(
            centerX,
            centerY,
            w.coerceAtLeast(h) * 0.75f,
            intArrayOf(0xFF0B1220.toInt(), 0xFF05070D.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val groupCount = 3
        val inhale = pulse.coerceIn(0.35f, 1f)
        for (i in 0 until groupCount) {
            val offsetXFrac = -0.10f + rnd.nextFloat() * 0.20f
            val offsetYFrac = -0.08f + rnd.nextFloat() * 0.16f
            val baseRadiusFrac = 0.16f + rnd.nextFloat() * 0.14f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()

            val breathFactor = sin(phase + phaseOffset)
            val radiusFactor = 1f + 0.3f * (breathFactor * 0.5f + inhale * 0.5f)
            val baseRadius = minDim * baseRadiusFrac
            val radius = (baseRadius * radiusFactor).coerceAtLeast(1f)

            val cx = centerX + offsetXFrac * w
            val cy = centerY + offsetYFrac * h

            val color = palette.colorAt(i)
            val coreAlpha = ((0.22f + 0.10f * inhale) * 255).toInt().coerceIn(0, 255)
            val haloAlpha = (coreAlpha * 0.45f).toInt().coerceIn(0, 255)
            val haloRadius = radius * 1.6f

            paint.shader = RadialGradient(
                cx,
                cy,
                haloRadius,
                Color.argb(haloAlpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, haloRadius, paint)
            paint.shader = null

            paint.shader = RadialGradient(
                cx,
                cy,
                radius,
                Color.argb(coreAlpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }
    }
}
