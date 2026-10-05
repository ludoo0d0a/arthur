package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Glass Marbles for Auto/Ambient album art. */
internal object GlassMarblesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val oval = RectF()

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
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.2f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A1620.toInt(), 0xFF0C0A10.toInt(), 0xFF050408.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.78f, (minDim * 0.7f).coerceAtLeast(1f),
            Color.argb(89, 42, 36, 52),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.78f, minDim * 0.7f, paint)
        paint.shader = null

        data class Marble(
            val x0: Float,
            val y0: Float,
            val radiusFrac: Float,
            val rollAmp: Float,
            val rollFreq: Float,
            val rollPhase: Float,
            val swirlAngle: Float,
            val swirlSpin: Float,
            val highlightAngle: Float,
            val alpha: Float,
            val colorIndex: Int,
        )

        val marbles = List(11) { i ->
            Marble(
                x0 = 0.12f + rnd.nextFloat() * 0.76f,
                y0 = 0.38f + rnd.nextFloat() * 0.44f,
                radiusFrac = 0.055f + rnd.nextFloat() * 0.065f,
                rollAmp = 0.008f + rnd.nextFloat() * 0.02f,
                rollFreq = 0.15f + rnd.nextFloat() * 0.3f,
                rollPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                swirlAngle = rnd.nextFloat() * 2f * PI.toFloat(),
                swirlSpin = 0.2f + rnd.nextFloat() * 0.5f,
                highlightAngle = -0.9f + rnd.nextFloat() * 0.55f,
                alpha = 0.45f + rnd.nextFloat() * 0.3f,
                colorIndex = i,
            )
        }.sortedBy { it.y0 }

        marbles.forEach { m ->
            val roll = m.rollAmp * sin(time * m.rollFreq + m.rollPhase)
            val cx = (m.x0 + roll) * w
            val cy = m.y0 * h
            val r = m.radiusFrac * minDim
            val tint = palette.colorAt(m.colorIndex)
            val tr = (Color.red(tint) + 232) / 2
            val tg = (Color.green(tint) + 240) / 2
            val tb = (Color.blue(tint) + 255) / 2
            val a = m.alpha * dim

            oval.set(cx - r * 0.95f, cy + r * 0.55f, cx + r * 0.95f, cy + r * 1.1f)
            paint.shader = RadialGradient(
                cx, cy + r * 0.85f, (r * 1.1f).coerceAtLeast(1f),
                Color.argb((a * 0.28f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(oval, paint)
            paint.shader = null

            paint.shader = RadialGradient(
                cx - r * 0.28f, cy - r * 0.32f, (r * 1.15f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((a * 0.55f * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.argb((a * 0.7f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                    Color.argb((a * 0.25f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                    Color.argb((a * 0.55f * 255).toInt().coerceIn(0, 255), 16, 16, 24),
                ),
                floatArrayOf(0f, 0.25f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            val swirl = m.swirlAngle + time * m.swirlSpin
            val sx = cx + cos(swirl) * r * 0.22f
            val sy = cy + sin(swirl) * r * 0.18f
            paint.shader = RadialGradient(
                sx, sy, (r * 0.55f).coerceAtLeast(1f),
                Color.argb((a * 0.55f * 255).toInt().coerceIn(0, 255), tr, tg, tb),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(sx, sy, r * 0.55f, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (r * 0.04f).coerceAtLeast(0.8f)
            paint.color = Color.argb((a * 0.35f * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(cx, cy, r, paint)
            paint.style = Paint.Style.FILL

            val hx = cx + cos(m.highlightAngle) * r * 0.38f
            val hy = cy + sin(m.highlightAngle) * r * 0.38f
            paint.shader = RadialGradient(
                hx, hy, (r * 0.28f).coerceAtLeast(1f),
                Color.argb((a * 0.95f * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(hx, hy, r * 0.28f, paint)
            paint.shader = null

            paint.color = Color.argb((a * 0.45f * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(cx + r * 0.42f, cy - r * 0.15f, r * 0.06f, paint)
        }
        paint.alpha = 255
        paint.shader = null
    }
}
