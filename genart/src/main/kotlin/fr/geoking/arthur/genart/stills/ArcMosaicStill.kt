package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Arc Mosaic for Auto/Ambient album art. */
internal object ArcMosaicStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val oval = RectF()

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
        paint.color = 0xFF08070E.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val cols = 7
        val cell = min(w, h) / cols
        val originX = (w - cell * cols) * 0.5f
        val originY = (h - cell * cols) * 0.5f

        for (index in 0 until cols * cols) {
            val col = index % cols
            val row = index / cols
            val cx = originX + (col + 0.5f) * cell
            val cy = originY + (row + 0.5f) * cell
            val rot0 = rnd.nextFloat() * 4f
            val speedMul = 0.15f + rnd.nextFloat() * 0.3f
            val strokeFrac = 0.06f + rnd.nextFloat() * 0.06f
            val rotDeg = (rot0 + loop * speedMul * 4f) * 90f
            val envelope = sin01((loop + rot0 * 0.25f) * speedMul * 2f * PI.toFloat())
            val appear = if (envelope < 0.05f) 0f else envelope
            if (appear <= 0f) continue
            val tint = palette.colorAt(index)
            val strokePulse = 0.85f + 0.3f * sin(loop * 2f * PI.toFloat() * speedMul + rot0)
            val stroke = cell * strokeFrac * strokePulse
            val arcSize = cell * 0.92f
            oval.set(cx - arcSize * 0.5f, cy - arcSize * 0.5f, cx + arcSize * 0.5f, cy + arcSize * 0.5f)

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                (0.06f * dim * appear * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawCircle(cx, cy, cell * 0.35f, paint)

            canvas.save()
            canvas.rotate(rotDeg, cx, cy)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = stroke
            paint.color = Color.argb(
                (0.55f * dim * appear * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawArc(oval, 0f, 90f, false, paint)
            paint.strokeWidth = stroke * 0.85f
            paint.color = Color.argb(
                (0.4f * dim * appear * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawArc(oval, 180f, 90f, false, paint)
            canvas.restore()
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
