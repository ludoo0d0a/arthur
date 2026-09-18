package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one calm frame of a towering flame — deterministic for a given [generation]. */
internal object FireStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val TONGUE_COUNT = 5

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
        val minDim = w.coerceAtMost(h)
        val rnd = Random(generation)
        val warmBottom = Color.rgb(
            (10f + Color.red(palette.primary) * 0.2f).toInt().coerceIn(0, 255),
            (5f + Color.green(palette.primary) * 0.12f).toInt().coerceIn(0, 255),
            (2f + Color.blue(palette.primary) * 0.06f).toInt().coerceIn(0, 255),
        )
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF050201.toInt(), 0xFF0A0402.toInt(), warmBottom),
            floatArrayOf(0f, 0.15f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val glowMult = 0.75f + 0.25f * pulse.coerceIn(0f, 1f)
        val time = phase * 2f * PI.toFloat()
        val drift = rotationDeg * PI.toFloat() / 180f

        for (i in 0 until TONGUE_COUNT) {
            val baseXFrac = rnd.nextFloat()
            val widthFrac = rnd.nextFloat() * 0.08f + 0.04f
            val heightFrac = rnd.nextFloat() * 0.45f + 0.55f
            val swayAmp = rnd.nextFloat() * 0.06f + 0.02f
            val flickerFreq = rnd.nextFloat() * 2.5f + 1.5f
            val flickerPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val colorIndex = i

            val sway = sin(time * 1.2f + flickerPhase) * swayAmp
            val breath = 0.85f + 0.15f * sin(time * 0.4f + flickerPhase * 0.5f)
            val height = heightFrac * breath * minDim
            val halfWidth = widthFrac * minDim
            val tipY = (1f - height / minDim * 0.9f) * h
            val baseY = h - h * 0.08f
            val baseX = (baseXFrac + sway) * w

            val tint = palette.colorAt(colorIndex)
            val r = (Color.red(tint) * 0.55f + 255f * 0.45f).toInt().coerceIn(0, 255)
            val g = (Color.green(tint) * 0.55f + 194f * 0.45f).toInt().coerceIn(0, 255)
            val b = (Color.blue(tint) * 0.55f + 102f * 0.45f).toInt().coerceIn(0, 255)
            val flicker = 0.7f + 0.3f * sin(time * flickerFreq + flickerPhase)
            val alphaMult = glowMult * flicker

            val outerAlpha = (alphaMult * 0.55f * 255).toInt().coerceIn(0, 255)
            val coreAlpha = (alphaMult * 0.85f * 255).toInt().coerceIn(0, 255)
            val smokeAlpha = (alphaMult * 0.2f * 255).toInt().coerceIn(0, 255)

            paint.shader = LinearGradient(
                baseX, tipY, baseX, baseY,
                intArrayOf(
                    Color.argb(0, r, g, b),
                    Color.argb(smokeAlpha, r, g, b),
                    Color.argb((outerAlpha * 0.7f).toInt().coerceIn(0, 255), r, g, b),
                    Color.argb(coreAlpha, r, g, b),
                    Color.argb(0, r, g, b),
                ),
                floatArrayOf(0f, 0.1f, 0.4f, 0.75f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            val path = Path().apply {
                moveTo(baseX - halfWidth * flicker, baseY)
                cubicTo(
                    baseX - halfWidth * 1.3f * flicker, tipY + height * 0.3f,
                    baseX + halfWidth * 1.3f * flicker, tipY + height * 0.3f,
                    baseX + halfWidth * flicker, tipY
                )
                close()
            }
            canvas.drawPath(path, paint)
            paint.shader = null

            paint.shader = RadialGradient(
                baseX, tipY + height * 0.15f,
                (halfWidth * 0.5f * flicker).coerceAtLeast(1f),
                Color.argb(coreAlpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(baseX, tipY + height * 0.15f, (halfWidth * 0.5f * flicker).coerceAtLeast(1f), paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
