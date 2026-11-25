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
import kotlin.math.PI
import kotlin.math.exp

internal object StormStrobeFlashStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = size.toFloat()
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val cloudMotion = loopedFbm(t01, radius = 1.4f, octaves = 2, seedOffset = 7)
        val flashPhase = (t01 + 0.37f) % 1f
        val flash = exp(-((flashPhase - 0.13f) * (flashPhase - 0.13f)) / 0.0016f)
        val flashAmt = (flash * (0.7f + 0.3f * pulse)).coerceIn(0f, 1f)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080C16.toInt(), 0xFF111A28.toInt(), 0xFF202938.toInt()),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // A broad, soft sheet-lightning glow sits behind the silhouetted cloud bank.
        val glowX = w * (0.49f + 0.06f * (cloudMotion - 0.5f))
        val glowY = h * 0.61f
        val glowRadius = minDim * (0.74f + 0.04f * flashAmt)
        paint.shader = RadialGradient(
            glowX, glowY, glowRadius,
            intArrayOf(
                Color.argb((0.55f * flashAmt * 255).toInt().coerceIn(0, 255), 232, 240, 255),
                Color.argb((0.28f * flashAmt * 255).toInt().coerceIn(0, 255), 184, 200, 224),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)
        paint.shader = null

        // Keep the foreground clouds soft and dark so the flash remains a brief background event.
        repeat(6) { index ->
            val xRaw = (index + 0.35f) / 6f + (cloudMotion - 0.5f) * 0.22f + t01 * 0.35f
            val x = ((xRaw % 1f) + 1f) % 1f
            val y = 0.48f + (index % 3) * 0.07f
            val rx = minDim * (0.26f + (index % 3) * 0.05f)
            val ry = minDim * (0.14f + (index % 2) * 0.04f)
            val centerX = x * w
            val centerY = y * h
            oval.set(centerX - rx, centerY - ry, centerX + rx, centerY + ry)
            paint.shader = RadialGradient(
                centerX, centerY, rx,
                intArrayOf(0xF0111925.toInt(), 0xC7111925.toInt(), Color.TRANSPARENT),
                floatArrayOf(0f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(oval, paint)
        }
        paint.shader = null

        if (flashAmt > 0.015f) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((0.32f * flashAmt * 255).toInt().coerceIn(0, 255), 220, 232, 250),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }
    }
}
