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

/** Bakes one calm, cozy frame of a single candle-flame glow — deterministic for a given [generation]. */
internal object CandleEmberStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val HALO_COUNT = 3

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val w = size.toFloat()
        val h = size.toFloat()

        val primary = palette.primary
        val warmBottom = Color.rgb(
            (42f + Color.red(primary) * 0.18f).toInt().coerceIn(0, 255),
            (18f + Color.green(primary) * 0.10f).toInt().coerceIn(0, 255),
            (6f + Color.blue(primary) * 0.06f).toInt().coerceIn(0, 255),
        )
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF040203.toInt(), 0xFF0A0603.toInt(), warmBottom),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase * 2f * PI.toFloat()
        val flicker = 0.5f +
            0.2f * sin(time * 1.3f) +
            0.18f * sin(time * 2.1f + 1.7f) +
            0.12f * sin(time * 0.7f + 4.1f)
        val flickerNorm = flicker.coerceIn(0.15f, 1f)
        val glowMult = (0.7f + 0.3f * flickerNorm) * (0.75f + 0.25f * pulse.coerceIn(0f, 1f))

        val base = palette.colorAt(0)
        val r = (Color.red(base) * 0.55f + 255f * 0.45f).toInt().coerceIn(0, 255)
        val g = (Color.green(base) * 0.55f + 194f * 0.45f).toInt().coerceIn(0, 255)
        val b = (Color.blue(base) * 0.55f + 102f * 0.45f).toInt().coerceIn(0, 255)

        val cx = w * 0.5f
        val cy = h * 0.72f
        val coreRadius = (w.coerceAtMost(h) * 0.05f) * (0.9f + 0.15f * flickerNorm)

        for (halo in 0 until HALO_COUNT) {
            val haloScale = 1f + halo * 1.6f
            val haloRadius = (coreRadius * (2.4f + haloScale * 1.8f)).coerceAtLeast(1f)
            val haloAlpha = ((0.5f / (halo + 1)) * glowMult).coerceIn(0f, 1f)
            val alphaInt = (haloAlpha * 255).toInt().coerceIn(0, 255)
            val rr = (r * glowMult).toInt().coerceIn(0, 255)
            val gg = (g * glowMult).toInt().coerceIn(0, 255)
            val bb = (b * glowMult).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                cx,
                cy,
                haloRadius,
                Color.argb(alphaInt, rr, gg, bb),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, haloRadius, paint)
            paint.shader = null
        }

        val coreAlpha = (0.85f * 255).toInt().coerceIn(0, 255)
        val coreMult = (glowMult * 1.15f).coerceAtMost(1.4f)
        val coreR = (r * coreMult).toInt().coerceIn(0, 255)
        val coreG = (g * coreMult).toInt().coerceIn(0, 255)
        val coreB = (b * coreMult).toInt().coerceIn(0, 255)
        paint.color = Color.argb(coreAlpha, coreR, coreG, coreB)
        canvas.drawCircle(cx, cy * 0.94f, coreRadius, paint)
        paint.alpha = 255
    }
}
