package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

private val RainbowSpectrum = intArrayOf(
    0xFFC9A6FF.toInt(),
    0xFFA6B8FF.toInt(),
    0xFF9BD3FF.toInt(),
    0xFFA6F0C6.toInt(),
    0xFFFFF3A6.toInt(),
    0xFFFFCBA0.toInt(),
    0xFFFFA6A6.toInt(),
)

/** Bakes one frozen frame of "Soft Rainbow" for Auto/Ambient album art. */
internal object RainbowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

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

        val skyTop = palette.primary
        val skyMid = palette.secondary
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
                Color.argb(
                    255,
                    (Color.red(skyTop) * 0.4f).toInt().coerceIn(0, 255),
                    (Color.green(skyTop) * 0.4f).toInt().coerceIn(0, 255),
                    (Color.blue(skyTop) * 0.4f).toInt().coerceIn(0, 255),
                ),
                Color.argb(
                    255,
                    (Color.red(skyMid) * 0.24f).toInt().coerceIn(0, 255),
                    (Color.green(skyMid) * 0.24f).toInt().coerceIn(0, 255),
                    (Color.blue(skyMid) * 0.24f).toInt().coerceIn(0, 255),
                ),
                0xFF0A1018.toInt(),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val loop = ((phase / (2f * PI.toFloat()) + rotationDeg / 360f) % 1f + 1f) % 1f
        val angle = loop * 2f * PI.toFloat()
        val breathe = 0.55f + 0.45f * sin01(angle)
        val dim = 0.65f + 0.35f * pulse
        val centerX = w * (0.5f + 0.04f * sin(angle * 0.6f))
        val centerY = h * 1.35f
        val bandGap = h * 0.045f
        val baseRadius = h * (1.20f + 0.08f * breathe)
        val bandWidth = bandGap * 0.85f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = bandWidth

        for (i in RainbowSpectrum.indices) {
            val radius = baseRadius + i * bandGap
            val base = RainbowSpectrum[i]
            val bandShimmer = 0.75f + 0.25f * sin01(angle * 1.2f + i * 0.55f)
            val alpha = (0.14f * dim * breathe * bandShimmer * 255f).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
            rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            canvas.drawArc(rect, 180f, 180f, false, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
