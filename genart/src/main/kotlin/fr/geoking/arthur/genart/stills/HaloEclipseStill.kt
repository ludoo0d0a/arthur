package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Halo Eclipse for Auto/Ambient album art. */
internal object HaloEclipseStill {
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
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)

        paint.color = 0xFF020205.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        val count = 6
        val ringSpecs = List(count) { i ->
            val radiusFrac = 0.15f + 0.08f * i
            val strokeFrac = 0.02f + rnd.nextFloat() * 0.03f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            Triple(radiusFrac, strokeFrac, phaseOffset)
        }

        ringSpecs.reversed().forEachIndexed { reverseIdx, (radiusFrac, strokeFrac, phaseOffset) ->
            val i = count - 1 - reverseIdx
            val p = ((sin(loop * 2f * PI.toFloat() + phaseOffset) + 1f) / 2f)
            val radius = (radiusFrac + p * 0.04f) * minDim
            val strokeWidth = strokeFrac * minDim
            val c = palette.colorAt(i)

            // Outer shadow
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth * 1.5f
            paint.color = Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            canvas.drawCircle(cx, cy, radius + strokeWidth * 1.2f, paint)

            // Halo glow ring
            paint.shader = RadialGradient(
                cx, cy, (radius + strokeWidth * 2f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.65f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.argb((0.2f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.strokeWidth = strokeWidth
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        // Deep central eclipse shadow disk
        val coreRadius = 0.12f * minDim
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, (coreRadius * 1.5f).coerceAtLeast(1f),
            intArrayOf(
                Color.BLACK,
                Color.argb((0.85f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, coreRadius * 1.5f, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
