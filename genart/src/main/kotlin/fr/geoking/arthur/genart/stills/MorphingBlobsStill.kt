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

/** Bakes one frozen frame of Morphing Blobs for Auto/Ambient album art. */
internal object MorphingBlobsStill {
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
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.8f,
            intArrayOf(0xFF0C0A14.toInt(), 0xFF040308.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 8) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val driftX = 0.03f + rnd.nextFloat() * 0.09f
            val driftY = 0.02f + rnd.nextFloat() * 0.08f
            val radiusFrac = 0.14f + rnd.nextFloat() * 0.18f
            val pulseFreq = 0.2f + rnd.nextFloat() * 0.4f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.18f + rnd.nextFloat() * 0.2f
            val x = (((x0 + loop * driftX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * driftY) % 1f) + 1f) % 1f * h
            val pulseMul = 0.8f + 0.2f * sin(loop * 2f * PI.toFloat() * pulseFreq + pulsePhase)
            val radius = radiusFrac * minDim * pulseMul
            val tint = palette.colorAt(i)
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((alpha * 0.4f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
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
