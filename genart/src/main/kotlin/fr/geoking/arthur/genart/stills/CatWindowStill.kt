package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Cat at Window for Auto/Ambient album art. */
internal object CatWindowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val path = Path()

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
        val minDim = w.coerceAtMost(h)

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = 0xFF1A2238.toInt()
        canvas.drawRect(0f, 0f, w, h * 0.45f, paint)
        paint.color = 0xFF12161F.toInt()
        canvas.drawRect(0f, h * 0.45f, w, h, paint)

        val paneLeft = w * 0.12f
        val paneTop = h * 0.08f
        val paneW = w * 0.76f
        val paneH = h * 0.62f
        val dawn = mixArgb(0xFFFFD8A8.toInt(), palette.colorAt(1), 0.25f)
        paint.shader = RadialGradient(
            paneLeft + paneW * 0.55f,
            paneTop + paneH * 0.35f,
            paneW * 0.7f,
            intArrayOf(
                Color.argb(140, Color.red(dawn), Color.green(dawn), Color.blue(dawn)),
                Color.argb(90, 0x6B, 0x8A, 0xB8),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(paneLeft, paneTop, paneLeft + paneW, paneTop + paneH, paint)
        paint.shader = null

        paint.color = 0xFF1C1410.toInt()
        val fs = minDim * 0.018f
        canvas.drawRect(paneLeft, paneTop, paneLeft + paneW, paneTop + fs, paint)
        canvas.drawRect(paneLeft, paneTop + paneH - fs, paneLeft + paneW, paneTop + paneH, paint)
        canvas.drawRect(paneLeft, paneTop, paneLeft + fs, paneTop + paneH, paint)
        canvas.drawRect(paneLeft + paneW - fs, paneTop, paneLeft + paneW, paneTop + paneH, paint)
        canvas.drawRect(
            paneLeft + paneW * 0.5f - fs * 0.5f,
            paneTop,
            paneLeft + paneW * 0.5f + fs * 0.5f,
            paneTop + paneH,
            paint,
        )

        val sillY = paneTop + paneH
        paint.color = 0xFF2A211C.toInt()
        canvas.drawRect(paneLeft - minDim * 0.02f, sillY, paneLeft + paneW + minDim * 0.02f, sillY + minDim * 0.06f, paint)

        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.025f * breath * pulse
        val bodyColor = mixArgb(0xFF1A1412.toInt(), palette.colorAt(0), 0.2f)
        paint.color = bodyColor
        val cx = w * 0.58f
        val cy = sillY - minDim * 0.02f
        val bodyRx = minDim * 0.14f * scale
        val bodyRy = minDim * 0.10f * scale
        rect.set(cx - bodyRx, cy - bodyRy * 1.6f, cx + bodyRx, cy + bodyRy * 0.4f)
        canvas.drawOval(rect, paint)
        val headR = minDim * 0.075f * scale
        val hx = cx - bodyRx * 0.15f
        val hy = cy - bodyRy * 2.15f
        canvas.drawCircle(hx, hy, headR, paint)
        path.reset()
        path.moveTo(hx - headR * 0.75f, hy - headR * 0.2f)
        path.lineTo(hx - headR * 0.95f, hy - headR * 1.35f)
        path.lineTo(hx - headR * 0.15f, hy - headR * 0.55f)
        path.close()
        path.moveTo(hx + headR * 0.15f, hy - headR * 0.55f)
        path.lineTo(hx + headR * 0.95f, hy - headR * 1.35f)
        path.lineTo(hx + headR * 0.75f, hy - headR * 0.2f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
