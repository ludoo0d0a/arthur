package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the Rivers look for Auto album art. */
internal object RiversStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

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

        val bankTop = palette.colorAt(2)
        val bankBottom = palette.colorAt(3)
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(bankTop, bankBottom),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val segments = 48
        val amp1 = 0.12f * h
        val amp2 = 0.06f * h
        val amp3 = 0.03f * h
        val freq1 = 2f * PI.toFloat() / w.coerceAtLeast(1f)
        val timePhase = phase * 2f * PI.toFloat()

        fun riverX(fracY: Float): Float {
            val y = fracY * h
            return w * 0.5f +
                amp1 * sin(y * freq1 * 1.3f + timePhase * 0.6f) +
                amp2 * sin(y * freq1 * 2.7f - timePhase * 0.9f + 1.7f) +
                amp3 * sin(y * freq1 * 4.1f + timePhase * 1.3f + 0.4f)
        }

        val halfWidth = w * 0.09f
        path.reset()
        for (k in 0..segments) {
            val fracY = k / segments.toFloat()
            val y = fracY * h
            val x = riverX(fracY) - halfWidth
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        for (k in segments downTo 0) {
            val fracY = k / segments.toFloat()
            val y = fracY * h
            val x = riverX(fracY) + halfWidth
            path.lineTo(x, y)
        }
        path.close()

        val waterTop = palette.colorAt(0)
        val waterBottom = palette.colorAt(1)
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(waterTop, waterBottom),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        paint.shader = null

        val rippleCount = 9
        val pulseAlpha = 0.85f + pulse * 0.15f
        for (i in 0 until rippleCount) {
            val phaseOffset = rnd.nextFloat()
            val cyclesPerLoop = 0.6f + rnd.nextFloat() * 0.5f
            val travelOffset = rnd.nextFloat()
            val maxRadiusFrac = 0.02f + rnd.nextFloat() * 0.03f
            val baseAlpha = 0.25f + rnd.nextFloat() * 0.2f

            val rawLife = phase * cyclesPerLoop + phaseOffset
            val life = rawLife - kotlin.math.floor(rawLife)
            val radius = life * maxRadiusFrac * w
            if (radius <= 0.5f) continue

            val rawFrac = travelOffset + phase * 0.5f
            val fracY = rawFrac - kotlin.math.floor(rawFrac)
            val cx = riverX(fracY)
            val cy = fracY * h
            val alpha = (1f - life) * baseAlpha * pulseAlpha
            val strokeWidth = (1.6f - life * 1.0f).coerceAtLeast(0.5f)

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
