package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Fireflies for Auto/Ambient album art. */
internal object FirefliesStill {
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
            intArrayOf(0xFF05080E.toInt(), 0xFF02040A.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase / (2f * PI.toFloat())
        val drift = phase + rotationDeg * PI.toFloat() / 180f
        for (i in 0 until 24) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val driftX = 0.03f + rnd.nextFloat() * 0.09f
            val driftY = 0.02f + rnd.nextFloat() * 0.06f
            val pulseFreq = 0.4f + rnd.nextFloat() * 1.2f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val radius = 2.5f + rnd.nextFloat() * 4f
            val x = (((x0 + time * driftX) % 1f) + 1f) % 1f * w
            val y = (((y0 + time * driftY + 0.02f * sin(drift + pulsePhase)) % 1f) + 1f) % 1f * h
            val glowPulse = 0.35f + 0.55f * pulse * (0.5f + 0.5f * sin(drift * pulseFreq + pulsePhase))
            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) + 245) / 2)
            val g = ((Color.green(tint) + 230) / 2)
            val b = ((Color.blue(tint) + 160) / 2)
            val glowR = radius * 3.5f
            val alpha = (0.55f * glowPulse * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, glowR.coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, glowR, paint)
            paint.shader = null
            paint.color = Color.argb((0.85f * glowPulse * 255).toInt().coerceIn(0, 255), r, g, b)
            canvas.drawCircle(x, y, radius * glowPulse, paint)
        }
        paint.alpha = 255
    }
}
