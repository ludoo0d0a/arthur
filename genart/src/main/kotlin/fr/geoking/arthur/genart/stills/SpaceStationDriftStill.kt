package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of "Space Station Drift" — starfield backdrop plus a drifting station silhouette. */
internal object SpaceStationDriftStill {
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
        StarFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)

        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)

        val t = (generation % 600L).toFloat() / 600f
        val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
        if (edgeFade <= 0.02f) return

        val startX = -0.1f * w
        val startY = 0.7f * h
        val endX = 1.1f * w
        val endY = 0.2f * h
        val x = startX + (endX - startX) * t
        val y = startY + (endY - startY) * t
        val moduleRotationDeg = rotationDeg * 0.05f

        val alpha = (0.6f * edgeFade * 255).toInt().coerceIn(0, 255)
        paint.maskFilter = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(alpha, 0x23, 0x2B, 0x3A)

        val moduleSize = minDim * 0.05f
        canvas.save()
        canvas.rotate(moduleRotationDeg, x, y)
        canvas.drawRect(
            x - moduleSize * 0.8f,
            y - moduleSize * 0.25f,
            x + moduleSize * 0.8f,
            y + moduleSize * 0.25f,
            paint,
        )
        canvas.drawRect(
            x - moduleSize * 0.2f,
            y - moduleSize * 0.7f,
            x + moduleSize * 0.2f,
            y + moduleSize * 0.7f,
            paint,
        )
        canvas.drawOval(
            x - moduleSize * 0.9f,
            y - moduleSize * 0.45f,
            x - moduleSize * 0.4f,
            y + moduleSize * 0.05f,
            paint,
        )
        canvas.restore()
        paint.alpha = 255
    }
}
