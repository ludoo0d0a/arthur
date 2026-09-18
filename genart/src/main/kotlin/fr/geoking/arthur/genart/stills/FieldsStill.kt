package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one static frame of "Soft Fields" for Android Auto album art — deterministic per [generation]. */
internal object FieldsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val wheatGold = Color.rgb(0xD9, 0xB6, 0x6B)
    private val sageGreen = Color.rgb(0x9C, 0xAA, 0x7A)
    private val oliveColor = Color.rgb(0x7C, 0x7A, 0x45)
    private val dustyRose = Color.rgb(0xC4, 0x8D, 0x82)
    private val warmFieldColors = intArrayOf(wheatGold, sageGreen, oliveColor, dustyRose)
    private const val BAND_COUNT = 5
    private const val SAMPLE_COUNT = 7

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

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFFCFE0E8.toInt(), 0xFFE9DFC4.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase * 2f * PI.toFloat()
        val pulseMix = 0.85f + pulse * 0.15f

        for (i in 0 until BAND_COUNT) {
            val phaseOffset = rnd.nextFloat() * (2f * PI.toFloat())
            val swayAmp = 0.25f + rnd.nextFloat() * 0.3f
            val freq = 1.4f + rnd.nextFloat() * 1.2f

            val topFrac = i / BAND_COUNT.toFloat()
            val bottomFrac = (i + 1) / BAND_COUNT.toFloat()
            val bandHeight = (bottomFrac - topFrac) * h
            val amplitude = bandHeight * swayAmp * 0.5f * pulseMix
            val bottomY = bottomFrac * h

            path.reset()
            path.moveTo(0f, bottomY)
            for (j in 0..SAMPLE_COUNT) {
                val xFrac = j / SAMPLE_COUNT.toFloat()
                val x = xFrac * w
                val wave = sin(time + phaseOffset + xFrac * freq) * amplitude
                val y = topFrac * h + wave
                path.lineTo(x, y)
            }
            path.lineTo(w, bottomY)
            path.close()

            val warmBase = warmFieldColors[i % warmFieldColors.size]
            val tint = palette.colorAt(i)
            val mixed = mixArgb(warmBase, tint, 0.35f)
            val depthFactor = 0.55f + 0.45f * (i / (BAND_COUNT - 1).coerceAtLeast(1).toFloat())
            val color = scaleBrightness(mixed, depthFactor)

            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun scaleBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
