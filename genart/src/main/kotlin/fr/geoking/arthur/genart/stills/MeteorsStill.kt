package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Sparse Meteors for Auto/Ambient album art. */
internal object MeteorsStill {
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
            intArrayOf(0xFF060510.toInt(), 0xFF020208.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        for (i in 0 until 28) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h
            val a = (0.08f + rnd.nextFloat() * 0.27f)
            paint.color = Color.argb((a * 255).toInt(), 255, 255, 255)
            canvas.drawCircle(sx, sy, 1.2f, paint)
        }

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.6f + 0.4f * pulse
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE

        for (i in 0 until 4) {
            val startX = rnd.nextFloat()
            val startY = rnd.nextFloat() * 0.45f
            val lengthFrac = 0.12f + rnd.nextFloat() * 0.16f
            val angle = 0.35f + rnd.nextFloat() * 0.5f
            val phaseOffset = rnd.nextFloat()
            val duration = 0.12f + rnd.nextFloat() * 0.16f
            val thickness = 1.2f + rnd.nextFloat() * 1.6f
            val local = ((loop + phaseOffset) % 1f + 1f) % 1f
            if (local > duration) continue
            val life = local / duration
            val travel = life * lengthFrac * 2.2f
            val x = (startX + cos(angle) * travel) * w
            val y = (startY + sin(angle) * travel) * h
            val trail = lengthFrac * minOf(w, h)
            val sx = x - cos(angle) * trail
            val sy = y - sin(angle) * trail
            val fade = (1f - life).coerceIn(0f, 1f) * (life / 0.15f).coerceIn(0f, 1f)
            val tint = palette.colorAt(i)
            paint.strokeWidth = thickness
            paint.shader = LinearGradient(
                sx, sy, x, y,
                Color.TRANSPARENT,
                Color.argb(
                    (0.55f * fade * dim * 255).toInt().coerceIn(0, 255),
                    (Color.red(tint) + 255) / 2,
                    (Color.green(tint) + 255) / 2,
                    (Color.blue(tint) + 255) / 2,
                ),
                Shader.TileMode.CLAMP,
            )
            canvas.drawLine(sx, sy, x, y, paint)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                (0.7f * fade * dim * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 255) / 2,
                (Color.green(tint) + 255) / 2,
                (Color.blue(tint) + 255) / 2,
            )
            canvas.drawCircle(x, y, thickness * 1.2f, paint)
            paint.style = Paint.Style.STROKE
        }
        paint.shader = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
