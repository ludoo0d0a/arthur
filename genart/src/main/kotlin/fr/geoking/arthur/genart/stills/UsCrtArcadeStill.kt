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
import kotlin.math.abs
import kotlin.math.sin

/** Bakes one frozen frame of US CRT Arcade for Auto/Ambient album art. */
internal object UsCrtArcadeStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val shimmer = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = 11)
        val roll = (t01 * 2f + 0.16f * shimmer) % 1f
        val dim = 0.85f + 0.15f * pulse
        val phosphor = mixArgb(0xFF40FF80.toInt(), palette.colorAt(0), 0.2f)
        val screenLeft = w * 0.09f
        val screenTop = h * 0.13f
        val screenWidth = w * 0.82f
        val screenHeight = h * 0.69f
        val screenRight = screenLeft + screenWidth
        val screenBottom = screenTop + screenHeight
        val cx = w * 0.5f
        val cy = screenTop + screenHeight * 0.47f
        val rect = RectF()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF050609.toInt(), 0xFF11151A.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        rect.set(w * 0.045f, h * 0.075f, w * 0.955f, h * 0.865f)
        paint.color = 0xFF080A0C.toInt()
        canvas.drawRoundRect(rect, minDim * 0.055f, minDim * 0.055f, paint)
        rect.set(w * 0.065f, h * 0.095f, w * 0.935f, h * 0.845f)
        paint.shader = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            intArrayOf(0xFF222831.toInt(), 0xFF12161C.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, minDim * 0.045f, minDim * 0.045f, paint)
        paint.shader = null

        rect.set(screenLeft, screenTop, screenRight, screenBottom)
        paint.shader = LinearGradient(
            0f, screenTop, 0f, screenBottom,
            intArrayOf(0xFF03100C.toInt(), mixArgb(0xFF07150F.toInt(), phosphor, 0.08f), 0xFF020806.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, minDim * 0.028f, minDim * 0.028f, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            cx, cy, minDim * 0.52f,
            intArrayOf(withAlpha(phosphor, ((0.16f + 0.035f * shimmer) * dim * 255).toInt()), withAlpha(phosphor, (0.055f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.52f, paint)
        paint.shader = null

        val horizon = screenTop + screenHeight * 0.48f
        val screenGridBottom = screenBottom - screenHeight * 0.04f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.0014f
        for (i in 0..6) {
            val f = i / 6f
            val y = horizon + (screenBottom - horizon) * f * f
            paint.color = withAlpha(phosphor, ((0.16f + 0.08f * (1f - f)) * dim * 255).toInt())
            canvas.drawLine(screenLeft + screenWidth * (0.08f + 0.13f * f), y, screenRight - screenWidth * (0.08f + 0.13f * f), y, paint)
        }
        for (i in -4..4) {
            val x = cx + screenWidth * i * 0.105f
            paint.color = withAlpha(phosphor, (0.14f * dim * 255).toInt())
            canvas.drawLine(cx + (x - cx) * 0.12f, horizon, x, screenGridBottom, paint)
        }

        val orbY = screenTop + screenHeight * 0.40f
        val orbRadius = screenWidth * (0.105f + 0.006f * sin(t01 * 2f * PI.toFloat()))
        paint.color = withAlpha(0xFF40FF80.toInt(), (0.16f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.002f
        canvas.drawCircle(cx, orbY, orbRadius + minDim * 0.012f, paint)
        paint.color = withAlpha(phosphor, (0.58f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.003f
        canvas.drawCircle(cx, orbY, orbRadius, paint)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, orbY, orbRadius * 1.55f,
            intArrayOf(withAlpha(phosphor, (0.17f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, orbY, orbRadius * 1.55f, paint)
        paint.shader = null

        // Horizontal dark scanlines and a faint, looping phosphor roll.
        paint.style = Paint.Style.STROKE
        val scanlineCount = 48
        for (i in 0 until scanlineCount) {
            val y = screenTop + screenHeight * i / scanlineCount
            val distance = abs(i.toFloat() / scanlineCount - roll)
            val rollGlow = (1f - distance * 14f).coerceIn(0f, 1f)
            paint.strokeWidth = minDim * 0.0018f
            paint.color = withAlpha(0xFF010503.toInt(), ((0.20f + 0.08f * (1f - shimmer)) * dim * 255).toInt())
            canvas.drawLine(screenLeft + minDim * 0.008f, y, screenRight - minDim * 0.008f, y, paint)
            if (rollGlow > 0f) {
                paint.strokeWidth = minDim * 0.004f
                paint.color = withAlpha(phosphor, (rollGlow * 0.045f * dim * 255).toInt())
                canvas.drawLine(screenLeft + minDim * 0.008f, y, screenRight - minDim * 0.008f, y, paint)
            }
        }
        paint.style = Paint.Style.FILL

        // Glass rim and faint curved catchlight.
        rect.set(screenLeft, screenTop, screenRight, screenBottom)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.003f
        paint.color = withAlpha(0xFF9CC8AC.toInt(), (0.2f * dim * 255).toInt())
        canvas.drawRoundRect(rect, minDim * 0.028f, minDim * 0.028f, paint)
        paint.color = withAlpha(Color.WHITE, (0.035f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.018f
        val reflection = Path().apply {
            moveTo(screenLeft + screenWidth * 0.08f, screenTop + screenHeight * 0.03f)
            cubicTo(screenLeft + screenWidth * 0.38f, screenTop + screenHeight * 0.08f, screenLeft + screenWidth * 0.48f, screenTop + screenHeight * 0.18f, screenLeft + screenWidth * 0.57f, screenTop + screenHeight * 0.30f)
        }
        canvas.drawPath(reflection, paint)
        paint.style = Paint.Style.FILL

        rect.set(w * 0.19f, h * 0.79f, w * 0.81f, h * 0.86f)
        paint.color = withAlpha(0xFF030405.toInt(), (0.8f * dim * 255).toInt())
        canvas.drawRoundRect(rect, minDim * 0.018f, minDim * 0.018f, paint)
        paint.color = withAlpha(0xFFFF554C.toInt(), (0.55f * dim * 255).toInt())
        canvas.drawCircle(w * 0.68f, h * 0.825f, minDim * 0.012f, paint)
        paint.color = withAlpha(0xFF38D9B0.toInt(), (0.48f * dim * 255).toInt())
        canvas.drawCircle(w * 0.74f, h * 0.825f, minDim * 0.012f, paint)
        paint.color = withAlpha(0xFF304050.toInt(), (0.75f * dim * 255).toInt())
        canvas.drawCircle(w * 0.32f, h * 0.825f, minDim * 0.018f, paint)
    }

    private fun mixArgb(a: Int, b: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt().coerceIn(0, 255)
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, blue)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
