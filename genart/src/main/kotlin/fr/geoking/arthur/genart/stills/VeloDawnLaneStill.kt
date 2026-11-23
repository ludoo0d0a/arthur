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

/** Bakes one frozen frame of Velo Dawn Lane for Auto/Ambient album art. */
internal object VeloDawnLaneStill {
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
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.4f, octaves = 3, seedOffset = 17)
        val dim = 0.85f + 0.15f * pulse
        val horizonY = h * (0.48f + (wash - 0.5f) * 0.018f)
        val vanishingX = w * (0.5f + (loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 41) - 0.5f) * 0.025f)
        val dawn = 0xFFE8C090.toInt()
        val glow = mixArgb(0xFFFFE8C0.toInt(), palette.colorAt(0), 0.25f)
        val foreground = 0xFF0A0C10.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF3A4858.toInt(), dawn, 0xFF9A7056.toInt()),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            vanishingX, horizonY + h * 0.025f, minDim * 0.48f,
            intArrayOf(withAlpha(glow, (0.30f * dim * 255).toInt()), withAlpha(dawn, (0.16f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.34f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(vanishingX, horizonY + h * 0.025f, minDim * 0.48f, paint)
        paint.shader = null

        val fieldY = horizonY + h * 0.11f
        val leftField = Path().apply {
            moveTo(0f, horizonY + h * 0.045f)
            cubicTo(w * 0.15f, horizonY + h * 0.09f, w * 0.27f, fieldY - h * 0.02f, vanishingX, fieldY)
            lineTo(vanishingX, h)
            lineTo(0f, h)
            close()
        }
        val rightField = Path().apply {
            moveTo(w, horizonY + h * 0.04f)
            cubicTo(w * 0.82f, horizonY + h * 0.1f, w * 0.68f, fieldY - h * 0.015f, vanishingX, fieldY)
            lineTo(vanishingX, h)
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(0f, horizonY, 0f, h, intArrayOf(withAlpha(0xFF746653.toInt(), (0.62f * dim * 255).toInt()), foreground), null, Shader.TileMode.CLAMP)
        canvas.drawPath(leftField, paint)
        canvas.drawPath(rightField, paint)
        paint.shader = null

        val roadTop = horizonY + h * 0.035f
        val roadPath = Path().apply {
            moveTo(vanishingX - w * 0.012f, roadTop)
            lineTo(vanishingX + w * 0.012f, roadTop)
            lineTo(w * 0.83f, h)
            lineTo(w * 0.17f, h)
            close()
        }
        paint.shader = LinearGradient(0f, roadTop, 0f, h, intArrayOf(0xFF292A2D.toInt(), 0xFF080A0C.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(roadPath, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.003f
        paint.color = withAlpha(glow, (0.20f * dim * 255).toInt())
        for (edgeX in floatArrayOf(0.17f, 0.83f)) {
            canvas.drawLine(vanishingX + (edgeX - 0.5f) * w * 0.035f, roadTop, w * edgeX, h, paint)
        }

        paint.style = Paint.Style.FILL
        val dashPhase = (t01 * 0.22f) % 1f
        for (i in 0 until 8) {
            val depth = ((i / 8f + dashPhase) % 1f)
            val y = roadTop + (h - roadTop) * depth * depth
            val perspective = 0.012f + depth * 0.028f
            val dashH = h * (0.008f + depth * 0.022f)
            paint.color = withAlpha(glow, ((0.10f + depth * 0.28f) * dim * 255).toInt())
            canvas.drawRoundRect(
                vanishingX - w * perspective * 0.5f,
                y,
                vanishingX + w * perspective * 0.5f,
                y + dashH,
                minDim * 0.004f,
                minDim * 0.004f,
                paint,
            )
        }

        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = i * 73 + 5)
            val x = (0.12f + i * 0.12f) * w
            val y = horizonY + (0.12f + (i % 3) * 0.08f) * h + sin(t01 * 2f * PI.toFloat() + i) * h * 0.003f
            val r = minDim * (0.0015f + 0.0025f * n)
            paint.shader = RadialGradient(
                x, y, r * 4f,
                intArrayOf(withAlpha(glow, ((0.18f + 0.12f * n) * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 4f, paint)
            paint.shader = null
        }

        paint.shader = LinearGradient(
            0f, h * 0.68f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(foreground, (0.56f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.68f, w, h, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, blue)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
