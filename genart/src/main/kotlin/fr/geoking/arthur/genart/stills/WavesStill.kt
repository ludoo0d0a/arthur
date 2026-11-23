package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.engines.wavesHeight
import fr.geoking.arthur.genart.engines.wavesSegments
import kotlin.math.sin

/** Ported from Julius WavesEffectSurface — single fixed frame. */
internal object WavesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val outlinePath = Path()

    fun draw(
        canvas: Canvas,
        size: Int,
        phaseBase: Float,
        palette: AnimationPalette,
        isActive: Boolean = true,
    ) {
        if (size <= 0) return
        val centerX = size * 0.5f
        val centerY = size * 0.5f
        val amplitudeMult = if (isActive) 1.5f else 1f

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            size.toFloat(),
            intArrayOf(0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 0.33f, 0.66f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        // The still API supplies one phase, not the live engine's three independent clocks.
        // Use it for each layer so preview sequences remain periodic and coincide at phase zero.
        drawWaveLayer(canvas, size, phaseBase, 40f * amplitudeMult, 0.015f, palette.primary, centerY * 0.3f, 0.6f, isActive)
        drawWaveLayer(canvas, size, phaseBase, 50f * amplitudeMult, 0.012f, palette.secondary, centerY * 0.6f, 0.5f, isActive)
        drawWaveLayer(canvas, size, phaseBase, 45f * amplitudeMult, 0.010f, palette.tertiary, centerY * 0.9f, 0.4f, isActive)
        if (isActive) {
            drawWaveLayer(canvas, size, phaseBase, 35f * amplitudeMult, 0.018f, palette.quaternary, centerY * 0.45f, 0.3f, true)
            drawWaveLayer(canvas, size, phaseBase, 55f * amplitudeMult, 0.008f, palette.quinary, centerY * 1.15f, 0.3f, true)
        }

        val maxRadius = size * 0.8f
        val waveCount = if (isActive) 8 else 5
        for (i in 0 until waveCount) {
            val radius = ((maxRadius / waveCount) * (i + 1) + sin(phaseBase + i) * 20f).coerceAtLeast(1f)
            val alpha = (1f - i / waveCount.toFloat()) * 0.15f
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                Color.red(palette.primary),
                Color.green(palette.primary),
                Color.blue(palette.primary),
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (isActive) 3f else 2f
            paint.shader = null
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawWaveLayer(
        canvas: Canvas,
        size: Int,
        phase: Float,
        amplitude: Float,
        frequency: Float,
        color: Int,
        centerY: Float,
        alpha: Float,
        isActive: Boolean,
    ) {
        path.reset()
        outlinePath.reset()
        val segments = wavesSegments(size.toFloat(), GenartQuality.High)
        val step = size.toFloat() / segments
        for (i in 0..segments) {
            val x = i * step
            val y = wavesHeight(x, centerY, amplitude, frequency, phase)
            if (i == 0) {
                path.moveTo(x, y)
                outlinePath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                outlinePath.lineTo(x, y)
            }
        }
        path.lineTo(size.toFloat(), size.toFloat())
        path.lineTo(0f, size.toFloat())
        path.close()

        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val red = if (isActive) (Color.red(color) * 0.7f + 76.5f).toInt() else Color.red(color)
        val green = if (isActive) (Color.green(color) * 0.7f + 76.5f).toInt() else Color.green(color)
        val blue = if (isActive) (Color.blue(color) * 0.7f + 76.5f).toInt() else Color.blue(color)
        paint.shader = LinearGradient(
            0f,
            centerY - amplitude,
            0f,
            size.toFloat(),
            intArrayOf(
                Color.argb(a, red, green, blue),
                Color.argb(a / 2, red, green, blue),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.color = Color.argb(
            ((alpha + 0.2f).coerceIn(0f, 1f) * 255).toInt(),
            red, green, blue,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(outlinePath, paint)
        paint.style = Paint.Style.FILL
    }
}
