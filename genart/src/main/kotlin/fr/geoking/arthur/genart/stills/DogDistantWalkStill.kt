package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Distant Dog Walk for Auto/Ambient album art. */
internal object DogDistantWalkStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
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
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = 0xFF1A2438.toInt()
        canvas.drawRect(0f, 0f, w, h * 0.5f, paint)
        paint.color = 0xFF3A2E3A.toInt()
        canvas.drawRect(0f, h * 0.35f, w, h * 0.58f, paint)
        paint.color = 0xFF1C1612.toInt()
        val horizonY = h * 0.58f
        canvas.drawRect(0f, horizonY, w, h, paint)

        val t = ((phase % (2f * Math.PI.toFloat())) / (2f * Math.PI.toFloat()) + 1f) % 1f
        val walkCycle = if (t < 0.5f) t * 2f else (1f - t) * 2f
        val dogX = w * (0.18f + 0.64f * walkCycle)
        val dogY = horizonY - minDim * 0.02f
        val scale = minDim * 0.035f * (1f + 0.02f * pulse)
        paint.color = mixArgb(0xFF0E0C0A.toInt(), palette.colorAt(0), 0.15f)

        rect.set(dogX - scale * 1.4f, dogY - scale * 1.1f, dogX + scale * 1.4f, dogY + scale * 0.4f)
        canvas.drawOval(rect, paint)
        canvas.drawCircle(dogX + scale * 1.5f, dogY - scale * 0.9f, scale * 0.7f, paint)

        paint.strokeWidth = scale * 0.35f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val legPhase = sin(phase * 4f) * scale * 0.25f
        path.reset()
        path.moveTo(dogX - scale * 0.8f, dogY)
        path.lineTo(dogX - scale * 0.9f, dogY + scale * 1.2f + legPhase)
        path.moveTo(dogX + scale * 0.2f, dogY)
        path.lineTo(dogX + scale * 0.35f, dogY + scale * 1.2f - legPhase)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
