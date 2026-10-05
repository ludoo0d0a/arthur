package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Pane Reflections for Auto/Ambient album art. */
internal object PaneReflectionsStill {
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
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.15f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF101820.toInt(), 0xFF060A10.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        for (i in 0 until 6) {
            val x0 = rnd.nextFloat()
            val y0 = 0.15f + rnd.nextFloat() * 0.7f
            val radius = (0.12f + rnd.nextFloat() * 0.16f) * w
            val drift = 0.02f + rnd.nextFloat() * 0.04f
            val blobPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alpha = 0.18f + rnd.nextFloat() * 0.22f
            val cx = (((x0 + drift * sin(time + blobPhase)) % 1f) + 1f) % 1f * w
            val cy = y0 * h
            val tint = palette.colorAt(i)
            paint.shader = RadialGradient(
                cx, cy, radius.coerceAtLeast(1f),
                Color.argb((alpha * dim * 255).toInt().coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.argb((0.04f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.TRANSPARENT,
                Color.argb((0.06f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        for (k in 0 until 5) {
            val x0 = 0.1f + rnd.nextFloat() * 0.8f
            val y0 = 0.05f + rnd.nextFloat() * 0.3f
            val length = (0.25f + rnd.nextFloat() * 0.45f) * h
            val thickness = 0.8f + rnd.nextFloat() * 1.6f
            val slideFreq = 0.08f + rnd.nextFloat() * 0.14f
            val slidePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alpha = 0.06f + rnd.nextFloat() * 0.1f
            val x = (x0 + sin(time * slideFreq + slidePhase) * 0.08f) * w
            val yStart = y0 * h
            val yEnd = yStart + length
            paint.shader = LinearGradient(
                x, yStart, x, yEnd,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((alpha * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.strokeWidth = thickness
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(x, yStart, x, yEnd, paint)
            paint.style = Paint.Style.FILL
            paint.shader = null
        }

        for (j in 0 until 14) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val sizeFrac = 0.004f + rnd.nextFloat() * 0.014f
            val driftFreq = 0.2f + rnd.nextFloat() * 0.35f
            val driftAmp = 0.01f + rnd.nextFloat() * 0.03f
            val twinkleFreq = 0.4f + rnd.nextFloat() * 1f
            val twinklePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val drift = driftAmp * sin(time * driftFreq + twinklePhase)
            val x = (((x0 + drift) % 1f) + 1f) % 1f * w
            val y = (((y0 + drift * 0.4f) % 1f) + 1f) % 1f * h
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * twinkleFreq + twinklePhase))
            val tint = palette.colorAt(j)
            val r = ((Color.red(tint) + 255) / 2)
            val g = ((Color.green(tint) + 255) / 2)
            val b = ((Color.blue(tint) + 255) / 2)
            val radius = sizeFrac * minDim * (0.7f + 0.5f * twinkle)
            paint.shader = RadialGradient(
                x, y, (radius * 2.2f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.85f * twinkle * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.argb((0.35f * twinkle * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius * 2.2f, paint)
            paint.shader = null
            paint.color = Color.argb((0.7f * twinkle * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(x, y, radius * 0.35f, paint)
        }
        paint.alpha = 255
        paint.shader = null
    }
}
