package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Star Field Parallax" for Auto/Ambient album art. */
internal object StarFieldStill {
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
        val minDim = w.coerceAtMost(h)

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF02030A.toInt(), Color.BLACK),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f

        drawLayer(canvas, rnd, w, h, minDim, count = 70, sizeScale = 1f, alphaScale = 0.7f, blurFrac = 0f, drift = drift, pulse = pulse, palette = palette, colorOffset = 0)
        drawLayer(canvas, rnd, w, h, minDim, count = 26, sizeScale = 1.7f, alphaScale = 0.85f, blurFrac = 0.018f, drift = drift, pulse = pulse, palette = palette, colorOffset = 70)
        drawLayer(canvas, rnd, w, h, minDim, count = 10, sizeScale = 2.6f, alphaScale = 1f, blurFrac = 0.045f, drift = drift, pulse = pulse, palette = palette, colorOffset = 96)

        paint.maskFilter = null
        val t = (generation % 480L).toFloat() / 480f
        val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
        if (edgeFade > 0.02f) {
            val sx = t * w
            val sy = (0.08f + (generation % 43L).toFloat() / 43f * 0.42f) * h
            paint.color = Color.argb((0.55f * edgeFade * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(sx, sy, 0.02f * minDim, paint)
        }
        paint.alpha = 255
    }

    private fun drawLayer(
        canvas: Canvas,
        rnd: Random,
        w: Float,
        h: Float,
        minDim: Float,
        count: Int,
        sizeScale: Float,
        alphaScale: Float,
        blurFrac: Float,
        drift: Float,
        pulse: Float,
        palette: AnimationPalette,
        colorOffset: Int,
    ) {
        paint.maskFilter = if (blurFrac > 0f) BlurMaskFilter(blurFrac * minDim, BlurMaskFilter.Blur.NORMAL) else null
        for (i in 0 until count) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val sizeUnit = rnd.nextFloat()
            val twinkle = 0.5f + 0.5f * sin(drift * (0.5f + rnd.nextFloat() * 1.1f) + rnd.nextFloat() * 2f * PI.toFloat())
            val x = (((x0 + drift * 0.01f) % 1f) + 1f) % 1f * w
            val y = y0 * h
            val radius = (0.008f + sizeUnit * 0.014f) * minDim * sizeScale
            val tint = palette.colorAt(colorOffset + i)
            val alpha = (0.35f + 0.65f * pulse) * (0.4f + 0.6f * twinkle) * alphaScale
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 255) / 2,
                (Color.green(tint) + 255) / 2,
                (Color.blue(tint) + 255) / 2,
            )
            canvas.drawCircle(x, y, radius, paint)
        }
    }
}
