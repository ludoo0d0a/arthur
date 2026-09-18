package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the Bird Flock look for Auto album art. */
internal object BirdFlockStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

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
        val minDim = size.toFloat()

        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.4f,
            size * 0.85f,
            intArrayOf(0xFF0B1023.toInt(), 0xFF03040A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        for (i in 0 until 24) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h * 0.7f
            val sAlpha = 0.06f + rnd.nextFloat() * 0.16f
            paint.color = Color.argb((sAlpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(sx, sy, 0.6f + rnd.nextFloat() * 1f, paint)
        }

        val flockCenterX = (0.5f + 0.3f * cos(phase * 0.5f)) * w
        val flockCenterY = (0.35f + 0.15f * sin(phase * 0.7f)) * h

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 18) {
            val orbitRadius = 0.05f + rnd.nextFloat() * 0.17f
            val angleOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val angularSpeedJitter = 0.75f + rnd.nextFloat() * 0.6f
            val flapPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val flapFrequency = 3.2f + rnd.nextFloat() * 2.3f
            val birdScale = 0.55f + rnd.nextFloat() * 0.6f
            val verticalTilt = 0.4f + rnd.nextFloat() * 0.6f

            val angle = angleOffset + phase * angularSpeedJitter
            val bx = flockCenterX + cos(angle) * orbitRadius * minDim
            val by = flockCenterY + sin(angle) * orbitRadius * minDim * verticalTilt

            val halfAngle = 0.9f + 0.5f * sin(phase * flapFrequency + flapPhase)
            val wingLength = birdScale * minDim * 0.05f
            val leftX = bx - sin(halfAngle) * wingLength
            val leftY = by + cos(halfAngle) * wingLength * verticalTilt
            val rightX = bx + sin(halfAngle) * wingLength
            val rightY = by + cos(halfAngle) * wingLength * verticalTilt

            val depthFactor = birdScale.coerceIn(0.4f, 1.2f)
            val alpha = (0.4f + 0.5f * depthFactor).coerceIn(0f, 1f)
            val c = palette.colorAt(i)
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            paint.strokeWidth = 1.2f + 1.4f * depthFactor

            canvas.drawLine(leftX, leftY, bx, by, paint)
            canvas.drawLine(bx, by, rightX, rightY, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
