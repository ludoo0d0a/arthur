package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Rain on Glass for Auto/Ambient album art. */
internal object RainOnGlassStill {
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
            0f, 0f, 0f, h,
            intArrayOf(0xFF141C28.toInt(), 0xFF080C12.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val dim = 0.7f + 0.3f * pulse

        for (i in 0 until 3) {
            val cx = rnd.nextFloat() * w
            val cy = (0.2f + rnd.nextFloat() * 0.6f) * h
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 143) / 2
            val g = (Color.green(tint) + 180) / 2
            val b = (Color.blue(tint) + 216) / 2
            val alpha = (0.25f * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                cx, cy, (w * 0.35f).coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, w * 0.35f, paint)
            paint.shader = null
        }

        for (i in 0 until 16) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val baseRadiusFrac = 0.018f + rnd.nextFloat() * 0.027f
            val growAmpFrac = 0.006f + rnd.nextFloat() * 0.012f
            val growFreq = 0.05f + rnd.nextFloat() * 0.11f
            val growPhase = rnd.nextFloat()
            val alphaBase = 0.32f + rnd.nextFloat() * 0.28f
            val highlightAngle = rnd.nextFloat() * 2f * PI.toFloat()

            val grow = (sin(drift * growFreq + growPhase * 2f * PI.toFloat()).toFloat() + 1f) / 2f
            val radius = (baseRadiusFrac + growAmpFrac * grow) * w
            val x = x0 * w
            val y = y0 * h

            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 220) / 2
            val g = (Color.green(tint) + 235) / 2
            val b = (Color.blue(tint) + 250) / 2
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, r, g, b),
                    Color.argb((alpha * 0.35f).toInt(), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null

            val hx = x + sin(highlightAngle) * radius * 0.35f
            val hy = y - sin(highlightAngle + PI.toFloat() / 2f) * radius * 0.35f
            paint.shader = RadialGradient(
                hx, hy, (radius * 0.4f).coerceAtLeast(1f),
                Color.argb((alpha * 0.8f).toInt(), 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(hx, hy, radius * 0.4f, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
