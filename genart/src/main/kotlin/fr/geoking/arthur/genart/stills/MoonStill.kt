package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
        val moonR = minDim * 0.30f
        val glowPulse = 0.88f + 0.12f * ((sin(phase + rotationDeg * PI.toFloat() / 180f) + 1f) / 2f)
        val dim = 0.65f + 0.35f * pulse
        val drift = ((sin(phase * 2f * PI.toFloat()) + 1f) / 2f) * minDim * 0.02f
        val bandOffset = (phase - 0.5f) * 2f * moonR * 1.3f
        val cx = w * 0.64f + drift
        val cy = h * 0.5f
        val tint = palette.colorAt(0)
        val mr = (Color.red(tint) + 232) / 2
        val mg = (Color.green(tint) + 236) / 2
        val mb = (Color.blue(tint) + 244) / 2

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A1228.toInt(), 0xFF020409.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        GenartStillFx.randomScatter(
            canvas, size, generation xor 0x4D4F4FL, count = 36,
            minRadiusFrac = 0.0008f, maxRadiusFrac = 0.0025f,
            alphaRange = 0.12f..0.38f,
            withGlow = false,
        )

        paint.maskFilter = BlurMaskFilter((moonR * 0.35f).coerceAtLeast(8f), BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, moonR * 1.7f,
            intArrayOf(
                Color.argb((0.38f * dim * glowPulse * 255).toInt().coerceIn(0, 255), mr, mg, mb),
                Color.argb((0.16f * dim * glowPulse * 255).toInt().coerceIn(0, 255), mr, mg, mb),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, moonR * 1.7f, paint)
        paint.maskFilter = null
        paint.shader = null

        paint.shader = RadialGradient(
            cx - moonR * 0.3f, cy - moonR * 0.3f, moonR * 1.25f,
            intArrayOf(
                Color.argb((0.95f * dim * glowPulse * 255).toInt().coerceIn(0, 255), 255, 252, 240),
                Color.argb((0.8f * dim * glowPulse * 255).toInt().coerceIn(0, 255), mr, mg, mb),
                Color.argb((0.85f * dim * glowPulse * 255).toInt().coerceIn(0, 255), 35, 44, 70),
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, moonR, paint)
        paint.shader = null

        for (i in 0 until 5) {
            val xFrac = rnd.nextFloat() * 1.4f - 0.7f
            val yFrac = rnd.nextFloat() * 1.4f - 0.7f
            if (xFrac * xFrac + yFrac * yFrac > 0.85f) continue
            val r = moonR * (0.06f + rnd.nextFloat() * 0.1f)
            val px = cx + xFrac * moonR
            val py = cy + yFrac * moonR
            paint.color = Color.argb((0.55f * dim * glowPulse * 255).toInt().coerceIn(0, 255), 53, 65, 92)
            canvas.drawCircle(px, py, r, paint)
        }

        for (i in 0 until 12) {
            val xFrac = rnd.nextFloat() * 1.4f - 0.7f
            val yFrac = rnd.nextFloat() * 1.4f - 0.7f
            if (xFrac * xFrac + yFrac * yFrac > 0.85f) continue
            val r = moonR * (0.005f + rnd.nextFloat() * 0.012f)
            paint.color = Color.argb((0.1f * dim * glowPulse * 255).toInt().coerceIn(0, 255), 242, 245, 250)
            canvas.drawCircle(cx + xFrac * moonR, cy + yFrac * moonR, r, paint)
        }

        val bandPath = Path().apply {
            moveTo(cx - moonR * 1.7f - bandOffset, cy - moonR * 0.5f)
            quadTo(
                cx - bandOffset,
                cy - moonR * 1.25f,
                cx + moonR * 1.7f - bandOffset,
                cy - moonR * 0.5f,
            )
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = moonR * 0.22f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.argb((0.36f * dim * glowPulse * 255).toInt().coerceIn(0, 255), 62, 74, 102)
        canvas.drawPath(bandPath, paint)
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f

        for (i in 0 until 10) {
            val xFrac = rnd.nextFloat() * 1.4f - 0.7f
            val yFrac = rnd.nextFloat() * 1.4f - 0.7f
            if (xFrac * xFrac + yFrac * yFrac > 0.85f) continue
            val r = moonR * (0.04f + rnd.nextFloat() * 0.1f)
            val px = cx + xFrac * moonR
            val py = cy + yFrac * moonR
            val depth = 0.15f + rnd.nextFloat() * 0.25f
            paint.color = Color.argb((depth * dim * glowPulse * 255).toInt().coerceIn(0, 255), 20, 28, 44)
            canvas.drawCircle(px, py, r, paint)
            paint.color = Color.argb(((0.3f + 0.45f * depth) * dim * glowPulse * 255).toInt().coerceIn(0, 255), 244, 247, 252)
            canvas.drawCircle(px - r * 0.18f, py - r * 0.18f, r * 0.86f, paint)
            paint.color = Color.argb((depth * dim * glowPulse * 255).toInt().coerceIn(0, 255), 11, 16, 26)
            canvas.drawCircle(px + r * 0.14f, py + r * 0.14f, r * 0.68f, paint)
        }

        paint.alpha = 255
        paint.shader = null
        paint.maskFilter = null
    }
}
