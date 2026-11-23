package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

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
        val cloudNoise = loopedFbm(t01, radius = 1.4f, octaves = 3, seedOffset = 7)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080C15.toInt(), 0xFF101827.toInt(), 0xFF1A2230.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Brief diffuse sheet lightning behind the clouds, rather than a full-frame strobe.
        val flashPhase = (t01 + 0.37f) % 1f
        val flash1 = exp(-((flashPhase - 0.12f) * (flashPhase - 0.12f)) / 0.0018f)
        val flash2 = exp(-((flashPhase - 0.62f) * (flashPhase - 0.62f)) / 0.0012f) * 0.45f
        val flash = (flash1 + flash2).coerceIn(0f, 1f) * (0.7f + 0.3f * pulse)
        val bandY = h * (0.36f + 0.035f * (cloudNoise - 0.5f))
        if (flash > 0.025f) {
            paint.shader = LinearGradient(
                0f, bandY - h * 0.26f, 0f, bandY + h * 0.26f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((0.04f * flash * 255).toInt().coerceIn(0, 255), 220, 232, 255),
                    Color.argb((0.22f * flash * 255).toInt().coerceIn(0, 255), 232, 240, 255),
                    Color.argb((0.07f * flash * 255).toInt().coerceIn(0, 255), 211, 226, 250),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.30f, 0.50f, 0.70f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }

        val cloudCount = 4
        val rect = RectF()
        repeat(cloudCount) { i ->
            val x0 = seededRange(i * 19 + 5, -0.12f, 0.86f)
            val y0 = seededRange(i * 31 + 9, 0.26f, 0.70f)
            val width = seededRange(i * 43 + 13, 0.40f, 0.70f)
            val height = seededRange(i * 53 + 17, 0.12f, 0.22f)
            val drift = (loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = i * 37 + 19) - 0.5f) * w * 0.055f
            val bob = sin(t01 * 2f * PI.toFloat() + i * 1.7f) * h * 0.008f
            val cx = x0 * w + drift
            val cy = y0 * h + bob
            val alpha = (0.43f + flash * 0.14f).coerceIn(0f, 0.72f)
            paint.shader = RadialGradient(
                cx + width * w * 0.5f,
                cy + height * h * 0.55f,
                width * w * 0.58f,
                intArrayOf(
                    Color.argb((alpha * 255).toInt(), 26, 34, 48),
                    Color.argb((alpha * 0.82f * 255).toInt(), 16, 24, 39),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
            rect.set(cx, cy, cx + width * w, cy + height * h)
            canvas.drawOval(rect, paint)
            paint.shader = null

            if (flash > 0.04f) {
                paint.color = Color.argb((0.12f * flash * 255).toInt().coerceIn(0, 255), 232, 240, 255)
                paint.strokeWidth = minOf(w, h) * 0.005f
                canvas.drawLine(cx + width * w * 0.18f, cy + height * h * 0.16f, cx + width * w * 0.82f, cy + height * h * 0.16f, paint)
            }
        }
        paint.color = Color.WHITE
        paint.shader = null
    }
}
