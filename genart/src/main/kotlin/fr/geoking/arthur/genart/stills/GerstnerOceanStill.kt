package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the Gerstner Ocean swell look for Auto album art. */
internal object GerstnerOceanStill {
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
            0xFF08324A.toInt(),
            0xFF01111C.toInt(),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val bandCount = 4
        val trainCount = 4
        val sampleCount = 30
        val pulseMix = 0.9f + pulse * 0.1f

        data class WaveTrain(
            val ampFrac: Float,
            val freq: Float,
            val angle: Float,
            val phaseSpeed: Float,
            val phaseOffset: Float,
        )
        data class SwellBand(
            val depthFrac: Float,
            val baseYFrac: Float,
            val colorIndex: Int,
            val trains: List<WaveTrain>,
        )

        val bands = (0 until bandCount).map { i ->
            val depthFrac = i / (bandCount - 1f)
            val bandAmpFrac = 0.018f + depthFrac * 0.05f
            val freqBase = 5f + (1f - depthFrac) * 9f
            val baseYFrac = 0.30f + depthFrac * 0.58f + (rnd.nextFloat() - 0.5f) * 0.04f
            val trains = (0 until trainCount).map {
                WaveTrain(
                    ampFrac = (0.5f + rnd.nextFloat() * 0.5f) * (bandAmpFrac / trainCount),
                    freq = (0.75f + rnd.nextFloat() * 0.5f) * freqBase,
                    angle = (rnd.nextFloat() - 0.5f) * 1.1f,
                    phaseSpeed = (0.35f + rnd.nextFloat() * 0.6f) * (if (it % 2 == 0) 1f else -0.6f),
                    phaseOffset = rnd.nextFloat() * 6.2832f,
                )
            }
            SwellBand(depthFrac = depthFrac, baseYFrac = baseYFrac, colorIndex = i, trains = trains)
        }

        val step = w / sampleCount

        fun waveY(baseYFrac: Float, trains: List<WaveTrain>, xFrac: Float): Float {
            var offset = 0f
            trains.forEach { train ->
                offset += train.ampFrac * h * pulseMix * sin(
                    train.freq * xFrac * cos(train.angle) * 6.2832f +
                        phase * train.phaseSpeed * 6.2832f +
                        train.phaseOffset,
                )
            }
            return baseYFrac * h + offset
        }

        bands.forEach { band ->
            path.reset()
            path.moveTo(0f, waveY(band.baseYFrac, band.trains, 0f))
            for (s in 1..sampleCount) {
                val x = s * step
                path.lineTo(x, waveY(band.baseYFrac, band.trains, x / w))
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()

            val base = palette.colorAt(band.colorIndex)
            val factor = (0.95f - band.depthFrac * 0.4f).coerceAtLeast(0.15f)
            val alpha = (0.35f + band.depthFrac * 0.55f).coerceIn(0.2f, 0.95f)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                (alpha * 255f).toInt().coerceIn(0, 255),
                (Color.red(base) * factor).toInt().coerceIn(0, 255),
                (Color.green(base) * factor).toInt().coerceIn(0, 255),
                (Color.blue(base) * factor).toInt().coerceIn(0, 255),
            )
            canvas.drawPath(path, paint)

            if (band.depthFrac > 0.45f) {
                val foamPath = Path()
                foamPath.moveTo(0f, waveY(band.baseYFrac, band.trains, 0f))
                for (s in 1..sampleCount) {
                    val x = s * step
                    foamPath.lineTo(x, waveY(band.baseYFrac, band.trains, x / w))
                }
                val foamAlpha = ((0.1f + band.depthFrac * 0.1f) * 255f).toInt().coerceIn(0, 56)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.4f
                paint.color = Color.argb(foamAlpha, 255, 255, 255)
                canvas.drawPath(foamPath, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
