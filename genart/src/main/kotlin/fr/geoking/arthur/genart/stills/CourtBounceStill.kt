package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Bakes one frozen frame of Court Bounce for Auto/Ambient album art. */
internal object CourtBounceStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        val dim = 0.65f + 0.35f * pulse
        val cycle = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(3, 16, 8)
        canvas.drawRect(0f, 0f, w, h, paint)

        val tint = palette.colorAt(0)
        val gr = ((0x33 + Color.red(tint)) / 2)
        val gg = ((0xFF + Color.green(tint)) / 2)
        val gb = ((0x66 + Color.blue(tint)) / 2)

        val margin = minOf(w, h) * 0.06f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb((90 * dim).toInt(), gr, gg, gb)
        canvas.drawRect(margin, margin, w - margin, h - margin, paint)

        paint.style = Paint.Style.FILL
        val dashCount = 9
        val dashH = (h - margin * 2f) / (dashCount * 2f)
        paint.color = Color.argb((70 * dim).toInt(), gr, gg, gb)
        for (i in 0 until dashCount) {
            val y0 = margin + i * dashH * 2f + dashH * 0.25f
            canvas.drawRect(w * 0.5f - 1.5f, y0, w * 0.5f + 1.5f, y0 + dashH, paint)
        }

        val leftY = h * 0.5f + sin(cycle * 2f * PI.toFloat()) * h * 0.22f
        val rightY = h * 0.5f + sin(cycle * 2f * PI.toFloat() + 1.7f) * h * 0.2f
        val paddleW = w * 0.018f
        val paddleH = h * 0.14f
        paint.color = Color.argb((220 * dim).toInt(), gr, gg, gb)
        canvas.drawRect(margin * 1.6f, leftY - paddleH * 0.5f, margin * 1.6f + paddleW, leftY + paddleH * 0.5f, paint)
        canvas.drawRect(w - margin * 1.6f - paddleW, rightY - paddleH * 0.5f, w - margin * 1.6f, rightY + paddleH * 0.5f, paint)

        val triX = abs(((sin(cycle * 2f * PI.toFloat()) + 1.0) / 2.0).toFloat() * 2f - 1f)
        val triY = abs(((sin(cycle * 3.2f * PI.toFloat() + 0.4f) + 1.0) / 2.0).toFloat() * 2f - 1f)
        val bx = margin * 2.2f + triX * (w - margin * 4.4f)
        val by = margin * 2f + triY * (h - margin * 4f)
        val ballR = minOf(w, h) * 0.014f
        paint.color = Color.argb((50 * dim).toInt(), gr, gg, gb)
        canvas.drawCircle(bx, by, ballR * 2.4f, paint)
        paint.color = Color.argb((240 * dim).toInt(), gr, gg, gb)
        canvas.drawCircle(bx, by, ballR, paint)

        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        val scanStep = (h / 48f).coerceAtLeast(3f)
        var sy = 0f
        while (sy < h) {
            paint.color = Color.argb((10 * dim).toInt(), gr, gg, gb)
            canvas.drawLine(0f, sy, w, sy, paint)
            sy += scanStep
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
