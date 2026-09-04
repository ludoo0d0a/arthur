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

/** Bakes one frozen frame of Nebula Drift for Auto/Ambient album art. */
internal object NebulaStill {
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
        paint.color = 0xFF030208.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        for (j in 0 until 32) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h
            val a = 0.1f + rnd.nextFloat() * 0.4f
            paint.color = Color.argb((a * 255).toInt(), 255, 255, 255)
            canvas.drawCircle(sx, sy, 1f, paint)
        }

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 6) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val speedX = 0.02f + rnd.nextFloat() * 0.06f
            val speedY = 0.01f + rnd.nextFloat() * 0.04f
            val radiusFrac = 0.22f + rnd.nextFloat() * 0.26f
            val pulseFreq = 0.15f + rnd.nextFloat() * 0.35f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.1f + rnd.nextFloat() * 0.18f
            val x = (((x0 + loop * speedX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * speedY) % 1f) + 1f) % 1f * h
            val pulseMul = 0.85f + 0.15f * sin(loop * 2f * PI.toFloat() * pulseFreq + pulsePhase) * pulse
            val radius = radiusFrac * minDim * pulseMul
            val tint = palette.colorAt(i)
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((alpha * 0.35f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
