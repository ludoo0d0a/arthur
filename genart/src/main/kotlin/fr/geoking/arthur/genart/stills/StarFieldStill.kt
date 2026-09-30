package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.GenartStillFx
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Star Field Parallax" for Auto/Ambient album art. */
internal object StarFieldStill {
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

        // Soft nebula / galaxy dust — fills most of the dark sky for AA surface coverage.
        paint.maskFilter = BlurMaskFilter(minDim * 0.08f, BlurMaskFilter.Blur.NORMAL)
        val dustCount = 10
        for (i in 0 until dustCount) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val radius = (0.28f + rnd.nextFloat() * 0.38f) * minDim
            val tint = palette.colorAt(i)
            val alpha = ((0.12f + rnd.nextFloat() * 0.14f) * (0.85f + 0.15f * pulse) * 255f)
                .toInt()
                .coerceIn(0, 255)
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
        }
        paint.shader = null
        paint.maskFilter = null

        // Dense far / mid / near layers — random positions per generation.
        drawLayer(canvas, rnd, w, h, minDim, count = 180, sizeScale = 1f, alphaScale = 0.8f, blurFrac = 0f, drift = drift, pulse = pulse, palette = palette, colorOffset = 0)
        drawLayer(canvas, rnd, w, h, minDim, count = 64, sizeScale = 1.85f, alphaScale = 0.95f, blurFrac = 0.012f, drift = drift, pulse = pulse, palette = palette, colorOffset = 70)
        drawLayer(canvas, rnd, w, h, minDim, count = 28, sizeScale = 2.9f, alphaScale = 1.15f, blurFrac = 0.032f, drift = drift, pulse = pulse, palette = palette, colorOffset = 96)

        // Extra seeded sparkle dust for near-4K richness.
        GenartStillFx.randomScatter(
            canvas = canvas,
            size = size,
            seed = generation xor 0x51F1E17L,
            count = 90,
            minRadiusFrac = 0.0005f,
            maxRadiusFrac = 0.0016f,
            alphaRange = 0.15f..0.55f,
            withGlow = true,
        )

        paint.maskFilter = null
        val t = (generation % 480L).toFloat() / 480f
        val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
        if (edgeFade > 0.02f) {
            val sx = t * w
            val sy = (0.08f + (generation % 43L).toFloat() / 43f * 0.42f) * h
            GenartStillFx.softGlow(canvas, sx, sy, 0.035f * minDim, Color.WHITE, 0.45f * edgeFade)
            paint.color = Color.argb((0.7f * edgeFade * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(sx, sy, 0.006f * minDim, paint)
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
            val radius = (0.0012f + sizeUnit * 0.0038f) * minDim * sizeScale
            val tint = palette.colorAt(colorOffset + i)
            val alpha = (0.4f + 0.6f * pulse) * (0.45f + 0.55f * twinkle) * alphaScale
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 255) / 2,
                (Color.green(tint) + 255) / 2,
                (Color.blue(tint) + 255) / 2,
            )
            canvas.drawCircle(x, y, radius.coerceAtLeast(0.85f), paint)
        }
    }
}
