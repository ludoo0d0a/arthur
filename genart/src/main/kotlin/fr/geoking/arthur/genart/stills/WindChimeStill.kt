package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Wind Chime Silhouette" for Auto/Ambient album art. */
internal object WindChimeStill {
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
            intArrayOf(0xFF3A2E28.toInt(), 0xFF211A16.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val railY = h * 0.12f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 2f
        paint.color = Color.argb((0.35f * 255).toInt(), 216, 195, 165)
        canvas.drawLine(w * 0.08f, railY, w * 0.92f, railY, paint)

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val gust = 0.25f + 0.75f * ((sin(drift.toDouble()).toFloat() + 1f) / 2f)

        val count = 3
        for (i in 0 until count) {
            val xFrac = (i + 0.5f) / count + (rnd.nextFloat() - 0.5f) * 0.08f
            val lengthFrac = 0.22f + rnd.nextFloat() * 0.20f
            val widthFrac = 0.045f + rnd.nextFloat() * 0.025f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()

            val localSway = sin((drift * 0.5f + phaseOffset).toDouble()).toFloat()
            val sway = localSway * gust * 0.16f
            val anchorX = xFrac * w
            val length = lengthFrac * h
            val bottomX = anchorX + sway * length
            val bottomY = railY + length

            paint.strokeWidth = 1.5f
            paint.color = Color.argb((0.5f * (0.7f + 0.3f * pulse) * 255).toInt().coerceIn(0, 255), 201, 183, 154)
            canvas.drawLine(anchorX, railY, bottomX, bottomY, paint)

            val tint = palette.colorAt(i)
            val bodyR = ((Color.red(tint) + 138) / 2)
            val bodyG = ((Color.green(tint) + 110) / 2)
            val bodyB = ((Color.blue(tint) + 82) / 2)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((0.85f * (0.7f + 0.3f * pulse) * 255).toInt().coerceIn(0, 255), bodyR, bodyG, bodyB)
            val width = widthFrac * w
            val bodyHeight = length * 0.32f
            val rect = RectF(
                bottomX - width / 2f,
                bottomY - bodyHeight / 2f,
                bottomX + width / 2f,
                bottomY + bodyHeight / 2f,
            )
            canvas.drawRoundRect(rect, width * 0.4f, width * 0.4f, paint)
            paint.style = Paint.Style.STROKE
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
