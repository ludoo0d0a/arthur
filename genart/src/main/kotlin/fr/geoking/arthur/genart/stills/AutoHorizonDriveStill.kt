package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI

/** Bakes one frozen frame of Auto Horizon Drive for Auto/Ambient album art. */
internal object AutoHorizonDriveStill {
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
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A2838.toInt(), 0xFF0E1620.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFFE8C090.toInt(), palette.colorAt(0), 0.35f)
        val horizon = h * (0.42f + 0.018f * (wash - 0.5f))
        val farHill = Path().apply {
            moveTo(0f, horizon + h * 0.015f)
            cubicTo(w * 0.18f, horizon - h * 0.14f, w * 0.28f, horizon - h * 0.09f, w * 0.48f, horizon)
            cubicTo(w * 0.70f, horizon + h * 0.06f, w * 0.82f, horizon - h * 0.13f, w, horizon - h * 0.035f)
            lineTo(w, h * 0.66f)
            lineTo(0f, h * 0.66f)
            close()
        }
        paint.color = withAlpha(0xFF111B27.toInt(), (0.92f * dim * 255).toInt())
        canvas.drawPath(farHill, paint)
        val nearHill = Path().apply {
            moveTo(0f, horizon + h * 0.13f)
            cubicTo(w * 0.16f, horizon + h * 0.04f, w * 0.26f, horizon + h * 0.20f, w * 0.47f, horizon + h * 0.12f)
            cubicTo(w * 0.68f, horizon + h * 0.03f, w * 0.86f, horizon + h * 0.22f, w, horizon + h * 0.10f)
            lineTo(w, h * 0.72f)
            lineTo(0f, h * 0.72f)
            close()
        }
        paint.color = withAlpha(0xFF0B121B.toInt(), (0.94f * dim * 255).toInt())
        canvas.drawPath(nearHill, paint)

        paint.shader = LinearGradient(
            0f, horizon - h * 0.08f, 0f, horizon + h * 0.08f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.18f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.08f, w, horizon + h * 0.08f, paint)
        paint.shader = null

        val road = Path().apply {
            moveTo(w * 0.5f - w * 0.022f, horizon + h * 0.06f)
            lineTo(w * 0.5f + w * 0.022f, horizon + h * 0.06f)
            lineTo(w * 0.94f, h)
            lineTo(w * 0.06f, h)
            close()
        }
        paint.color = withAlpha(0xFF080A0C.toInt(), (0.82f * dim * 255).toInt())
        canvas.drawPath(road, paint)
        paint.color = withAlpha(accent, (0.18f * dim * 255).toInt())
        paint.strokeWidth = w * 0.006f
        canvas.drawLine(w * 0.5f - w * 0.022f, horizon + h * 0.06f, w * 0.10f, h, paint)
        canvas.drawLine(w * 0.5f + w * 0.022f, horizon + h * 0.06f, w * 0.90f, h, paint)
        paint.style = Paint.Style.FILL

        repeat(6) { i ->
            val p = ((i / 6f + t01) % 1f)
            val perspective = p * p
            val y = horizon + h * 0.07f + perspective * h * 0.72f
            val halfWidth = w * (0.002f + perspective * 0.012f)
            val dashHeight = h * (0.008f + perspective * 0.038f)
            val dashX = w * 0.5f - halfWidth
            val dashY = y
            paint.color = withAlpha(0xFFE8C090.toInt(), ((0.18f + 0.34f * perspective) * dim * 255).toInt())
            canvas.drawRoundRect(dashX, dashY, dashX + halfWidth * 2f, dashY + dashHeight, halfWidth, halfWidth, paint)
        }

        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.58f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
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
