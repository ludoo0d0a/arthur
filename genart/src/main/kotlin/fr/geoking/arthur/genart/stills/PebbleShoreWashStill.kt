package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.random.Random

/** Bakes one frozen frame of the Pebble Shore Wash look for Auto album art. */
internal object PebbleShoreWashStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

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
            0f,
            0f,
            0f,
            h,
            0xFF070B0E.toInt(),
            0xFF14181C.toInt(),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val pebbleCount = 20
        val pulseAlpha = 0.85f + pulse * 0.15f
        for (i in 0 until pebbleCount) {
            val cx = (0.02f + rnd.nextFloat() * 0.96f) * w
            val cy = (0.78f + rnd.nextFloat() * 0.21f) * h
            val rx = (0.008f + rnd.nextFloat() * 0.014f) * w
            val ry = rx * (0.5f + rnd.nextFloat() * 0.35f)
            val warmth = rnd.nextFloat()
            val shade = 0.25f + rnd.nextFloat() * 0.3f
            val grey = mixGrey(warmth)
            paint.color = Color.argb(
                (shade * 255f * pulseAlpha).toInt().coerceIn(0, 255),
                Color.red(grey),
                Color.green(grey),
                Color.blue(grey),
            )
            paint.style = Paint.Style.FILL
            canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, paint)
        }

        val washCount = 3
        for (i in 0 until washCount) {
            val phaseOffset = rnd.nextFloat()
            val cyclesPerLoop = 0.55f + rnd.nextFloat() * 0.3f
            val maxHeightFrac = 0.34f + rnd.nextFloat() * 0.24f
            val baseAlpha = 0.16f + rnd.nextFloat() * 0.14f
            val centerFrac = 0.32f + rnd.nextFloat() * 0.36f
            val spanFrac = 0.78f + rnd.nextFloat() * 0.27f
            val curveFrac = -0.06f + rnd.nextFloat() * 0.12f

            val raw = phase * cyclesPerLoop + phaseOffset
            val life = raw - kotlin.math.floor(raw)
            val y = h - life * maxHeightFrac * h
            val alpha = (1f - life) * baseAlpha * pulseAlpha
            val halfSpan = spanFrac * w * 0.5f
            val cx = centerFrac * w
            val startX = cx - halfSpan
            val endX = cx + halfSpan
            val curveY = y - curveFrac * h - life * h * 0.04f

            path.reset()
            path.moveTo(startX, y + h * 0.015f)
            path.quadTo((startX + endX) * 0.5f, curveY, endX, y + h * 0.015f)

            val tint = palette.colorAt(i)
            paint.color = Color.argb(
                (alpha * 255f).toInt().coerceIn(0, 255),
                Color.red(tint),
                Color.green(tint),
                Color.blue(tint),
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (3.2f - life * 1.6f).coerceAtLeast(0.8f)
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
    }

    private fun mixGrey(t: Float): Int {
        val fromR = 0x3A
        val fromG = 0x36
        val fromB = 0x30
        val toR = 0x8A
        val toG = 0x83
        val toB = 0x78
        val r = (fromR + (toR - fromR) * t).toInt().coerceIn(0, 255)
        val g = (fromG + (toG - fromG) * t).toInt().coerceIn(0, 255)
        val b = (fromB + (toB - fromB) * t).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
