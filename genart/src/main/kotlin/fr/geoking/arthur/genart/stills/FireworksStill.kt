package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Fireworks for Auto/Ambient album art. */
internal object FireworksStill {
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
            intArrayOf(0xFF050510.toInt(), 0xFF020208.toInt(), 0xFF010104.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (bi in 0 until 5) {
            val cx = (0.18f + rnd.nextFloat() * 0.64f) * w
            val cy = (0.12f + rnd.nextFloat() * 0.43f) * h
            val phaseOffset = rnd.nextFloat()
            val duration = 0.18f + rnd.nextFloat() * 0.14f
            val maxRadiusFrac = 0.12f + rnd.nextFloat() * 0.16f
            val local = ((loop + phaseOffset) % 1f + 1f) % 1f
            if (local > duration) continue
            val life = local / duration
            val expand = (life / 0.35f).coerceIn(0f, 1f)
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.12f).coerceIn(0f, 1f)
            val radius = maxRadiusFrac * minDim * expand
            val tint = palette.colorAt(bi)
            val r = (Color.red(tint) + 255) / 2
            val g = (Color.green(tint) + 255) / 2
            val b = (Color.blue(tint) + 255) / 2

            paint.shader = RadialGradient(
                cx, cy, (radius * 0.55f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.45f * fade * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.argb((0.18f * fade * dim * 255).toInt().coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, (radius * 0.55f).coerceAtLeast(1f), paint)
            paint.shader = null

            paint.strokeCap = Paint.Cap.ROUND
            paint.style = Paint.Style.STROKE
            for (s in 0 until 14) {
                val angle = rnd.nextFloat() * 2f * PI.toFloat()
                val lengthMul = 0.55f + rnd.nextFloat() * 0.6f
                val thickness = 1.2f + rnd.nextFloat() * 1.4f
                val len = radius * lengthMul
                val ex = cx + cos(angle) * len
                val ey = cy + sin(angle) * len
                paint.strokeWidth = thickness
                paint.shader = LinearGradient(
                    cx, cy, ex, ey,
                    Color.argb((0.7f * fade * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawLine(cx, cy, ex, ey, paint)
                paint.shader = null
            }
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
