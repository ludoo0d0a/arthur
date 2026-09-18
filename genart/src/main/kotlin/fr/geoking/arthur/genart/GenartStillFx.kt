package fr.geoking.arthur.genart

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.min
import kotlin.random.Random

/**
 * Shared near-4K polish for baked stills: anti-aliased/dithered paint, soft glow,
 * vignette, contrast, and seeded random scatter (e.g. star dust).
 */
internal object GenartStillFx {
    /** Composition zoom so motifs fill most of the frame without sparse letterboxing. */
    const val FILL_SCALE = 1.14f

    /** Contrast lift applied after the engine bake (1 = identity). */
    const val CONTRAST = 1.22f

    fun paint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    fun softGlow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
        alpha: Float,
    ) {
        if (radius <= 0.5f || alpha <= 0.01f) return
        val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        val paint = paint()
        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(
                Color.argb(a, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb((a * 0.35f).toInt(), Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    fun softShadowBlur(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        blurFrac: Float,
        color: Int = Color.BLACK,
        alpha: Float = 0.35f,
    ) {
        if (radius <= 0.5f) return
        val paint = paint()
        val blur = (blurFrac * radius).coerceAtLeast(1f)
        paint.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        paint.color = Color.argb(
            (alpha.coerceIn(0f, 1f) * 255f).toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
        canvas.drawCircle(cx + radius * 0.04f, cy + radius * 0.06f, radius, paint)
        paint.maskFilter = null
    }

    /**
     * Seeded random point scatter — use for starfields, pollen, sparkles, dust.
     * Positions and sizes are deterministic for [seed] so Ambient Rotation stays stable.
     */
    fun randomScatter(
        canvas: Canvas,
        size: Int,
        seed: Long,
        count: Int,
        minRadiusFrac: Float = 0.0008f,
        maxRadiusFrac: Float = 0.0035f,
        color: Int = Color.WHITE,
        alphaRange: ClosedFloatingPointRange<Float> = 0.25f..0.95f,
        withGlow: Boolean = true,
    ) {
        if (count <= 0) return
        val rnd = Random(seed)
        val dim = size.toFloat()
        val paint = paint()
        for (i in 0 until count) {
            val x = rnd.nextFloat() * dim
            val y = rnd.nextFloat() * dim
            val r = (minRadiusFrac + rnd.nextFloat() * (maxRadiusFrac - minRadiusFrac)) * dim
            val a = alphaRange.start + rnd.nextFloat() * (alphaRange.endInclusive - alphaRange.start)
            if (withGlow && r > dim * 0.0015f) {
                softGlow(canvas, x, y, r * 3.2f, color, a * 0.35f)
            }
            paint.color = Color.argb(
                (a * 255f).toInt().coerceIn(0, 255),
                Color.red(color),
                Color.green(color),
                Color.blue(color),
            )
            canvas.drawCircle(x, y, r.coerceAtLeast(0.75f), paint)
        }
        paint.alpha = 255
    }

    /** High-contrast vignette + ColorMatrix contrast polish drawn over the finished still. */
    fun polish(canvas: Canvas, size: Int, generation: Long) {
        val dim = size.toFloat()
        val cx = dim * 0.5f
        val cy = dim * 0.48f
        val paint = paint()

        // Soft outer vignette — keeps edges from looking sparse / clipped.
        paint.shader = RadialGradient(
            cx,
            cy,
            dim * 0.72f,
            intArrayOf(Color.TRANSPARENT, Color.argb(90, 0, 0, 0)),
            floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, dim, dim, paint)
        paint.shader = null

        // Contrast lift via a full-frame ColorMatrix pass (drawn as a filtered overlay of itself
        // is expensive; instead bake contrast into a translucent highlight/shadow wash).
        val contrastBoost = ((CONTRAST - 1f) * 40f).toInt().coerceIn(0, 48)
        if (contrastBoost > 0) {
            paint.shader = RadialGradient(
                cx,
                cy,
                dim * 0.55f,
                intArrayOf(Color.argb(contrastBoost, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, dim, dim, paint)
            paint.shader = null
            paint.color = Color.argb((contrastBoost * 0.55f).toInt(), 0, 0, 0)
            canvas.drawRect(0f, 0f, dim, dim * 0.08f, paint)
            canvas.drawRect(0f, dim * 0.92f, dim, dim, paint)
        }

        // Micro random sparkle dust — subtle, seed-stable, breaks flat regions without pixel blocks.
        val sparkleCount = 18 + (generation % 27L).toInt()
        randomScatter(
            canvas = canvas,
            size = size,
            seed = generation xor 0xA5A5_5A5AL,
            count = sparkleCount,
            minRadiusFrac = 0.00055f,
            maxRadiusFrac = 0.0018f,
            alphaRange = 0.08f..0.28f,
            withGlow = true,
        )
    }

    fun contrastColorFilter(contrast: Float = CONTRAST): ColorMatrixColorFilter {
        val c = contrast
        val t = (1f - c) * 0.5f * 255f
        val m = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        return ColorMatrixColorFilter(m)
    }

    fun minDim(width: Float, height: Float): Float = min(width, height)
}
