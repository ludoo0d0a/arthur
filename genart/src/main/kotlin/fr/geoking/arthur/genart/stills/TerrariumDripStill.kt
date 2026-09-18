package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.random.Random

/** Bakes one frozen frame of Terrarium Drip for Auto/Ambient album art. */
internal object TerrariumDripStill {
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
        val minDim = w.coerceAtMost(h)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF16261C.toInt(), 0xFF0A140E.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val driftUnit = phase / (2f * PI.toFloat())
        val dim = 0.7f + 0.3f * pulse
        val count = 3
        for (i in 0 until count) {
            val x0 = 0.18f + rnd.nextFloat() * 0.64f
            val y0 = 0.15f + rnd.nextFloat() * 0.6f
            val phaseOffset = rnd.nextFloat()
            val cycleSpeed = 0.6f + rnd.nextFloat() * 0.8f
            val maxRadiusFrac = 0.014f + rnd.nextFloat() * 0.012f
            val dripFrac = 0.06f + rnd.nextFloat() * 0.05f
            val growEnd = 0.78f
            val cycle = (((phaseOffset + driftUnit * cycleSpeed) % 1f) + 1f) % 1f
            val maxRadius = maxRadiusFrac * minDim
            val radius: Float
            val yOffset: Float
            val stretch: Float
            val alphaShape: Float
            if (cycle < growEnd) {
                val growT = cycle / growEnd
                radius = (0.08f + 0.92f * growT) * maxRadius
                yOffset = 0f
                stretch = 1f
                alphaShape = growT.coerceIn(0.15f, 1f)
            } else {
                val dripT = (cycle - growEnd) / (1f - growEnd)
                radius = maxRadius * (1f - 0.5f * dripT)
                yOffset = dripT * dripFrac * h
                stretch = 1f + 0.6f * dripT
                alphaShape = 1f - dripT
            }
            val alpha = alphaShape * dim
            val cx = x0 * w
            val cy = y0 * h + yOffset
            val rh = radius * stretch
            val tint = palette.colorAt(i)
            val bodyR = (Color.red(tint) + 191) / 2
            val bodyG = (Color.green(tint) + 230) / 2
            val bodyB = (Color.blue(tint) + 198) / 2
            val rimR = bodyR / 3
            val rimG = bodyG / 3 + 20
            val rimB = bodyB / 3

            paint.shader = null
            paint.color = Color.argb((alpha * 0.6f * 255).toInt().coerceIn(0, 255), rimR, rimG, rimB)
            canvas.drawOval(RectF(cx - radius, cy - rh, cx + radius, cy + rh), paint)

            paint.shader = RadialGradient(
                cx, cy, (radius * 0.92f).coerceAtLeast(1f),
                Color.argb((alpha * 0.85f * 255).toInt().coerceIn(0, 255), bodyR, bodyG, bodyB),
                Color.argb((alpha * 0.15f * 255).toInt().coerceIn(0, 255), bodyR, bodyG, bodyB),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(RectF(cx - radius * 0.92f, cy - rh * 0.92f, cx + radius * 0.92f, cy + rh * 0.92f), paint)
            paint.shader = null

            val highlightRadius = (radius * 0.32f).coerceAtLeast(1f)
            val hx = cx - radius * 0.35f
            val hy = cy - rh * 0.4f
            paint.shader = RadialGradient(
                hx, hy, highlightRadius,
                Color.argb((alpha * 0.9f * 255).toInt().coerceIn(0, 255), 244, 255, 246),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(hx, hy, highlightRadius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
