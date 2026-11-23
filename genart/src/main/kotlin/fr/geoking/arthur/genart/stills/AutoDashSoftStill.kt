package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Auto Dash Soft for Auto/Ambient album art. */
internal object AutoDashSoftStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val road = Path()
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
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val atmosphere = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val horizonY = h * (0.43f + 0.018f * (atmosphere - 0.5f))
        val cx = w * 0.5f
        val accent = mixArgb(0xFF80C0E0.toInt(), palette.colorAt(0), 0.28f)
        val amber = 0xFFFFC98A.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF101418.toInt(), 0xFF1C242C.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Cool distant glow and a subtle warm roadside light.
        paint.shader = RadialGradient(
            cx, horizonY, minDim * 0.34f,
            intArrayOf(withAlpha(accent, (0.24f * dim * 255).toInt()), withAlpha(accent, (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, horizonY, minDim * 0.34f, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            w * 0.77f, horizonY + h * 0.03f, minDim * 0.13f,
            intArrayOf(withAlpha(amber, (0.25f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.77f, horizonY + h * 0.03f, minDim * 0.13f, paint)
        paint.shader = null

        // Perspective road plane.
        road.reset()
        road.moveTo(cx - w * 0.018f, horizonY)
        road.lineTo(cx + w * 0.018f, horizonY)
        road.lineTo(w * 0.96f, h)
        road.lineTo(w * 0.04f, h)
        road.close()
        paint.shader = LinearGradient(
            0f, horizonY, 0f, h,
            intArrayOf(0xFF172027.toInt(), 0xFF0B0E12.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(road, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.002f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(accent, (0.12f * dim * 255).toInt())
        canvas.drawLine(cx - w * 0.02f, horizonY, w * 0.04f, h, paint)
        canvas.drawLine(cx + w * 0.02f, horizonY, w * 0.96f, h, paint)

        // Evenly spaced frozen road dashes retain the vanishing-point perspective.
        val dashCount = 7
        for (i in 0 until dashCount) {
            val depthPhase = (t01 + i.toFloat() / dashCount) % 1f
            val depth = depthPhase * depthPhase
            val y = horizonY + (h - horizonY) * depth
            val perspective = 0.18f + depth * 1.55f
            val dashH = minDim * (0.005f + depth * 0.027f) * perspective
            val dashW = minDim * (0.003f + depth * 0.008f) * perspective
            val shimmer = 0.82f + 0.18f * sin((t01 + i.toFloat() / dashCount) * 2f * PI.toFloat())
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(accent, ((0.24f + 0.28f * depth) * shimmer * dim * 255).toInt())
            rect.set(cx - dashW * 0.5f, y - dashH * 0.5f, cx + dashW * 0.5f, y + dashH * 0.5f)
            canvas.drawRoundRect(rect, dashW, dashW, paint)
        }

        // Dark lower band suggests the dashboard without competing detail.
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.35f * dim * 255).toInt()), withAlpha(0xFF080A0C.toInt(), (0.92f * dim * 255).toInt())),
            floatArrayOf(0f, 0.25f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.004f
        paint.color = withAlpha(amber, (0.10f * dim * 255).toInt())
        rect.set(w * 0.24f, h * 0.83f, w * 0.76f, h * 1.13f)
        canvas.drawArc(rect, 205f, 130f, false, paint)
        paint.style = Paint.Style.FILL
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
