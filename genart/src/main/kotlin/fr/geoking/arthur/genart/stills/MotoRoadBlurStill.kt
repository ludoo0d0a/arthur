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
import kotlin.math.sin

/** Bakes one frozen frame of Moto Road Blur for Auto/Ambient album art. */
internal object MotoRoadBlurStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val road = Path()
    private val fairing = Path()

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
        val centerX = w * 0.5f
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 23)
        val dim = 0.85f + 0.15f * pulse
        val horizonY = h * (0.43f + (wash - 0.5f) * 0.035f)
        val roadBottomY = h * 0.93f
        val roadTopHalf = w * 0.065f
        val roadBottomHalf = w * 0.62f
        val accent = mixArgb(0xFF808890.toInt(), palette.colorAt(0), 0.25f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A1C20.toInt(), 0xFF0C0E12.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            centerX, horizonY, minDim * 0.52f,
            intArrayOf(withAlpha(accent, (0.19f * dim * 255).toInt()), withAlpha(accent, (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(centerX, horizonY, minDim * 0.52f, paint)
        paint.shader = null

        road.reset()
        road.moveTo(centerX - roadTopHalf, horizonY)
        road.lineTo(centerX + roadTopHalf, horizonY)
        road.lineTo(centerX + roadBottomHalf, roadBottomY)
        road.lineTo(centerX - roadBottomHalf, roadBottomY)
        road.close()
        paint.color = withAlpha(0xFF161A20.toInt(), (dim * 255).toInt())
        canvas.drawPath(road, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.012f
        paint.color = withAlpha(accent, (0.10f * dim * 255).toInt())
        canvas.drawLine(centerX - roadTopHalf, horizonY, centerX - roadBottomHalf, roadBottomY, paint)
        canvas.drawLine(centerX + roadTopHalf, horizonY, centerX + roadBottomHalf, roadBottomY, paint)
        paint.style = Paint.Style.FILL

        // Center markings flow toward the viewpoint and widen with perspective.
        for (i in 0 until 7) {
            val fraction = (i / 7f + t01) % 1f
            val perspective = fraction * fraction
            val y = horizonY + (roadBottomY - horizonY) * fraction
            val dashH = minDim * (0.004f + 0.045f * perspective)
            val dashW = w * (0.003f + 0.018f * perspective)
            val alpha = (0.22f + 0.38f * perspective) * dim
            paint.shader = LinearGradient(
                centerX - dashW, y, centerX + dashW, y,
                intArrayOf(Color.TRANSPARENT, withAlpha(0xFFB8C4CE.toInt(), (alpha * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(centerX - dashW, y, centerX + dashW, y + dashH, dashH * 0.5f, dashH * 0.5f, paint)
            paint.shader = null
        }

        // Low blurred road-side lights stretch diagonally past the rider.
        for (i in 0 until 9) {
            val lane = i / 9f
            val side = if (i % 2 == 0) -1f else 1f
            val progress = (lane + t01) % 1f
            val depth = progress * progress
            val y = horizonY + (roadBottomY - horizonY) * (0.12f + 0.86f * progress)
            val x = centerX + side * (roadTopHalf * (1f - progress) + roadBottomHalf * progress) * 1.18f
            val length = minDim * (0.025f + 0.24f * depth)
            val sway = sin((t01 + lane) * 2f * PI.toFloat()) * minDim * 0.008f
            val glowRadius = minDim * (0.018f + 0.042f * depth)
            val glowColor = if (i % 3 == 0) 0xFF8695A6.toInt() else 0xFF596A7A.toInt()
            paint.shader = RadialGradient(
                x, y, glowRadius,
                intArrayOf(withAlpha(glowColor, ((0.24f + 0.18f * depth) * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, glowRadius, paint)
            paint.shader = LinearGradient(
                x - length * 0.5f, y + sway, x + length * 0.5f, y - sway,
                intArrayOf(Color.TRANSPARENT, withAlpha(glowColor, ((0.22f + 0.25f * depth) * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
            paint.strokeWidth = minDim * (0.002f + 0.006f * depth)
            paint.strokeCap = Paint.Cap.ROUND
            paint.style = Paint.Style.STROKE
            canvas.drawLine(x - length * 0.5f, y + sway, x + length * 0.5f, y - sway, paint)
            paint.style = Paint.Style.FILL
            paint.shader = null
        }

        fairing.reset()
        fairing.moveTo(0f, h * 0.91f)
        fairing.cubicTo(w * 0.28f, h * 0.85f, w * 0.36f, h * 0.96f, centerX, h * 0.94f)
        fairing.cubicTo(w * 0.64f, h * 0.96f, w * 0.72f, h * 0.85f, w, h * 0.91f)
        fairing.lineTo(w, h)
        fairing.lineTo(0f, h)
        fairing.close()
        paint.shader = LinearGradient(0f, h * 0.9f, 0f, h, intArrayOf(0xFF080A0C.toInt(), 0xFF050608.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(fairing, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
