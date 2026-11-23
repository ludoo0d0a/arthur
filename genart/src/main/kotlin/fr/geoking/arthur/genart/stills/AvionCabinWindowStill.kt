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

/** Bakes one frozen frame of an airplane cabin window and drifting cloudscape. */
internal object AvionCabinWindowStill {
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
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.4f, octaves = 3, seedOffset = 215)
        val dim = 0.85f + 0.15f * pulse
        val left = w * 0.12f
        val top = h * 0.11f
        val windowW = w * 0.76f
        val windowH = h * 0.70f
        val corner = minDim * 0.105f
        val accent = mixArgb(0xFF80B0E0.toInt(), palette.colorAt(0), 0.35f)
        val drift = t01 * minDim * 0.10f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF05070A.toInt(), 0xFF080A0C.toInt(), 0xFF111820.toInt()),
            floatArrayOf(0f, 0.54f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Diffuse exterior light on the cabin wall.
        paint.shader = RadialGradient(
            w * 0.5f, top + windowH * 0.44f, minDim * 0.62f,
            intArrayOf(withAlpha(accent, (0.11f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        rect.set(left - minDim * 0.08f, top - minDim * 0.05f, left + windowW + minDim * 0.08f, top + windowH + minDim * 0.05f)
        canvas.drawRoundRect(rect, corner + minDim * 0.05f, corner + minDim * 0.05f, paint)
        paint.shader = null

        // Glass and high-altitude sky.
        paint.shader = LinearGradient(
            0f, top, 0f, top + windowH,
            intArrayOf(0xFF0E1420.toInt(), 0xFF142033.toInt(), 0xFF1A2438.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        rect.set(left, top, left + windowW, top + windowH)
        canvas.drawRoundRect(rect, corner, corner, paint)
        paint.shader = null

        canvas.save()
        canvas.clipRect(left + minDim * 0.02f, top + minDim * 0.02f, left + windowW - minDim * 0.02f, top + windowH - minDim * 0.02f)
        val cloudY = top + windowH * (0.63f + 0.025f * (wash - 0.5f))
        paint.shader = LinearGradient(
            0f, cloudY - windowH * 0.22f, 0f, cloudY + windowH * 0.18f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(left + minDim * 0.02f, cloudY - windowH * 0.22f, left + windowW - minDim * 0.02f, cloudY + windowH * 0.18f, paint)
        paint.shader = null

        // Soft cloud bands share the animated engine's slow horizontal drift.
        for (i in 0 until 7) {
            val lane = i / 7f
            val n = loopedFbm(t01, radius = 1.1f + lane, octaves = 2, seedOffset = 240 + i * 17)
            val y = top + windowH * (0.43f + lane * 0.42f)
            val x = left - windowW * 0.32f + ((drift * (0.65f + lane * 0.45f) + i * windowW * 0.34f) % (windowW * 1.64f))
            val bandW = windowW * (0.42f + 0.25f * n)
            val bandH = windowH * (0.065f + 0.045f * n)
            val path = Path().apply {
                moveTo(x, y + bandH * 0.25f)
                cubicTo(x + bandW * 0.22f, y - bandH * 0.22f, x + bandW * 0.30f, y + bandH * 0.12f, x + bandW * 0.52f, y)
                cubicTo(x + bandW * 0.72f, y - bandH * 0.20f, x + bandW * 0.80f, y + bandH * 0.36f, x + bandW, y + bandH * 0.08f)
            }
            paint.color = withAlpha(mixArgb(0xFF526783.toInt(), 0xFFB5C9DF.toInt(), 0.35f + 0.35f * n), ((0.10f + 0.10f * n) * dim * 255).toInt())
            paint.strokeWidth = bandH
            paint.strokeCap = Paint.Cap.ROUND
            paint.style = Paint.Style.STROKE
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }

        // Haze behind the cloud layer.
        paint.shader = RadialGradient(
            left + windowW * 0.54f, cloudY, windowW * 0.56f,
            intArrayOf(withAlpha(accent, (0.16f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(left + windowW * 0.54f, cloudY, windowW * 0.56f, paint)
        paint.shader = null
        canvas.restore()

        // Gasket and inner reflected bevel.
        paint.color = withAlpha(0xFF080A0C.toInt(), (0.96f * dim * 255).toInt())
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.027f
        rect.set(left - minDim * 0.023f, top - minDim * 0.023f, left + windowW + minDim * 0.023f, top + windowH + minDim * 0.023f)
        canvas.drawRoundRect(rect, corner + minDim * 0.022f, corner + minDim * 0.022f, paint)
        paint.color = withAlpha(0xFF53647A.toInt(), (0.25f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.006f
        rect.set(left + minDim * 0.008f, top + minDim * 0.008f, left + windowW - minDim * 0.008f, top + windowH - minDim * 0.008f)
        canvas.drawRoundRect(rect, corner - minDim * 0.008f, corner - minDim * 0.008f, paint)
        paint.style = Paint.Style.FILL

        // Cabin shadow and padded sill.
        paint.shader = LinearGradient(
            0f, top + windowH - minDim * 0.015f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF0A0C10.toInt(), (0.65f * dim * 255).toInt()), withAlpha(0xFF0A0C10.toInt(), (0.95f * dim * 255).toInt())),
            floatArrayOf(0f, 0.20f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top + windowH - minDim * 0.015f, w, h, paint)
        paint.shader = null
        paint.color = withAlpha(0xFF151A22.toInt(), (0.75f * dim * 255).toInt())
        rect.set(left - minDim * 0.045f, top + windowH - minDim * 0.008f, left + windowW + minDim * 0.045f, top + windowH + minDim * 0.034f)
        canvas.drawRoundRect(rect, minDim * 0.012f, minDim * 0.012f, paint)
        paint.color = withAlpha(accent, (0.12f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.003f
        canvas.drawLine(left + windowW * 0.08f, top + windowH + minDim * 0.004f, left + windowW * 0.92f, top + windowH + minDim * 0.004f, paint)
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
