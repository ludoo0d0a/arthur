package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.cos
import kotlin.math.sin

private const val MICRO_RAY_WIDTH_FRAC = 0.22f
private const val MICRO_RAY_PRIMARY_ALPHA_ACTIVE = 0.26f
private const val MICRO_RAY_PRIMARY_ALPHA_IDLE = 0.14f
private const val MICRO_RAY_STREAK_ALPHA_ACTIVE = 0.22f
private const val MICRO_RAY_STREAK_ALPHA_IDLE = 0.11f
private const val MICRO_RAY_SECONDARY_ALPHA_ACTIVE = 0.18f
private const val MICRO_RAY_SECONDARY_ALPHA_IDLE = 0.09f

/**
 * Fixed-phase Micro rays still — adapted from Julius MicroEffectCanvas
 * (Julius has no Auto Micro surface).
 */
internal object MicroStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    fun draw(
        canvas: Canvas,
        size: Int,
        rotationDeg: Float,
        pulseScale: Float,
        palette: AnimationPalette,
        isActive: Boolean = true,
    ) {
        val centerX = size * 0.5f
        val centerY = size * 0.5f
        val maxRadius = size * 1.2f
        val primaryColor = palette.primary
        val secondaryColor = palette.secondary
        val baseColor = if (isActive) 0xFF2E1065.toInt() else 0xFF1A0038.toInt()
        val primaryRayAlpha = if (isActive) MICRO_RAY_PRIMARY_ALPHA_ACTIVE else MICRO_RAY_PRIMARY_ALPHA_IDLE
        val streakAlpha = if (isActive) MICRO_RAY_STREAK_ALPHA_ACTIVE else MICRO_RAY_STREAK_ALPHA_IDLE
        val secondaryRayAlpha = if (isActive) MICRO_RAY_SECONDARY_ALPHA_ACTIVE else MICRO_RAY_SECONDARY_ALPHA_IDLE
        val centerVignetteAlpha = if (isActive) 0.52f else 0.32f
        val edgeVignetteAlpha = 0.38f

        canvas.drawColor(0xFF0D001A.toInt())

        canvas.save()
        canvas.rotate(rotationDeg, centerX, centerY)
        val rayCount = 8
        for (i in 0 until rayCount) {
            val angle = i * 360f / rayCount
            canvas.save()
            canvas.rotate(angle, centerX, centerY)
            paint.shader = RadialGradient(
                centerX,
                centerY,
                maxRadius * pulseScale,
                intArrayOf(
                    Color.argb(
                        (primaryRayAlpha * 255).toInt().coerceIn(0, 255),
                        Color.red(primaryColor),
                        Color.green(primaryColor),
                        Color.blue(primaryColor),
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            paint.alpha = (0.78f * 255).toInt()
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            paint.shader = null

            val rayWidth = size * MICRO_RAY_WIDTH_FRAC
            paint.shader = LinearGradient(
                centerX - rayWidth,
                0f,
                centerX + rayWidth,
                0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(
                        (streakAlpha * 255).toInt().coerceIn(0, 255),
                        Color.red(primaryColor),
                        Color.green(primaryColor),
                        Color.blue(primaryColor),
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(
                centerX - rayWidth,
                centerY - maxRadius,
                centerX + rayWidth,
                centerY + maxRadius,
                paint,
            )
            paint.shader = null
            canvas.restore()
        }
        canvas.restore()

        canvas.save()
        canvas.rotate(-rotationDeg * 0.7f, centerX, centerY)
        val secondaryCount = 6
        for (i in 0 until secondaryCount) {
            val angle = i * 360f / secondaryCount + 15f
            canvas.save()
            canvas.rotate(angle, centerX, centerY)
            paint.shader = RadialGradient(
                centerX,
                centerY,
                maxRadius * 0.8f * pulseScale,
                intArrayOf(
                    Color.argb(
                        (secondaryRayAlpha * 255).toInt().coerceIn(0, 255),
                        Color.red(secondaryColor),
                        Color.green(secondaryColor),
                        Color.blue(secondaryColor),
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.alpha = (0.65f * 255).toInt()
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            paint.shader = null
            canvas.restore()
        }
        canvas.restore()

        paint.alpha = 255
        paint.shader = RadialGradient(
            centerX,
            centerY,
            maxRadius * 0.85f,
            intArrayOf(
                Color.argb(
                    (centerVignetteAlpha * 255).toInt().coerceIn(0, 255),
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor),
                ),
                Color.TRANSPARENT,
                Color.argb(
                    (edgeVignetteAlpha * 255).toInt().coerceIn(0, 255),
                    13,
                    0,
                    26,
                ),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        // Soft tip accents so rotations look distinct even when pulse is similar.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        for (i in 0 until 4) {
            val a = (rotationDeg + i * 90f) * (Math.PI.toFloat() / 180f)
            val r = size * 0.28f
            paint.color = Color.argb(
                60,
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor),
            )
            canvas.drawCircle(
                centerX + cos(a) * r,
                centerY + sin(a) * r,
                8f + i * 2f,
                paint,
            )
        }
        paint.style = Paint.Style.FILL
    }
}
