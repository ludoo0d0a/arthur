package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one calm, cozy frame of a single candle-flame glow — deterministic for a given [generation]. */
internal object CandleEmberStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val HALO_COUNT = 3
    private const val EMBER_COUNT = 6

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val rnd = Random(generation xor 0x5EED5EEDL)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)

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

        val loop = phase01((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val time = loop * 2f * PI.toFloat()
        val flicker = 0.5f +
            0.2f * sin(time * 1.3f) +
            0.18f * sin(time * 2.1f + 1.7f) +
            0.12f * sin(time * 0.7f + 4.1f) +
            0.08f * sin(time * 4.3f + 2.2f)
        val flickerNorm = flicker.coerceIn(0.15f, 1f)
        val glowMult = (0.7f + 0.3f * flickerNorm) * (0.75f + 0.25f * pulse.coerceIn(0f, 1f))

        val jitterX = sin(time * 3.7f) * w * 0.008f + sin(time * 6.1f + 1.2f) * w * 0.004f
        val jitterY = sin(time * 2.9f + 0.8f) * h * 0.006f
        val cx = w * 0.5f + jitterX
        val cy = h * 0.72f + jitterY

        val base = palette.colorAt(0)
        val r = (Color.red(base) * 0.55f + 255f * 0.45f).toInt().coerceIn(0, 255)
        val g = (Color.green(base) * 0.55f + 194f * 0.45f).toInt().coerceIn(0, 255)
        val b = (Color.blue(base) * 0.55f + 102f * 0.45f).toInt().coerceIn(0, 255)

        val coreRadius = (minDim * 0.05f) * (0.9f + 0.15f * flickerNorm)

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

        for (i in 0 until EMBER_COUNT) {
            val x0 = (rnd.nextFloat() - 0.5f) * 0.16f
            val phaseOffset = rnd.nextFloat()
            val riseSpeed = 0.6f + rnd.nextFloat() * 0.7f
            val sizeFrac = 0.004f + rnd.nextFloat() * 0.006f
            val alphaBase = 0.25f + rnd.nextFloat() * 0.3f
            val life = phase01(loop * riseSpeed + phaseOffset)
            val ex = cx + x0 * w + sin(time * 2.2f + phaseOffset * 6f) * w * 0.015f
            val ey = cy - life * h * 0.28f
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.12f).coerceIn(0f, 1f)
            val emberTint = palette.colorAt(i)
            val er = ((Color.red(emberTint) + 255) / 2).coerceIn(0, 255)
            val eg = ((Color.green(emberTint) + 224) / 2).coerceIn(0, 255)
            val eb = ((Color.blue(emberTint) + 160) / 2).coerceIn(0, 255)
            val emberAlpha = (alphaBase * fade * glowMult * flickerNorm * 255).toInt().coerceIn(0, 255)
            val emberR = (sizeFrac * minDim * (1f - life * 0.4f)).coerceAtLeast(0.8f)
            paint.color = Color.argb(emberAlpha, er, eg, eb)
            canvas.drawCircle(ex, ey, emberR, paint)
        }

        paint.alpha = 255
    }
}