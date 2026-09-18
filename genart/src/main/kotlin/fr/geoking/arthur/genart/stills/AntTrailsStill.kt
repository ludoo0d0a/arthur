package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Ant Trails" for Android Auto album art — deterministic per [generation]. */
internal object AntTrailsStill {
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
            intArrayOf(0xFF241A12.toInt(), 0xFF120C10.toInt(), 0xFF120C10.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val centerXFrac = 0.42f + rnd.nextFloat() * 0.16f
        val centerYFrac = 0.5f + rnd.nextFloat() * 0.12f
        val ampXFrac = 0.28f + rnd.nextFloat() * 0.1f
        val ampYFrac = 0.14f + rnd.nextFloat() * 0.08f
        val freqX = 1.6f + rnd.nextFloat() * 0.8f
        val freqY = 2.6f + rnd.nextFloat() * 0.8f
        val phaseX = rnd.nextFloat() * 2f * PI.toFloat()

        val cx = centerXFrac * w
        val cy = centerYFrac * h
        val ampX = ampXFrac * w
        val ampY = ampYFrac * h
        val minDim = size.toFloat()

        val count = 16
        paint.style = Paint.Style.FILL
        for (i in 0 until count) {
            val phaseOffset = i / count.toFloat()
            val scale = 0.7f + rnd.nextFloat() * 0.5f
            val ti = (((phase + phaseOffset) % 1f + 1f) % 1f) * (2f * PI.toFloat())
            val x = cx + ampX * sin(freqX * ti + phaseX)
            val y = cy + ampY * sin(freqY * ti)

            val base = palette.colorAt(i)
            val factor = 0.35f
            val alpha = (0.55f * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(
                alpha,
                (Color.red(base) * factor).toInt().coerceIn(0, 255),
                (Color.green(base) * factor).toInt().coerceIn(0, 255),
                (Color.blue(base) * factor).toInt().coerceIn(0, 255),
            )
            val radius = scale * minDim * 0.008f
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.alpha = 255
    }
}
