package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Iridescent Soap Film for Auto/Ambient album art. */
internal object SoapFilmStill {
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
        val dim = 0.65f + 0.35f * pulse
        val cx = w * 0.5f
        val cy = h * 0.48f
        val time = phase + rotationDeg * 0.01f

        paint.shader = RadialGradient(
            cx, cy, minDim * 0.95f,
            intArrayOf(0xFF101828.toInt(), 0xFF060810.toInt(), 0xFF020408.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val ringCount = 12
        for (i in 0 until ringCount) {
            val frac = (i + 1f) / (ringCount + 1f)
            val radius = minDim * (0.08f + frac * 0.42f)
            val tint = palette.colorAt(i)
            val alpha = ((0.15f + 0.15f * (0.5f + 0.5f * sin(time + i))) * dim * 255f).toInt()
            paint.strokeWidth = minDim * 0.012f
            paint.color = Color.argb(alpha.coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawCircle(cx + rnd.nextFloat() * 2f - 1f, cy, radius, paint)
            paint.strokeWidth = minDim * 0.006f
            paint.color = Color.argb((alpha * 0.45f).toInt().coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawCircle(cx, cy, radius + minDim * 0.006f, paint)
        }

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx - minDim * 0.08f, cy - minDim * 0.1f, minDim * 0.2f,
            intArrayOf(Color.argb((40 * dim).toInt(), 255, 255, 255), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx - minDim * 0.08f, cy - minDim * 0.1f, minDim * 0.2f, paint)
        paint.shader = null
    }
}
