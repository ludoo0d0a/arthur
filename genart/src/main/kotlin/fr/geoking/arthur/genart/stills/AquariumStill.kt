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
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Aquarium for Auto/Ambient album art. */
internal object AquariumStill {
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
            intArrayOf(0xFF1E5A66.toInt(), 0xFF072A33.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase
        val dim = 0.6f + 0.4f * pulse
        val minDim = minOf(w, h)
        val tankX = 0.5f * w
        val tankY = 0.48f * h

        for (i in 0 until 3) {
            val orbitRadius = 0.05f + rnd.nextFloat() * 0.13f
            val angleOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val angularSpeed = 0.4f + rnd.nextFloat() * 0.5f
            val scale = 0.6f + rnd.nextFloat() * 0.5f
            val verticalTilt = 0.35f + rnd.nextFloat() * 0.4f
            val angle = angleOffset + time * angularSpeed + rotationDeg * PI.toFloat() / 180f * 0.1f
            val fx = tankX + cos(angle) * orbitRadius * minDim
            val fy = tankY + sin(angle) * orbitRadius * minDim * verticalTilt
            val heading = Math.toDegrees(
                atan2(
                    cos(angle + 0.15f) - cos(angle),
                    -(sin(angle + 0.15f) - sin(angle)),
                ).toDouble(),
            ).toFloat()
            val bodyW = scale * minDim * 0.03f
            val bodyH = bodyW * 0.5f
            val tint = palette.colorAt(i)
            val alpha = ((0.5f + 0.35f * scale) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.save()
            canvas.rotate(heading, fx, fy)
            canvas.drawOval(RectF(fx - bodyW, fy - bodyH * 0.5f, fx + bodyW, fy + bodyH * 0.5f), paint)
            canvas.drawCircle(fx - bodyW * 0.85f, fy, bodyH * 0.35f, paint)
            canvas.restore()
        }

        for (i in 0 until 10) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val riseSpeed = 0.2f + rnd.nextFloat() * 0.4f
            val radius = 2f + rnd.nextFloat() * 4f
            val wobbleAmp = 0.008f + rnd.nextFloat() * 0.017f
            val wobbleFreq = 0.4f + rnd.nextFloat() * 1f
            val wobblePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val raw = time * riseSpeed + y0
            val life = raw - floor(raw)
            val y = (1f - life) * h
            val wobble = sin(time * 2f * PI.toFloat() * wobbleFreq + wobblePhase) * wobbleAmp
            val xRaw = x0 + wobble
            val x = (xRaw - floor(xRaw)) * w
            val fade = (life / 0.1f).coerceIn(0f, 1f) * ((1f - life) / 0.2f).coerceIn(0f, 1f)
            val alpha = (0.4f * fade * dim * 255).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((alpha * 0.3f).toInt(), 0xCF, 0xF3, 0xFF)
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.argb(alpha, 0xCF, 0xF3, 0xFF)
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.FILL
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
