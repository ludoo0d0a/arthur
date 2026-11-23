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
import fr.geoking.arthur.genart.positiveRadius
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Hearth Glow for Auto/Ambient album art. */
internal object HearthGlowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val arch = Path()
    private val flame = Path()
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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = (0.85f + 0.15f * pulse).coerceIn(0f, 1f)
        val flicker = loopedFbm(t, radius = 1.25f, octaves = 3, seedOffset = 41)
        val sway = (loopedFbm(t, radius = 1.35f, octaves = 2, seedOffset = 71) - 0.5f) * minDim * 0.035f
        val cx = w * 0.5f
        val baseY = h * 0.82f
        val topY = h * 0.43f
        val archW = minDim * 0.72f
        val left = cx - archW * 0.5f
        val right = cx + archW * 0.5f
        val shoulderY = topY + (baseY - topY) * 0.34f
        val accent = mixArgb(0xFFFF7A30.toInt(), palette.colorAt(0), 0.22f)

        // Dark, warm room with a low pool of reflected firelight.
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF090504.toInt(), 0xFF160A07.toInt(), 0xFF28110A.toInt(), 0xFF100805.toInt()),
            floatArrayOf(0f, 0.47f, 0.82f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            cx + sway, baseY - minDim * 0.08f, minDim * 0.72f,
            intArrayOf(withAlpha(accent, (75 * dim).toInt()), withAlpha(0xFF5A1D0C.toInt(), (28 * dim).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx + sway, baseY - minDim * 0.08f, minDim * 0.72f, paint)
        paint.shader = null

        // Masonry surround and arched firebox.
        rect.set(left - minDim * 0.045f, topY + minDim * 0.02f, right + minDim * 0.045f, baseY + minDim * 0.02f)
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(0xFF24140F.toInt(), 0xFF493027.toInt(), 0xFF24130E.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, minDim * 0.035f, minDim * 0.035f, paint)
        paint.shader = null
        arch.reset()
        arch.moveTo(left, baseY)
        arch.lineTo(left, shoulderY)
        arch.cubicTo(left, topY + minDim * 0.08f, cx - archW * 0.22f, topY, cx, topY)
        arch.cubicTo(cx + archW * 0.22f, topY, right, topY + minDim * 0.08f, right, shoulderY)
        arch.lineTo(right, baseY)
        arch.close()
        paint.shader = LinearGradient(0f, topY, 0f, baseY,
            intArrayOf(0xFF090504.toInt(), 0xFF180906.toInt(), 0xFF260D06.toInt()),
            floatArrayOf(0f, 0.72f, 1f), Shader.TileMode.CLAMP)
        canvas.drawPath(arch, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.011f
        paint.color = withAlpha(0xFF9A5430.toInt(), (85 * dim).toInt())
        canvas.drawPath(arch, paint)
        paint.style = Paint.Style.FILL

        // Mantel shelf and subtle horizontal stone courses.
        rect.set(left - minDim * 0.09f, topY - minDim * 0.022f, right + minDim * 0.09f, topY + minDim * 0.035f)
        paint.color = 0xFF20110C.toInt()
        canvas.drawRoundRect(rect, minDim * 0.012f, minDim * 0.012f, paint)
        paint.color = withAlpha(0xFF76503A.toInt(), (65 * dim).toInt())
        canvas.drawLine(rect.left + minDim * 0.025f, rect.bottom, rect.right - minDim * 0.025f, rect.bottom, paint)
        paint.strokeWidth = minDim * 0.0025f
        paint.color = withAlpha(0xFF120A07.toInt(), (130 * dim).toInt())
        canvas.drawLine(0f, h * 0.90f, w, h * 0.90f, paint)

        // Low, layered ember bed.
        val fireY = baseY - minDim * 0.07f
        paint.shader = RadialGradient(
            cx + sway, fireY, minDim * 0.34f,
            intArrayOf(withAlpha(0xFFFFD16B.toInt(), (150 * flicker * dim).toInt()), withAlpha(0xFFFF641E.toInt(), (115 * dim).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - minDim * 0.30f, fireY - minDim * 0.13f, cx + minDim * 0.30f, fireY + minDim * 0.14f, paint)
        paint.shader = null
        paint.color = 0xFF30140B.toInt()
        canvas.drawOval(cx - minDim * 0.25f, baseY - minDim * 0.09f, cx + minDim * 0.25f, baseY - minDim * 0.035f, paint)

        // Crossing charred logs with glowing edges.
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.045f
        paint.color = 0xFF180B07.toInt()
        canvas.drawLine(cx - minDim * 0.24f, baseY - minDim * 0.12f, cx + minDim * 0.22f, baseY - minDim * 0.045f, paint)
        canvas.drawLine(cx + minDim * 0.23f, baseY - minDim * 0.12f, cx - minDim * 0.20f, baseY - minDim * 0.045f, paint)
        paint.strokeWidth = minDim * 0.006f
        paint.color = withAlpha(0xFFFF7C2C.toInt(), (145 * dim).toInt())
        canvas.drawLine(cx - minDim * 0.22f, baseY - minDim * 0.13f, cx + minDim * 0.18f, baseY - minDim * 0.065f, paint)
        paint.style = Paint.Style.FILL

        // Three soft-edged flame tongues, gently shifted by the loop.
        val flameScale = 0.88f + flicker * 0.22f
        for (i in 0..2) {
            val fx = cx + sway + (i - 1) * minDim * 0.105f
            val fh = minDim * (0.27f + 0.06f * ((i + 1) % 2)) * flameScale
            val fw = minDim * (0.095f + 0.012f * (i % 2))
            val fy = fireY - fh * (0.55f + 0.05f * sin(t * 2f * PI.toFloat() + i))
            flame.reset()
            flame.moveTo(fx - fw, fireY)
            flame.cubicTo(fx - fw * 1.20f, fy + fh * 0.36f, fx - fw * 0.12f, fy + fh * 0.12f, fx + sway * 0.16f, fy)
            flame.cubicTo(fx + fw * 0.65f, fy + fh * 0.18f, fx + fw * 0.92f, fy + fh * 0.57f, fx + fw, fireY)
            flame.close()
            paint.shader = LinearGradient(fx, fy, fx, fireY,
                intArrayOf(0xFFFFE8A0.toInt(), 0xFFFF9B35.toInt(), 0xFFE94516.toInt()),
                floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP)
            canvas.drawPath(flame, paint)
            paint.shader = null
        }

        // Suspended sparks, rising through the firebox.
        for (i in 0 until 9) {
            val seed = loopedFbm(t, radius = 1.15f + i * 0.04f, octaves = 2, seedOffset = 137 + i * 17)
            val fall = (t * (0.42f + (i % 4) * 0.09f) + i * 0.173f) % 1f
            val x = cx + (seed - 0.5f) * minDim * 0.35f + sin(t * 2f * PI.toFloat() + i) * minDim * 0.025f
            val y = fireY - fall * minDim * 0.38f
            val r = (minDim * (0.0025f + seed * 0.0035f)).positiveRadius(0.5f)
            paint.color = withAlpha(if (i % 3 == 0) 0xFFFFE7A0.toInt() else 0xFFFF8A36.toInt(), ((90 + 130 * seed) * dim).toInt())
            canvas.drawCircle(x, y, r, paint)
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
