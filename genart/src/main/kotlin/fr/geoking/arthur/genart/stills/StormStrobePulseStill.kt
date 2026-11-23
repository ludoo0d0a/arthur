package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.exp

internal object StormStrobePulseStill {
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

        // Low layered storm clouds leave open, dark sky above the horizon.
        val cloudY = h * (0.48f + 0.025f * loopedFbm(t01, radius = 1.2f, seedOffset = 17))
        paint.shader = LinearGradient(
            0f, cloudY - h * 0.10f, 0f, cloudY + h * 0.25f,
            intArrayOf(0x30243143, 0xC0182232.toInt(), 0xF0080C14.toInt()),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(-w * 0.08f, cloudY - h * 0.10f, w * 1.08f, cloudY + h * 0.28f, paint)
        paint.shader = null
        for (i in 0..3) {
            val x = w * (i / 3f) + (loopedFbm(t01, radius = 1.3f, seedOffset = 31 + i * 13) - 0.5f) * w * 0.12f
            val y = cloudY + h * (0.015f + (i % 2) * 0.04f)
            val radius = w * (0.14f + 0.025f * loopedFbm(t01, seedOffset = 71 + i * 5))
            paint.shader = RadialGradient(
                x, y, radius,
                intArrayOf(0x662A3546, 0x002A3546),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(x - radius, y - h * 0.10f, x + radius, y + h * 0.15f, paint)
        }
        paint.shader = null

        // Infrequent diffuse pulse behind the clouds, never a full-frame strobe.
        val flashEnvelope = exp(-((t01 - 0.12f) * (t01 - 0.12f)) / 0.0018f) +
            0.48f * exp(-((t01 - 0.62f) * (t01 - 0.62f)) / 0.0012f)
        val flash = flashEnvelope * (0.65f + 0.35f * pulse.coerceIn(0f, 1f))
        if (flash > 0.05f) {
            val glowX = w * (0.30f + 0.40f * loopedFbm(t01, radius = 1.2f, seedOffset = 91))
            val glowY = cloudY + h * 0.03f
            val radius = minOf(w, h) * 0.42f
            paint.shader = RadialGradient(
                glowX, glowY, radius,
                intArrayOf(
                    Color.argb((0.30f * flash * 255).toInt().coerceIn(0, 255), 220, 230, 245),
                    Color.argb((0.13f * flash * 255).toInt().coerceIn(0, 255), 173, 192, 218),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(glowX - radius, glowY - radius * 0.45f, glowX + radius, glowY + radius * 0.45f, paint)
            paint.shader = null
        }

        // A faint cloud-base catch makes the pulse read as distant sky light.
        val baseY = cloudY + h * 0.13f
        val catchRadius = w * 0.46f
        paint.shader = RadialGradient(
            w * 0.5f, baseY, catchRadius,
            intArrayOf(
                Color.argb((0.16f * flash * 255).toInt().coerceIn(0, 255), 195, 207, 226),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(w * 0.04f, baseY - h * 0.12f, w * 0.96f, baseY + h * 0.14f, paint)
        paint.shader = null

        val horizon = Path().apply {
            moveTo(0f, h * 0.78f)
            lineTo(w * 0.16f, h * 0.73f)
            lineTo(w * 0.33f, h * 0.77f)
            lineTo(w * 0.51f, h * 0.71f)
            lineTo(w * 0.71f, h * 0.76f)
            lineTo(w, h * 0.72f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        paint.color = 0xFF080C14.toInt()
        canvas.drawPath(horizon, paint)
    }
}
