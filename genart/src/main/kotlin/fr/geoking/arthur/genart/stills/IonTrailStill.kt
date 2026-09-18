package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** Bakes one frozen frame of "Ion Trail" for Auto/Ambient album art: starfield plus a comet head and its fading trail. */
internal object IonTrailStill {
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
        StarFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)

        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)

        val t = ((phase + rotationDeg / 360f) % 1f + 1f) % 1f
        val baseY = h * 0.42f
        val amplitude = h * 0.16f
        val freq = 1.3f

        fun pathPoint(tt: Float): Pair<Float, Float> {
            val x = tt * w
            val y = baseY + sin(tt * 2f * PI.toFloat() * freq) * amplitude
            return x to y
        }

        val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
        val headAlpha = (0.5f + 0.5f * pulse) * edgeFade
        if (headAlpha <= 0.01f) return

        val tint = palette.colorAt(0)
        val segmentCount = 18
        val trailSpan = 0.05f
        paint.maskFilter = null
        for (i in segmentCount downTo 0) {
            val back = i.toFloat() / segmentCount
            val tt = t - back * trailSpan
            if (tt < 0f) continue
            val (x, y) = pathPoint(tt)
            val decay = (1f - back).pow(2)
            val alpha = (headAlpha * decay).coerceIn(0f, 1f)
            if (alpha <= 0.01f) continue
            val radius = (0.004f + (1f - back) * 0.009f) * minDim
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 255 * 2) / 3,
                (Color.green(tint) + 255 * 2) / 3,
                (Color.blue(tint) + 255 * 2) / 3,
            )
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.alpha = 255
    }
}
