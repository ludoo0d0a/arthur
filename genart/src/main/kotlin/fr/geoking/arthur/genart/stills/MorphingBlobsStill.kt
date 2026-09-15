package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Morphing Blobs for Auto/Ambient album art. */
internal object MorphingBlobsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

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
        val minDim = min(w, h)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.85f,
            intArrayOf(0xFF0C0A14.toInt(), 0xFF040308.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val shadowPx = minDim * 0.018f

        for (i in 0 until 10) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val driftX = 0.02f + rnd.nextFloat() * 0.09f
            val driftY = 0.015f + rnd.nextFloat() * 0.075f
            val radiusFrac = 0.10f + rnd.nextFloat() * 0.18f
            val aspect = 0.55f + rnd.nextFloat() * 1.3f
            val corner = 0.35f + rnd.nextFloat() * 0.2f
            val rot0 = rnd.nextFloat() * 2f * PI.toFloat()
            val rotSpeed = (rnd.nextFloat() - 0.5f) * 0.3f
            val pulseFreq = 0.15f + rnd.nextFloat() * 0.4f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.35f + rnd.nextFloat() * 0.35f
            val colorMix = rnd.nextFloat()
            val capsule = rnd.nextFloat() > 0.42f
            val x = (((x0 + loop * driftX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * driftY) % 1f) + 1f) % 1f * h
            val pulseMul = 0.82f + 0.18f * sin(loop * 2f * PI.toFloat() * pulseFreq + pulsePhase)
            val base = radiusFrac * minDim * pulseMul
            val bw = if (capsule) base * aspect else base * 2f
            val bh = if (capsule) base else base * 2f
            val tint = mixArgb(palette.colorAt(i), palette.colorAt(i + 2), colorMix)
            val rotDeg = Math.toDegrees((rot0 + loop * 2f * PI.toFloat() * rotSpeed).toDouble()).toFloat()
            canvas.save()
            canvas.rotate(rotDeg, x, y)
            paint.color = Color.argb((0.32f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            rect.set(
                x - bw * 0.5f + shadowPx * 0.55f,
                y - bh * 0.5f + shadowPx,
                x + bw * 0.5f + shadowPx * 0.55f,
                y + bh * 0.5f + shadowPx,
            )
            canvas.drawRoundRect(rect, bw * corner, bh * corner, paint)
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x - bw * 0.12f, y - bh * 0.18f, maxOf(bw, bh) * 0.85f,
                intArrayOf(
                    Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((alpha * 0.55f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((alpha * 0.12f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            rect.set(x - bw * 0.5f, y - bh * 0.5f, x + bw * 0.5f, y + bh * 0.5f)
            canvas.drawRoundRect(rect, bw * corner, bh * corner, paint)
            paint.shader = null
            canvas.restore()
        }
        paint.alpha = 255
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }
}
