package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Rising Bubbles for Auto/Ambient album art. */
internal object BubblesStill {
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF04101C.toInt(), 0xFF02060C.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 28) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val riseSpeed = 0.25f + rnd.nextFloat() * 0.6f
            val radius = 3f + rnd.nextFloat() * 9f
            val wobbleAmp = 0.01f + rnd.nextFloat() * 0.03f
            val wobbleFreq = 0.5f + rnd.nextFloat() * 1.5f
            val wobblePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val raw = loop * riseSpeed + y0
            val life = raw - floor(raw)
            val y = (1f - life) * h
            val wobble = sin(loop * 2f * PI.toFloat() * wobbleFreq + wobblePhase) * wobbleAmp
            val x = (((x0 + wobble) % 1f) + 1f) % 1f * w
            val fade = (life / 0.1f).coerceIn(0f, 1f) * ((1f - life) / 0.2f).coerceIn(0f, 1f)
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 168) / 2
            val g = (Color.green(tint) + 216) / 2
            val b = (Color.blue(tint) + 240) / 2
            val alpha = (0.35f * fade * dim * 255).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((alpha * 0.35f).toInt(), r, g, b)
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            paint.color = Color.argb(alpha, r, g, b)
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((alpha * 0.5f).toInt(), 255, 255, 255)
            canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.22f, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
