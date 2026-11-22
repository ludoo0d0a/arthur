package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.exp

internal object StormStrobeSheetStill {
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0E18.toInt(), 0xFF1A2230.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val flash = exp(-((t01 - 0.12f) * (t01 - 0.12f)) / 0.0018f) * (0.7f + 0.3f * pulse)
        if (flash > 0.05f) {
            paint.color = Color.argb((0.45f * flash * 255).toInt().coerceIn(0, 255), 232, 240, 255)
            canvas.drawRect(0f, 0f, w, h, paint)
        }
        paint.strokeWidth = minOf(w, h) * 0.008f
        paint.color = Color.argb((0.7f * flash * 255).toInt().coerceIn(40, 255), 220, 230, 255)
        val x = w * (0.35f + 0.3f * loopedFbm(t01, seedOffset = 9))
        canvas.drawLine(x, h * 0.1f, x + w * 0.04f, h * 0.7f, paint)
    }
}
