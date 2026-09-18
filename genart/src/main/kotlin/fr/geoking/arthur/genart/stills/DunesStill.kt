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

/** Bakes one frozen frame of "Wind-Blown Dunes" for Android Auto album art — deterministic per [generation]. */
internal object DunesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

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
            intArrayOf(0xFF241A12.toInt(), 0xFF120C10.toInt(), 0xFF05040A.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val layerCount = 5
        val layers = (0 until layerCount).map { i ->
            val depthFrac = i / (layerCount - 1f)
            DuneLayerStillSeed(
                depthFrac = depthFrac,
                baseYFrac = 0.42f + depthFrac * 0.36f + (rnd.nextFloat() - 0.5f) * 0.06f,
                ampFrac1 = 0.012f + rnd.nextFloat() * 0.02f,
                freq1 = 0.006f + rnd.nextFloat() * 0.007f,
                phase1 = rnd.nextFloat() * 2f * PI.toFloat(),
                ampFrac2 = 0.006f + rnd.nextFloat() * 0.01f,
                freq2 = 0.014f + rnd.nextFloat() * 0.012f,
                phase2 = rnd.nextFloat() * 2f * PI.toFloat(),
                driftSpeed = 0.015f + rnd.nextFloat() * 0.035f,
                colorIndex = i,
            )
        }

        val segments = (w / 8f).toInt().coerceIn(16, 160)
        val step = w / segments
        val pulseMix = 0.9f + pulse * 0.1f

        layers.forEach { layer ->
            val driftPhase = phase * layer.driftSpeed * 2f * PI.toFloat()
            val baseY = layer.baseYFrac * h
            val amp1 = layer.ampFrac1 * h * pulseMix
            val amp2 = layer.ampFrac2 * h * pulseMix

            path.reset()
            val firstY = baseY +
                amp1 * sin(layer.phase1 + driftPhase) +
                amp2 * sin(layer.phase2 + driftPhase)
            path.moveTo(0f, firstY)
            for (s in 1..segments) {
                val x = s * step
                val y = baseY +
                    amp1 * sin(x * layer.freq1 + layer.phase1 + driftPhase) +
                    amp2 * sin(x * layer.freq2 + layer.phase2 + driftPhase)
                path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            val base = palette.colorAt(layer.colorIndex)
            val factor = 0.4f + layer.depthFrac * 0.6f
            val alpha = (0.35f + layer.depthFrac * 0.6f).coerceIn(0.2f, 0.95f)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(base) * factor).toInt().coerceIn(0, 255),
                (Color.green(base) * factor).toInt().coerceIn(0, 255),
                (Color.blue(base) * factor).toInt().coerceIn(0, 255),
            )
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255

        val grainCount = 20
        val sandColor = palette.primary
        for (i in 0 until grainCount) {
            val x0 = rnd.nextFloat()
            val y0Frac = rnd.nextFloat() * 0.54f + 0.08f
            val jitterAmpFrac = rnd.nextFloat() * 0.014f + 0.004f
            val jitterFreq = rnd.nextFloat() * 1.6f + 0.6f
            val jitterPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val length = rnd.nextFloat() * 8f + 4f

            val x = (((x0 + phase * 0.4f) % 1f + 1f) % 1f) * w
            val y = y0Frac * h + sin(phase * jitterFreq * 2f * PI.toFloat() + jitterPhase) * jitterAmpFrac * h
            val alpha = ((0.12f + 0.18f * (1f - y0Frac)) * 255).toInt().coerceIn(0, 140)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.4f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.argb(
                alpha,
                Color.red(sandColor),
                Color.green(sandColor),
                Color.blue(sandColor),
            )
            canvas.drawLine(x - length, y, x, y, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}

private data class DuneLayerStillSeed(
    val depthFrac: Float,
    val baseYFrac: Float,
    val ampFrac1: Float,
    val freq1: Float,
    val phase1: Float,
    val ampFrac2: Float,
    val freq2: Float,
    val phase2: Float,
    val driftSpeed: Float,
    val colorIndex: Int,
)
