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

/** Bakes one static frame of the Constellation Twinkle starfield for Auto album art. */
internal object ConstellationStill {
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
            0f,
            0f,
            0f,
            h,
            0xFF07040F.toInt(),
            0xFF050914.toInt(),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val count = 26
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        val radii = FloatArray(count)
        val freqs = FloatArray(count)
        val phases = FloatArray(count)
        val colorIndices = IntArray(count)
        for (i in 0 until count) {
            xs[i] = rnd.nextFloat() * w
            ys[i] = rnd.nextFloat() * h
            radii[i] = 1.2f + rnd.nextFloat() * 2f
            freqs[i] = 0.4f + rnd.nextFloat() * 1.2f
            phases[i] = rnd.nextFloat() * 2f * PI.toFloat()
            colorIndices[i] = i
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.75f
        for (i in 0 until count) {
            if (i % 2 != 0) continue
            val j = (i + 1) % count
            val c = palette.primary
            paint.color = Color.argb(
                (0.09f * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawLine(xs[i], ys[i], xs[j], ys[j], paint)
        }

        paint.style = Paint.Style.FILL
        for (i in 0 until count) {
            val twinkle = 0.5f + 0.5f * sin(phase * freqs[i] + phases[i])
            val c = palette.colorAt(colorIndices[i])
            val alpha = (0.25f + 0.65f * twinkle).coerceIn(0f, 1f)
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawCircle(xs[i], ys[i], radii[i], paint)
        }
    }
}
