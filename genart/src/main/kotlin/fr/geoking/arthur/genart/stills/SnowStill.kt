package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the "Falling Snow" engine for Auto/Ambient album art. */
internal object SnowStill {
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

        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.45f,
            size * 0.85f,
            intArrayOf(0xFF0B1220.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val loopPhase = ((phase % 1f) + 1f) % 1f
        val flakeCount = 90
        for (i in 0 until flakeCount) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val fallSpeed = 0.4f + rnd.nextFloat() * 0.8f
            val radius = size * (0.0025f + rnd.nextFloat() * 0.011f)
            val swayAmp = 0.01f + rnd.nextFloat() * 0.04f
            val swayFreq = 0.4f + rnd.nextFloat() * 1.8f
            val swayPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.35f + rnd.nextFloat() * 0.65f

            val fallPhase = ((y0 + fallSpeed * loopPhase) % 1f + 1f) % 1f
            val y = fallPhase * h
            val swayTime = loopPhase * 2f * PI.toFloat() * swayFreq + swayPhase
            val x = (((x0 + sin(swayTime) * swayAmp) % 1f + 1f) % 1f) * w

            val depth = ((radius / size - 0.0025f) / 0.011f).coerceIn(0f, 1f)
            val tinted = i % 5 == 0
            val tintColor = if (tinted) palette.colorAt(i) else Color.WHITE
            val r = (255 + (Color.red(tintColor) - 255) * 0.25f).toInt().coerceIn(0, 255)
            val g = (255 + (Color.green(tintColor) - 255) * 0.25f).toInt().coerceIn(0, 255)
            val b = (255 + (Color.blue(tintColor) - 255) * 0.25f).toInt().coerceIn(0, 255)
            val alpha = (alphaBase * (0.5f + 0.5f * depth)).coerceIn(0f, 1f)

            if (depth > 0.55f) {
                val glowRadius = radius * (2.4f + pulse * 0.6f)
                paint.shader = RadialGradient(
                    x,
                    y,
                    glowRadius.coerceAtLeast(1f),
                    Color.argb((alpha * 0.22f * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x, y, glowRadius, paint)
                paint.shader = null
            }

            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.alpha = 255
    }
}
