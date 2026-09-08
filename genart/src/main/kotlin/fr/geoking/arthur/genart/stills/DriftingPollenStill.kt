package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Drifting Pollen for Auto/Ambient album art. */
internal object DriftingPollenStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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
            0f, 0f, 0f, h,
            intArrayOf(0xFF0E1208.toInt(), 0xFF06080A.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val angle = phase + rotationDeg * PI.toFloat() / 180f
        for (i in 0 until 24) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val wanderAmpX = 0.015f + rnd.nextFloat() * 0.03f
            val wanderAmpY = 0.015f + rnd.nextFloat() * 0.03f
            val wanderFreqX = 0.3f + rnd.nextFloat() * 0.6f
            val wanderFreqY = 0.3f + rnd.nextFloat() * 0.6f
            val wanderPhaseX = rnd.nextFloat() * 2f * PI.toFloat()
            val wanderPhaseY = rnd.nextFloat() * 2f * PI.toFloat()
            val pulseFreq = 0.25f + rnd.nextFloat() * 0.65f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val radius = 1.8f + rnd.nextFloat() * 2.7f
            val x0Wander = (((x0 + wanderAmpX * sin(angle * wanderFreqX + wanderPhaseX)) % 1f) + 1f) % 1f
            val y0Wander = (((y0 + wanderAmpY * cos(angle * wanderFreqY + wanderPhaseY)) % 1f) + 1f) % 1f
            val x = x0Wander * w
            val y = y0Wander * h
            val glowPulse = 0.4f + 0.5f * pulse * (0.5f + 0.5f * sin(angle * pulseFreq + pulsePhase))
            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) + 227) / 2)
            val g = ((Color.green(tint) + 232) / 2)
            val b = ((Color.blue(tint) + 107) / 2)
            val glowR = radius * 3f
            val alpha = (0.5f * glowPulse * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, glowR.coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, glowR, paint)
            paint.shader = null
            paint.color = Color.argb((0.8f * glowPulse * 255).toInt().coerceIn(0, 255), r, g, b)
            canvas.drawCircle(x, y, radius * glowPulse, paint)
        }
        paint.alpha = 255
    }
}
