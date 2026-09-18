package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of School of Fish for Auto/Ambient album art. */
internal object FishSchoolStill {
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
            intArrayOf(0xFF061525.toInt(), 0xFF020810.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase
        val schoolX = (0.5f + 0.28f * cos(time * 0.45f)) * w
        val schoolY = (0.5f + 0.18f * sin(time * 0.6f)) * h
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 20) {
            val orbitRadius = 0.06f + rnd.nextFloat() * 0.22f
            val angleOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val angularSpeed = 0.6f + rnd.nextFloat() * 0.8f
            val scale = 0.5f + rnd.nextFloat() * 0.7f
            val verticalTilt = 0.35f + rnd.nextFloat() * 0.55f
            val angle = angleOffset + time * angularSpeed + rotationDeg * PI.toFloat() / 180f * 0.1f
            val fx = schoolX + cos(angle) * orbitRadius * minDim
            val fy = schoolY + sin(angle) * orbitRadius * minDim * verticalTilt
            val heading = Math.toDegrees(
                atan2(
                    cos(angle + 0.15f) - cos(angle),
                    -(sin(angle + 0.15f) - sin(angle)),
                ).toDouble(),
            ).toFloat()
            val bodyW = scale * minDim * 0.035f
            val bodyH = bodyW * 0.45f
            val tint = palette.colorAt(i)
            val alpha = ((0.45f + 0.4f * scale) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.save()
            canvas.rotate(heading, fx, fy)
            canvas.drawOval(RectF(fx - bodyW, fy - bodyH * 0.5f, fx + bodyW, fy + bodyH * 0.5f), paint)
            canvas.drawCircle(fx - bodyW * 0.85f, fy, bodyH * 0.35f, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }
}
