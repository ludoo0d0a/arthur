package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ported from Julius ParticlesEffectSurface — deterministic for a given [seed].
 */
internal object ParticlesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    fun draw(
        canvas: Canvas,
        size: Int,
        seed: Long,
        time: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
        isActive: Boolean = true,
    ) {
        val rnd = Random(seed)
        val centerX = size * 0.5f
        val centerY = size * 0.5f
        val pulseScale = if (isActive) 0.5f + 0.5f * pulse else 1f
        val scale = if (isActive) 1.2f else 1f

        paint.shader = RadialGradient(
            centerX,
            centerY,
            size * 0.8f,
            intArrayOf(0xFF1E1B4B.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        canvas.save()
        canvas.rotate(rotationDeg, centerX, centerY)
        canvas.scale(pulseScale, pulseScale, centerX, centerY)

        val rayColor = palette.primary
        val rayAlpha = if (isActive) 0.15f else 0.05f
        val rayCount = 8
        for (i in 0 until rayCount) {
            val angleOffset = rnd.nextFloat() * 360f
            val width = rnd.nextFloat() * 30f + 10f
            val angleRad = (angleOffset + rotationDeg) * PI.toFloat() / 180f
            val endX = centerX + cos(angleRad) * size
            val endY = centerY + sin(angleRad) * size
            paint.color = Color.argb(
                (rayAlpha * 255).toInt().coerceIn(0, 255),
                Color.red(rayColor),
                Color.green(rayColor),
                Color.blue(rayColor),
            )
            paint.strokeWidth = width * (if (isActive) 2f else 1f)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(centerX, centerY, endX, endY, paint)
        }
        canvas.restore()

        val particleCount = 50
        for (i in 0 until particleCount) {
            var angle = rnd.nextFloat() * 2 * PI.toFloat()
            val radius = rnd.nextFloat() * 150f + 40f
            val speed = rnd.nextFloat() * 2f + 0.5f
            val pSize = rnd.nextFloat() * 4f + 2f
            val alpha = rnd.nextFloat() * 0.5f + 0.3f
            val colorIndex = rnd.nextInt()
            val paletteColors = palette.colors
            val baseColor = if (paletteColors.isNotEmpty()) {
                paletteColors[abs(colorIndex) % paletteColors.size]
            } else {
                0xFF6366F1.toInt()
            }
            val mult = if (isActive) 3f else 1f
            // Freeze one frame of motion using [time] as phase offset.
            angle += speed * 0.01f * mult * (1f + time * 40f)
            val wobble = sin(time * 50f + angle) * (if (isActive) 20f else 5f)
            val r = radius + wobble
            val x = centerX + cos(angle) * r * scale
            val y = centerY + sin(angle) * r * scale
            if (isActive) {
                paint.color = baseColor
                paint.alpha = (0.1f * alpha * 255).toInt().coerceIn(0, 255)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x, y, pSize * 4 * pulseScale, paint)
            }
            paint.color = if (isActive) Color.WHITE else baseColor
            paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, pSize * (if (isActive) 1.5f else 1f), paint)
        }
        paint.alpha = 255
    }
}
