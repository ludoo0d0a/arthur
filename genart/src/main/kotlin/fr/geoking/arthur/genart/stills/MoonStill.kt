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

/** Bakes one frozen frame of Full Moon for Auto/Ambient album art. */
internal object MoonStill {
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
        val cx = w * 0.5f
        val cy = h * 0.5f
        val moonR = minDim * 0.36f
        val dim = 0.65f + 0.35f * pulse
        val glowPulse = 0.9f + 0.1f * ((sin(phase + rotationDeg * PI.toFloat() / 180f) + 1f) / 2f)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF04060E.toInt(), 0xFF010208.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        GenartStillFx.randomScatter(
            canvas, size, generation xor 0x4D4F4FL, count = 36,
            minRadiusFrac = 0.0008f, maxRadiusFrac = 0.0025f,
            alphaRange = 0.15f..0.55f,
            withGlow = false,
        )

        val tint = palette.colorAt(0)
        val mr = (Color.red(tint) + 232) / 2
        val mg = (Color.green(tint) + 236) / 2
        val mb = (Color.blue(tint) + 244) / 2

        paint.maskFilter = BlurMaskFilter((moonR * 0.35f).coerceAtLeast(8f), BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, moonR * 1.55f,
            intArrayOf(
                Color.argb((0.4f * dim * glowPulse * 255).toInt().coerceIn(0, 255), mr, mg, mb),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, moonR * 1.55f, paint)
        paint.maskFilter = null
        paint.shader = null

        paint.shader = RadialGradient(
            cx - moonR * 0.35f, cy - moonR * 0.35f, moonR * 1.3f,
            intArrayOf(
                Color.argb((0.95f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.argb((0.85f * dim * 255).toInt().coerceIn(0, 255), mr, mg, mb),
                Color.argb((0.75f * dim * 255).toInt().coerceIn(0, 255), 200, 208, 220),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, moonR, paint)
        paint.shader = RadialGradient(
            cx + moonR * 0.45f, cy + moonR * 0.35f, moonR * 1.35f,
            intArrayOf(Color.TRANSPARENT, Color.argb((0.65f * dim * 255).toInt().coerceIn(0, 255), 26, 32, 48)),
            floatArrayOf(0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, moonR, paint)
        paint.shader = null

        for (i in 0 until 10) {
            val xFrac = rnd.nextFloat() * 1.4f - 0.7f
            val yFrac = rnd.nextFloat() * 1.4f - 0.7f
            if (xFrac * xFrac + yFrac * yFrac > 0.85f) continue
            val r = moonR * (0.04f + rnd.nextFloat() * 0.1f)
            val px = cx + xFrac * moonR
            val py = cy + yFrac * moonR
            val depth = 0.15f + rnd.nextFloat() * 0.25f
            paint.color = Color.argb((depth * dim * 255).toInt().coerceIn(0, 255), 154, 163, 180)
            canvas.drawCircle(px, py, r, paint)
            paint.color = Color.argb((0.2f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(px - r * 0.15f, py - r * 0.15f, r * 0.85f, paint)
            paint.color = Color.argb((0.35f * depth * dim * 255).toInt().coerceIn(0, 255), 42, 51, 68)
            canvas.drawCircle(px + r * 0.12f, py + r * 0.12f, r * 0.7f, paint)
        }
        paint.alpha = 255
    }
}
