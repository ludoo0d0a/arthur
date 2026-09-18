package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.random.Random

/** Bakes one frozen frame of the Moonlight Ripples look for Auto album art. */
internal object MoonlightRipplesStill {
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

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF03060F.toInt(), 0xFF060A16.toInt(), 0xFF01030A.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val minDim = if (w < h) w else h
        val moonCx = w * 0.5f
        val moonCy = h * 0.22f
        val moonRadius = minDim * 0.09f
        val waterTop = h * 0.58f
        val pulseAlpha = 0.85f + pulse * 0.15f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(0xEF, 0xF3, 0xFA)
        canvas.drawCircle(moonCx, moonCy, moonRadius, paint)

        paint.shader = RadialGradient(
            moonCx - moonRadius * 0.35f,
            moonCy - moonRadius * 0.35f,
            moonRadius * 1.25f,
            Color.argb((0.95f * pulseAlpha * 255f).toInt().coerceIn(0, 255), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, moonCy, moonRadius, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            moonCx,
            moonCy,
            moonRadius * 3.4f,
            Color.argb((0.35f * pulseAlpha * 255f).toInt().coerceIn(0, 255), 210, 220, 245),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, moonCy, moonRadius * 3.4f, paint)
        paint.shader = null

        val beamWidth = minDim * 0.05f
        paint.shader = LinearGradient(
            moonCx - beamWidth,
            0f,
            moonCx + beamWidth,
            0f,
            intArrayOf(Color.TRANSPARENT, Color.argb((0.2f * pulseAlpha * 255f).toInt().coerceIn(0, 255), 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(
            moonCx - beamWidth,
            moonCy + moonRadius * 0.6f,
            moonCx + beamWidth,
            waterTop,
            paint,
        )
        paint.shader = null

        val originCount = 2
        val origins = Array(originCount) {
            floatArrayOf(
                0.32f + rnd.nextFloat() * 0.36f,
                0.62f + rnd.nextFloat() * 0.24f,
            )
        }

        val ringCount = 9
        for (i in 0 until ringCount) {
            val originIndex = i % originCount
            val phaseOffset = rnd.nextFloat()
            val cyclesPerLoop = 2.2f + rnd.nextFloat() * 1.2f
            val maxRadiusFrac = 0.14f + rnd.nextFloat() * 0.16f
            val baseAlpha = 0.2f + rnd.nextFloat() * 0.16f

            val raw = phase * cyclesPerLoop + phaseOffset
            val life = raw - kotlin.math.floor(raw)
            val radius = life * maxRadiusFrac * minDim
            if (radius <= 0.5f) continue

            val origin = origins[originIndex]
            val cx = origin[0] * w
            val cy = origin[1] * h
            val alpha = (1f - life) * baseAlpha * pulseAlpha
            val strokeWidth = (2.4f - life * 1.4f).coerceAtLeast(0.6f)

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
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
