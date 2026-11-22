package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object PhysarumVeinsStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        paint.style = Paint.Style.FILL
        paint.color = 0xFF05080A.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        val accent = palette.colorAt(0)
        paint.strokeWidth = minOf(w, h) * 0.004f
        for (i in 0 until 14) {
            val n = loopedFbm(t01, radius = 1.3f, seedOffset = i * 17)
            val x = ((0.1f + i * 0.06f) + 0.04f * (n * 2f - 1f)) * w
            val y = ((0.15f + (i % 5) * 0.15f) + 0.03f * sin(t01 * 2f * PI.toFloat() + i)) * h
            val x2 = ((0.2f + ((i + 3) % 14) * 0.06f)) * w
            val y2 = ((0.2f + ((i + 2) % 5) * 0.15f) + 0.03f * cos(t01 * 2f * PI.toFloat() + i)) * h
            paint.color = Color.argb((0.25f * pulse * 255).toInt().coerceIn(20, 180), Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawLine(x, y, x2, y2, paint)
            canvas.drawCircle(x, y, minOf(w, h) * 0.012f, paint)
        }
    }
}
