package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Noise Field for Auto/Ambient album art. */
internal object SoftNoiseFieldStill {
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
        val minDim = minOf(w, h)
        paint.color = 0xFF06050A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 16) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val driftX = (rnd.nextFloat() - 0.5f) * 0.12f
            val driftY = (rnd.nextFloat() - 0.5f) * 0.1f
            val radiusFrac = 0.18f + rnd.nextFloat() * 0.24f
            val wobbleFreq = 0.1f + rnd.nextFloat() * 0.3f
            val wobblePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.08f + rnd.nextFloat() * 0.14f
            val wobble = sin(loop * 2f * PI.toFloat() * wobbleFreq + wobblePhase)
            val x = (((x0 + loop * driftX + wobble * 0.02f) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * driftY + wobble * 0.015f) % 1f) + 1f) % 1f * h
            val radius = radiusFrac * minDim * (0.9f + 0.1f * wobble)
            val tint = palette.colorAt(i)
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((alpha * 0.25f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
