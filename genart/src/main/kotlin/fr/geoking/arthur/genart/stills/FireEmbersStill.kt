package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one calm, cozy frame of the Fireplace Embers look — deterministic for a given [generation]. */
internal object FireEmbersStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val EMBER_COUNT = 40

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
            (40f + Color.red(primary) * 0.18f).toInt().coerceIn(0, 255),
            (14f + Color.green(primary) * 0.10f).toInt().coerceIn(0, 255),
            (8f + Color.blue(primary) * 0.06f).toInt().coerceIn(0, 255),
        )
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF040202.toInt(), 0xFF0A0503.toInt(), warmBottom),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val rnd = Random(generation)
        val glowMult = 0.75f + 0.25f * pulse.coerceIn(0f, 1f)
        val time = phase * 2f * PI.toFloat()

        for (i in 0 until EMBER_COUNT) {
            val startX = rnd.nextFloat()
            val phaseOffset = rnd.nextFloat()
            val riseSpeedFactor = rnd.nextFloat() * 0.8f + 0.6f
            val jitterAmp = rnd.nextFloat() * 0.03f + 0.01f
            val jitterFreq = rnd.nextFloat() * 2f + 0.5f
            val flickerFreq = rnd.nextFloat() * 2f + 0.5f
            val flickerPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val radius = rnd.nextFloat() * 6f + 3f

            val raw = phase * riseSpeedFactor + phaseOffset
            val life = raw - floor(raw)

            val jitterX = jitterAmp * sin(time * jitterFreq + phaseOffset * 10f)
            val x = (startX + jitterX) * w
            val y = (1f - life) * h

            val flicker = 0.75f + 0.25f * sin(time * flickerFreq + flickerPhase)
            val fadeIn = (life / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - life) / 0.55f).coerceIn(0f, 1f)
            val alphaLife = (fadeIn * fadeOut).coerceIn(0f, 1f)

            val base = palette.colorAt(i)
            val brightnessMult = (flicker * glowMult).coerceIn(0f, 1.4f)
            val r = (Color.red(base) * brightnessMult).toInt().coerceIn(0, 255)
            val g = (Color.green(base) * brightnessMult).toInt().coerceIn(0, 255)
            val b = (Color.blue(base) * brightnessMult).toInt().coerceIn(0, 255)
            val alpha = (alphaLife * 255).toInt().coerceIn(0, 255)

            val glowRadius = radius * 3.2f
            paint.shader = RadialGradient(
                x,
                y,
                glowRadius.coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, glowRadius, paint)
            paint.shader = null

            paint.color = Color.argb(alpha, r, g, b)
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.alpha = 255
    }
}
