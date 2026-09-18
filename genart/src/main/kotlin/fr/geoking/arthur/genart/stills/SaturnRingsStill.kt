package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.GenartStillFx
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Saturn and Rings for Auto/Ambient album art. */
internal object SaturnRingsStill {
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
        val cy = h * 0.52f
        val bodyR = minDim * 0.22f
        val dim = 0.65f + 0.35f * pulse

        paint.color = 0xFF03040A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        GenartStillFx.randomScatter(
            canvas, size, generation, count = 48,
            minRadiusFrac = 0.0008f, maxRadiusFrac = 0.0028f,
            alphaRange = 0.2f..0.7f,
        )

        val tint = palette.colorAt(0)
        val atmR = (Color.red(tint) + 232) / 2
        val atmG = (Color.green(tint) + 200) / 2
        val atmB = (Color.blue(tint) + 152) / 2
        val glowPulse = 0.85f + 0.15f * ((sin(phase + rotationDeg * PI.toFloat() / 180f) + 1f) / 2f)

        paint.maskFilter = BlurMaskFilter((bodyR * 0.35f).coerceAtLeast(6f), BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, bodyR * 1.55f,
            intArrayOf(
                Color.argb((0.35f * dim * glowPulse * 255).toInt().coerceIn(0, 255), atmR, atmG, atmB),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, bodyR * 1.55f, paint)
        paint.maskFilter = null
        paint.shader = null

        val ringTint = palette.colorAt(1)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        for (i in 0 until 4) {
            val rx = bodyR * (1.55f + i * 0.28f)
            val ry = rx * 0.22f
            paint.strokeWidth = bodyR * (0.04f + rnd.nextFloat() * 0.05f)
            paint.color = Color.argb(
                ((0.3f + rnd.nextFloat() * 0.25f) * dim * 255).toInt().coerceIn(0, 255),
                (Color.red(ringTint) + 200) / 2,
                (Color.green(ringTint) + 184) / 2,
                (Color.blue(ringTint) + 160) / 2,
            )
            canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint)
        }

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx - bodyR * 0.35f, cy - bodyR * 0.35f, bodyR * 1.35f,
            intArrayOf(
                Color.argb((0.95f * dim * 255).toInt().coerceIn(0, 255), atmR, atmG, atmB),
                Color.argb((0.7f * dim * 255).toInt().coerceIn(0, 255), (atmR * 0.7f).toInt(), (atmG * 0.65f).toInt(), (atmB * 0.5f).toInt()),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, bodyR, paint)
        paint.shader = null

        canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addCircle(cx, cy, bodyR, android.graphics.Path.Direction.CW)
        })
        for (i in 0 until 5) {
            val by = cy + (rnd.nextFloat() * 2f - 1f) * bodyR * 0.55f
            val bh = bodyR * (0.06f + rnd.nextFloat() * 0.08f)
            val band = palette.colorAt(i)
            paint.color = Color.argb(
                ((0.12f + rnd.nextFloat() * 0.16f) * dim * 255).toInt().coerceIn(0, 255),
                Color.red(band), Color.green(band), Color.blue(band),
            )
            canvas.drawOval(RectF(cx - bodyR, by - bh * 0.5f, cx + bodyR, by + bh * 0.5f), paint)
        }
        canvas.restore()

        paint.style = Paint.Style.STROKE
        for (i in 0 until 4) {
            val rx = bodyR * (1.55f + i * 0.28f)
            val ry = rx * 0.22f
            paint.strokeWidth = bodyR * (0.035f + rnd.nextFloat() * 0.04f)
            paint.color = Color.argb(
                ((0.35f + rnd.nextFloat() * 0.2f) * dim * 255).toInt().coerceIn(0, 255),
                (Color.red(ringTint) + 200) / 2,
                (Color.green(ringTint) + 184) / 2,
                (Color.blue(ringTint) + 160) / 2,
            )
            canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
