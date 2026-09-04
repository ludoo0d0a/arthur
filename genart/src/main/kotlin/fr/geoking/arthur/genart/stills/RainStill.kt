package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Rain for Auto/Ambient album art. */
internal object RainStill {
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
            intArrayOf(0xFF0A1220.toInt(), 0xFF05080F.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val dim = 0.7f + 0.3f * pulse
        for (i in 0 until 60) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val fallSpeed = 0.55f + rnd.nextFloat() * 0.8f
            val lengthFrac = 0.018f + rnd.nextFloat() * 0.037f
            val thickness = 1f + rnd.nextFloat() * 1.4f
            val driftAmp = 0.004f + rnd.nextFloat() * 0.016f
            val driftFreq = 0.3f + rnd.nextFloat() * 1.1f
            val alphaBase = 0.18f + rnd.nextFloat() * 0.37f
            val fall = ((y0 + fallSpeed * (phase / (2f * PI.toFloat()))) % 1f + 1f) % 1f
            val y = fall * h
            val sway = sin(drift * driftFreq) * driftAmp
            val x = (((x0 + sway) % 1f) + 1f) % 1f * w
            val len = lengthFrac * h
            val tint = palette.colorAt(i)
            paint.strokeWidth = thickness
            paint.color = Color.argb(
                (alphaBase * dim * 255).toInt().coerceIn(0, 255),
                ((Color.red(tint) + 184) / 2),
                ((Color.green(tint) + 212) / 2),
                ((Color.blue(tint) + 240) / 2),
            )
            canvas.drawLine(x, y, x + 0.12f * len, y + len, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
