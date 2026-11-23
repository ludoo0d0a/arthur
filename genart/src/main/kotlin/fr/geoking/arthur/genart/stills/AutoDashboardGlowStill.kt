package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Auto Dashboard Glow for Auto/Ambient album art. */
internal object AutoDashboardGlowStill {
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
            intArrayOf(0xFF0A0E12.toInt(), 0xFF141A22.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFF40E0A0.toInt(), palette.colorAt(0), 0.35f)
        val horizon = h * (0.55f + 0.04f * (wash - 0.5f))
        paint.shader = LinearGradient(
            0f, horizon - h * 0.08f, 0f, horizon + h * 0.08f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.18f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.08f, w, horizon + h * 0.08f, paint)
        paint.shader = null
        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.2f + i * 0.04f, seedOffset = i * 97 + 5)
            val x = ((0.1f + i * 0.12f + t01 * 0.15f) % 1f) * w
            val y = ((0.2f + (i % 4) * 0.15f) + 0.03f * sin(t01 * 2f * PI.toFloat() + i)) * h
            paint.color = withAlpha(mixArgb(palette.colorAt(i), accent, 0.4f), ((0.10f + 0.16f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, minOf(w, h) * (0.03f + 0.05f * n), paint)
        }
        val minDim = minOf(w, h)
        val dashboardTop = h * 0.70f
        paint.shader = LinearGradient(
            0f, dashboardTop, 0f, h,
            intArrayOf(withAlpha(0xFF0A0C10.toInt(), (0.82f * dim * 255).toInt()), withAlpha(0xFF050609.toInt(), (0.98f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, dashboardTop, w, h, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.008f
        paint.color = withAlpha(0xFF29343A.toInt(), (0.48f * dim * 255).toInt())
        canvas.drawLine(0f, dashboardTop, w, dashboardTop, paint)
        paint.style = Paint.Style.FILL

        val cyan = accent
        val amber = mixArgb(0xFFE6A45A.toInt(), palette.colorAt(1), 0.22f)
        // Instrument colors softly spill onto the lower windscreen.
        paint.shader = RadialGradient(
            w * 0.30f, h * 0.70f, minDim * 0.30f,
            intArrayOf(withAlpha(cyan, (0.22f * dim * 255).toInt()), withAlpha(cyan, (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.30f, h * 0.70f, minDim * 0.30f, paint)
        paint.shader = RadialGradient(
            w * 0.72f, h * 0.72f, minDim * 0.24f,
            intArrayOf(withAlpha(amber, (0.17f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.72f, h * 0.72f, minDim * 0.24f, paint)
        paint.shader = null

        // Recessed circular gauges with fine tick marks and subtly shifting needles.
        val gaugeY = h * 0.81f
        val gaugeRadius = minDim * 0.105f
        for (index in 0..1) {
            val gaugeX = w * if (index == 0) 0.27f else 0.73f
            val gaugeColor = if (index == 0) cyan else amber
            paint.color = 0xFF07090C.toInt()
            canvas.drawCircle(gaugeX, gaugeY, gaugeRadius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.006f
            paint.color = withAlpha(0xFF54616A.toInt(), (0.72f * dim * 255).toInt())
            canvas.drawCircle(gaugeX, gaugeY, gaugeRadius, paint)
            paint.strokeWidth = minDim * 0.004f
            paint.color = withAlpha(gaugeColor, (0.55f * dim * 255).toInt())
            canvas.drawCircle(gaugeX, gaugeY, gaugeRadius * 0.82f, paint)
            paint.style = Paint.Style.FILL
            for (tick in 0..8) {
                val angle = (-PI * 0.82 + tick * (PI * 1.64 / 8.0)).toFloat()
                val inner = gaugeRadius * 0.66f
                val outer = gaugeRadius * 0.76f
                paint.color = withAlpha(gaugeColor, (0.55f * dim * 255).toInt())
                paint.strokeWidth = minDim * 0.003f
                canvas.drawLine(
                    gaugeX + cos(angle) * inner, gaugeY + sin(angle) * inner,
                    gaugeX + cos(angle) * outer, gaugeY + sin(angle) * outer,
                    paint,
                )
            }
            val needleAngle = (-PI * 0.60 + (0.42 + 0.06 * sin(t01 * 2f * PI.toFloat() + index)) * PI * 1.2).toFloat()
            paint.color = withAlpha(gaugeColor, (0.95f * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.004f
            canvas.drawLine(
                gaugeX, gaugeY,
                gaugeX + cos(needleAngle) * gaugeRadius * 0.60f,
                gaugeY + sin(needleAngle) * gaugeRadius * 0.60f,
                paint,
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(gaugeX, gaugeY, minDim * 0.008f, paint)
        }

        // Center console and softly illuminated switches.
        paint.color = 0xFF0B0D11.toInt()
        val console = android.graphics.RectF(w * 0.43f, h * 0.74f, w * 0.57f, h * 0.99f)
        canvas.drawRoundRect(console, minDim * 0.035f, minDim * 0.035f, paint)
        paint.color = withAlpha(cyan, (0.28f * dim * 255).toInt())
        canvas.drawRoundRect(w * 0.455f, h * 0.77f, w * 0.545f, h * 0.805f, minDim * 0.008f, minDim * 0.008f, paint)
        for (i in 0..2) {
            paint.color = withAlpha(if (i == 1) amber else cyan, (0.74f * dim * 255).toInt())
            canvas.drawCircle(w * (0.46f + 0.04f * i), h * 0.84f, minDim * 0.009f, paint)
        }
        paint.color = withAlpha(0xFF42474A.toInt(), (0.65f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.018f
        canvas.drawLine(w * 0.59f, h * 0.92f, w * 0.63f, h * 0.82f, paint)
        paint.color = 0xFF22272A.toInt()
        canvas.drawCircle(w * 0.635f, h * 0.81f, minDim * 0.022f, paint)
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
