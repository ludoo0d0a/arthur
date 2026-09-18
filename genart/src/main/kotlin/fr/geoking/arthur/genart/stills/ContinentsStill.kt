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

/** Bakes one frozen frame of Continents for Auto/Ambient album art. */
internal object ContinentsStill {
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
        val cx = w * 0.5f
        val cy = h * 0.5f
        paint.shader = RadialGradient(
            cx, cy, maxOf(w, h) * 0.8f,
            intArrayOf(0xFF0A1830.toInt(), 0xFF030A16.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = (phase / (2f * PI.toFloat())) + rotationDeg / 360f
        val loop = ((time % 1f) + 1f) % 1f
        val rotation = loop * 2f * PI.toFloat()
        val dim = 0.65f + 0.35f * pulse

        val count = 3
        for (i in 0 until count) {
            val centerAngle = rnd.nextFloat() * 2f * PI.toFloat()
            val centerRadiusFrac = 0.16f + rnd.nextFloat() * 0.26f
            val scale = 0.55f + rnd.nextFloat() * 0.5f
            val lobeCount = 4 + rnd.nextInt(3)
            val angle = centerAngle + rotation
            val ox = cx + cos(angle) * centerRadiusFrac * minDim
            val oy = cy + sin(angle) * centerRadiusFrac * minDim
            val tint = palette.colorAt(i)
            for (l in 0 until lobeCount) {
                val dx = -0.5f + rnd.nextFloat()
                val dy = -0.5f + rnd.nextFloat()
                val radiusFrac = 0.24f + rnd.nextFloat() * 0.26f
                val lx = ox + dx * scale * minDim * 0.4f
                val ly = oy + dy * scale * minDim * 0.4f
                val radius = (radiusFrac * scale * minDim * 0.32f).coerceAtLeast(1f)
                val alphaCore = (0.85f * dim * 255).toInt().coerceIn(0, 255)
                val alphaMid = (0.5f * dim * 255).toInt().coerceIn(0, 255)
                paint.shader = RadialGradient(
                    lx, ly, radius,
                    intArrayOf(
                        Color.argb(alphaCore, Color.red(tint), Color.green(tint), Color.blue(tint)),
                        Color.argb(alphaMid, Color.red(tint), Color.green(tint), Color.blue(tint)),
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(lx, ly, radius, paint)
                paint.shader = null
            }
        }
        paint.alpha = 255
    }
}
