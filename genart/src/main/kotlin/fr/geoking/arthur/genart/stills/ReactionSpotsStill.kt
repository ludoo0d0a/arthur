package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

internal object ReactionSpotsStill {
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
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        for (i in 0 until 8) {
            val n = loopedFbm(t01, radius = 1.25f, seedOffset = i * 23)
            val accent = palette.colorAt(i)
            val x = ((0.15f + i * 0.1f) + 0.05f * (n * 2f - 1f)) * w
            val y = ((0.2f + (i % 4) * 0.18f) + 0.04f * sin(t01 * 2f * PI.toFloat() + i)) * h
            val r = minOf(w, h) * (0.06f + 0.08f * n) * (0.85f + 0.15f * pulse)
            paint.color = Color.argb((0.35f * 255).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(x, y, r, paint)
        }
    }
}
