package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Drifting Halos for Auto/Ambient album art. */
internal object DriftingHalosStill {
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
        val minDim = minOf(w, h)

        paint.color = 0xFF030308.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        val count = 10
        val haloSpecs = List(count) { i ->
            val xStart = -0.2f + rnd.nextFloat() * 1.4f
            val yStart = -0.2f + rnd.nextFloat() * 1.4f
            val xEnd = -0.2f + rnd.nextFloat() * 1.4f
            val yEnd = -0.2f + rnd.nextFloat() * 1.4f
            val radiusFrac = 0.15f + rnd.nextFloat() * 0.4f
            val ringThicknessFrac = 0.015f + rnd.nextFloat() * 0.065f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val speedMult = 0.5f + rnd.nextFloat() * 1.0f
            val baseIntensity = 0.35f + rnd.nextFloat() * 0.45f
            HaloSpec(xStart, yStart, xEnd, yEnd, radiusFrac, ringThicknessFrac, phaseOffset, speedMult, baseIntensity)
        }

        haloSpecs.forEachIndexed { i, halo ->
            val localTime = (loop * halo.speedMult + halo.phaseOffset / (2f * PI.toFloat())) % 1f
            val motionPhase = (sin(localTime * 2f * PI.toFloat()) + 1f) / 2f
            val intensityPulse = (sin(localTime * 4f * PI.toFloat() + halo.phaseOffset) + 1f) / 2f

            val cx = (halo.xStart + (halo.xEnd - halo.xStart) * motionPhase) * w
            val cy = (halo.yStart + (halo.yEnd - halo.yStart) * (1f - motionPhase)) * h

            val radius = halo.radiusFrac * minDim * (0.88f + 0.24f * intensityPulse)
            val strokeWidth = halo.ringThicknessFrac * minDim * (0.9f + 0.2f * motionPhase)
            val intensity = halo.baseIntensity * (0.65f + 0.35f * intensityPulse) * dim

            val c = palette.colorAt(i)
            val r = android.graphics.Color.red(c)
            val g = android.graphics.Color.green(c)
            val b = android.graphics.Color.blue(c)

            // Outer soft glow aura
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx, cy, (radius + strokeWidth * 2.5f).coerceAtLeast(1f),
                intArrayOf(
                    android.graphics.Color.argb((intensity * 0.45f * 255).toInt().coerceIn(0, 255), r, g, b),
                    android.graphics.Color.argb((intensity * 0.15f * 255).toInt().coerceIn(0, 255), r, g, b),
                    android.graphics.Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius + strokeWidth * 2.5f, paint)

            // Ring halo core
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.shader = RadialGradient(
                cx, cy, (radius + strokeWidth).coerceAtLeast(1f),
                intArrayOf(
                    android.graphics.Color.argb((intensity * 0.85f * 255).toInt().coerceIn(0, 255), r, g, b),
                    android.graphics.Color.argb((intensity * 0.3f * 255).toInt().coerceIn(0, 255), r, g, b),
                    android.graphics.Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)

            paint.shader = null
        }
        paint.alpha = 255
    }

    private data class HaloSpec(
        val xStart: Float,
        val yStart: Float,
        val xEnd: Float,
        val yEnd: Float,
        val radiusFrac: Float,
        val ringThicknessFrac: Float,
        val phaseOffset: Float,
        val speedMult: Float,
        val baseIntensity: Float,
    )
}
