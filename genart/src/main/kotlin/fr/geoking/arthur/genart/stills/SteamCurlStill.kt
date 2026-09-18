package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Steam Curl" for Auto/Ambient album art. */
internal object SteamCurlStill {
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
            intArrayOf(0xFF14100C.toInt(), 0xFF0A0806.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val dim = 0.7f + 0.3f * pulse
        val wispCount = 4
        val segments = 24

        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.maskFilter = BlurMaskFilter((size * 0.006f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)

        for (i in 0 until wispCount) {
            val baseXFrac = 0.5f + (rnd.nextFloat() - 0.5f) * 0.12f
            val baseYFrac = 0.8f + rnd.nextFloat() * 0.1f
            val riseHeightFrac = 0.42f + rnd.nextFloat() * 0.2f
            val phaseOffset = rnd.nextFloat()
            val windowWidth = 0.3f + rnd.nextFloat() * 0.12f
            val freq1 = 1.1f + rnd.nextFloat() * 0.8f
            val phase1 = rnd.nextFloat() * 2f * PI.toFloat()
            val amp1Frac = 0.03f + rnd.nextFloat() * 0.03f
            val freq2 = 2.2f + rnd.nextFloat() * 1.2f
            val phase2 = rnd.nextFloat() * 2f * PI.toFloat()
            val amp2Frac = 0.012f + rnd.nextFloat() * 0.016f
            val widthFrac = 0.01f + rnd.nextFloat() * 0.008f
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 244) / 2
            val g = (Color.green(tint) + 234) / 2
            val b = (Color.blue(tint) + 220) / 2

            val baseX = baseXFrac * w
            val baseY = baseYFrac * h
            val center = (((drift * 0.05f + phaseOffset) % 1f) + 1f) % 1f

            var prevX = baseX
            var prevY = baseY
            for (k in 0..segments) {
                val s = k / segments.toFloat()
                val angle1 = s * freq1 * 2f * PI.toFloat() + drift * 0.6f + phase1
                val angle2 = s * freq2 * 2f * PI.toFloat() + drift * 0.9f + phase2
                val wobble = (sin(angle1) * amp1Frac + sin(angle2) * amp2Frac) * s * minDim
                val x = baseX + wobble
                val y = baseY - s * riseHeightFrac * h

                val dist = s - center
                val windowed = dist / windowWidth
                val envelope = (1f - windowed * windowed).coerceAtLeast(0f)
                val fade = (1f - s).pow(1.6f)
                val alpha = (envelope * fade * 0.6f * dim).coerceIn(0f, 1f)
                val strokeWidth = (widthFrac * minDim * (1f - s).pow(1.3f)).coerceAtLeast(0.4f)

                if (k > 0 && alpha > 0.01f) {
                    paint.strokeWidth = strokeWidth
                    paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
                    canvas.drawLine(prevX, prevY, x, y, paint)
                }
                prevX = x
                prevY = y
            }
        }
        paint.maskFilter = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
