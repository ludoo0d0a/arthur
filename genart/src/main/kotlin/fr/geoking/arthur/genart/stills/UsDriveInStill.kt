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

/** Bakes one frozen frame of US Drive-In Glow for Auto/Ambient album art. */
internal object UsDriveInStill {
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
        val minDim = minOf(w, h)
        val t = phase / (2f * PI.toFloat())
        val t01 = ((t % 1f) + 1f) % 1f
        val haze = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = 17)
        val flicker = 0.96f + 0.04f * loopedFbm(t01, radius = 2.1f, octaves = 2, seedOffset = 43)
        val dim = 0.85f + 0.15f * pulse
        val amber = mixArgb(0xFFFFB040.toInt(), palette.colorAt(0), 0.24f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0E0A14.toInt(), 0xFF1A1424.toInt(), 0xFF100D17.toInt()),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val horizon = h * 0.61f
        path.reset()
        path.moveTo(0f, horizon + h * 0.015f)
        path.cubicTo(w * 0.19f, horizon - h * 0.07f, w * 0.32f, horizon + h * 0.03f, w * 0.48f, horizon - h * 0.025f)
        path.cubicTo(w * 0.65f, horizon - h * 0.09f, w * 0.82f, horizon + h * 0.02f, w, horizon - h * 0.04f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = 0xFF090A10.toInt()
        canvas.drawPath(path, paint)

        paint.shader = LinearGradient(
            0f, horizon - h * 0.08f, 0f, horizon + h * 0.09f,
            intArrayOf(Color.TRANSPARENT, withAlpha(amber, (0.10f * dim * (0.8f + 0.2f * haze) * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.08f, w, horizon + h * 0.09f, paint)
        paint.shader = null

        val cx = w * 0.5f
        val screenCy = h * 0.345f
        val screenW = w * 0.72f
        val screenH = h * 0.31f
        val frameTop = screenCy - screenH * 0.5f
        paint.shader = RadialGradient(
            cx, screenCy, minDim * 0.62f,
            intArrayOf(withAlpha(amber, (0.23f * flicker * dim * 255).toInt()), withAlpha(0xFF9D5D4B.toInt(), (0.09f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, screenCy, minDim * 0.62f, paint)
        paint.shader = null

        rect.set(cx - screenW * 0.5f - minDim * 0.012f, frameTop - minDim * 0.012f, cx + screenW * 0.5f + minDim * 0.012f, frameTop + screenH + minDim * 0.012f)
        paint.color = 0xFF08090E.toInt()
        canvas.drawRoundRect(rect, minDim * 0.006f, minDim * 0.006f, paint)

        rect.set(cx - screenW * 0.5f, frameTop, cx + screenW * 0.5f, frameTop + screenH)
        paint.shader = LinearGradient(
            0f, frameTop, 0f, frameTop + screenH,
            intArrayOf(0xFF4C3835.toInt(), 0xFF9C6654.toInt(), 0xFF332E40.toInt(), 0xFF1E2535.toInt()),
            floatArrayOf(0f, 0.33f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, paint)
        paint.shader = null

        val imageY = frameTop + screenH * (0.54f + 0.025f * (haze - 0.5f))
        paint.shader = RadialGradient(
            cx - screenW * 0.13f, imageY, screenW * 0.42f,
            intArrayOf(withAlpha(0xFFFFC681.toInt(), (0.39f * flicker * dim * 255).toInt()), withAlpha(0xFFE29A7A.toInt(), (0.16f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.47f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - screenW * 0.48f, frameTop + screenH * 0.08f, cx + screenW * 0.48f, frameTop + screenH * 0.93f, paint)
        paint.shader = null
        for (i in 0 until 5) {
            val n = loopedFbm(t01, radius = 1.1f + i * 0.19f, octaves = 2, seedOffset = 81 + i * 13)
            val y = frameTop + screenH * (0.18f + i * 0.105f)
            paint.color = withAlpha(0xFFFFE3BE.toInt(), ((0.025f + 0.025f * n) * flicker * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.003f
            canvas.drawLine(cx - screenW * 0.49f, y, cx + screenW * 0.49f, y + screenH * 0.012f * sin(t01 * 2f * PI.toFloat() + i), paint)
        }
        path.reset()
        path.moveTo(cx - screenW * 0.49f, frameTop + screenH * 0.80f)
        path.cubicTo(cx - screenW * 0.25f, frameTop + screenH * 0.69f, cx - screenW * 0.16f, frameTop + screenH * 0.84f, cx + screenW * 0.02f, frameTop + screenH * 0.76f)
        path.cubicTo(cx + screenW * 0.2f, frameTop + screenH * 0.68f, cx + screenW * 0.34f, frameTop + screenH * 0.82f, cx + screenW * 0.49f, frameTop + screenH * 0.73f)
        path.lineTo(cx + screenW * 0.49f, frameTop + screenH)
        path.lineTo(cx - screenW * 0.49f, frameTop + screenH)
        path.close()
        paint.color = 0xFF28263A.toInt()
        canvas.drawPath(path, paint)

        paint.color = 0xFF08090D.toInt()
        paint.strokeWidth = minDim * 0.014f
        canvas.drawLine(cx, frameTop + screenH + minDim * 0.01f, cx, horizon + h * 0.06f, paint)
        paint.color = 0xFF14131A.toInt()
        paint.strokeWidth = minDim * 0.007f
        canvas.drawLine(w * 0.18f, horizon + h * 0.055f, w * 0.82f, horizon + h * 0.055f, paint)

        val carY = h * 0.82f
        val carFractions = floatArrayOf(0.19f, 0.39f, 0.63f, 0.82f)
        carFractions.forEachIndexed { index, fraction ->
            val carW = w * (0.15f + if (index % 2 == 0) 0.012f else 0f)
            val carH = h * (0.042f + if (index % 2 == 0) 0.006f else 0f)
            val x = w * fraction
            path.reset()
            path.moveTo(x - carW * 0.5f, carY)
            path.lineTo(x - carW * 0.43f, carY - carH * 0.42f)
            path.quadTo(x - carW * 0.28f, carY - carH * 0.53f, x - carW * 0.14f, carY - carH * 0.98f)
            path.lineTo(x + carW * 0.16f, carY - carH * 0.98f)
            path.quadTo(x + carW * 0.33f, carY - carH * 0.44f, x + carW * 0.45f, carY - carH * 0.35f)
            path.lineTo(x + carW * 0.5f, carY)
            path.close()
            paint.color = 0xFF07080C.toInt()
            canvas.drawPath(path, paint)
            canvas.drawCircle(x - carW * 0.29f, carY, carH * 0.21f, paint)
            canvas.drawCircle(x + carW * 0.29f, carY, carH * 0.21f, paint)
        }
        paint.shader = LinearGradient(0f, h * 0.84f, 0f, h, intArrayOf(Color.TRANSPARENT, 0xFF06070B.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.84f, w, h, paint)
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
