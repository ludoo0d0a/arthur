package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.random.Random

/** Bakes one frozen frame of the Pond Ripples look for Auto album art. */
internal object PondRipplesStill {
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

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            0xFF010204.toInt(),
            0xFF04141C.toInt(),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val originCount = 3
        val origins = Array(originCount) {
            floatArrayOf(
                0.18f + rnd.nextFloat() * 0.64f,
                0.22f + rnd.nextFloat() * 0.56f,
            )
        }

        val ringCount = 9
        val minDim = if (w < h) w else h
        val pulseAlpha = 0.85f + pulse * 0.15f
        for (i in 0 until ringCount) {
            val originIndex = i % originCount
            val phaseOffset = rnd.nextFloat()
            val cyclesPerLoop = 2.4f + rnd.nextFloat() * 1.2f
            val maxRadiusFrac = 0.16f + rnd.nextFloat() * 0.18f
            val baseAlpha = 0.22f + rnd.nextFloat() * 0.18f

            val raw = phase * cyclesPerLoop + phaseOffset
            val life = raw - kotlin.math.floor(raw)
            val radius = life * maxRadiusFrac * minDim
            if (radius <= 0.5f) continue

            val origin = origins[originIndex]
            val cx = origin[0] * w
            val cy = origin[1] * h
            val alpha = (1f - life) * baseAlpha * pulseAlpha
            val strokeWidth = (2.6f - life * 1.6f).coerceAtLeast(0.6f)

            val tint = palette.colorAt(i)
            paint.color = Color.argb(
                (alpha * 255f).toInt().coerceIn(0, 255),
                Color.red(tint),
                Color.green(tint),
                Color.blue(tint),
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.alpha = 255
    }
}
