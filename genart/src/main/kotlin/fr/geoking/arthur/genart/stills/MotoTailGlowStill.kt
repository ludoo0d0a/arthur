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

/** Bakes a frozen frame of Moto Tail Glow for Auto/Ambient album art. */
internal object MotoTailGlowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
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
        val cx = w * 0.5f
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val atmosphere = loopedFbm(t01, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 37) - 0.5f
        val flicker = 0.92f + 0.08f * loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 71)
        val red = mixArgb(0xFFFF263F.toInt(), palette.colorAt(0), 0.2f)
        val hotRed = 0xFFFF5264.toInt()
        val horizonY = h * (0.49f + drift * 0.018f)
        val bikeY = h * 0.70f
        val tailY = bikeY - minDim * 0.115f
        val tailX = cx + drift * minDim * 0.018f
        val dim = 0.85f + 0.15f * pulse

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                0xFF08070C.toInt(),
                mixArgb(0xFF170A10.toInt(), 0xFF200A12.toInt(), atmosphere),
                0xFF09090D.toInt(),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Horizon haze and red glow cast behind the receding bike.
        paint.shader = LinearGradient(
            0f, horizonY - h * 0.16f, 0f, horizonY + h * 0.36f,
            intArrayOf(
                Color.TRANSPARENT,
                withAlpha(red, (0.11f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizonY - h * 0.16f, w, horizonY + h * 0.36f, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            tailX, tailY, minDim * 0.52f,
            intArrayOf(
                withAlpha(red, (0.17f * dim * flicker * 255).toInt()),
                withAlpha(red, (0.065f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(tailX, tailY, minDim * 0.52f, paint)
        paint.shader = null

        // Converging road plane.
        path.reset()
        path.moveTo(cx - w * 0.055f, horizonY)
        path.lineTo(cx + w * 0.055f, horizonY)
        path.lineTo(w * 1.15f, h)
        path.lineTo(-w * 0.15f, h)
        path.close()
        paint.shader = LinearGradient(
            0f, horizonY, 0f, h,
            intArrayOf(0xFF171116.toInt(), 0xFF121116.toInt(), 0xFF08090D.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // Red tail-light streaks that soften into the asphalt.
        val streaks = 7
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until streaks) {
            val side = if (i % 2 == 0) -1f else 1f
            val lane = i / 2 + 1
            val xEnd = tailX + side * minDim * (0.14f + lane * 0.065f)
            val yEnd = tailY + minDim * (0.12f + lane * 0.035f)
            val sway = sin(t01 * 2f * PI.toFloat() + i * 1.7f) * minDim * 0.009f
            path.reset()
            path.moveTo(tailX + side * minDim * 0.014f, tailY + minDim * 0.012f)
            path.cubicTo(
                tailX + sway + side * minDim * 0.035f,
                tailY + minDim * 0.055f,
                xEnd - sway * 0.4f,
                yEnd - minDim * 0.04f,
                xEnd,
                yEnd,
            )
            paint.color = withAlpha(red, ((0.075f + (1f - lane / 7f).coerceIn(0f, 1f) * 0.08f) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.005f + (1f - lane / 7f).coerceIn(0f, 1f) * 0.006f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL

        // Elongated reflection below the tail.
        paint.shader = RadialGradient(
            tailX, bikeY + minDim * 0.17f, minDim * 0.33f,
            intArrayOf(
                withAlpha(hotRed, (0.24f * dim * flicker * 255).toInt()),
                withAlpha(red, (0.15f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        rect.set(tailX - minDim * 0.3f, bikeY + minDim * 0.045f, tailX + minDim * 0.3f, bikeY + minDim * 0.495f)
        canvas.drawOval(rect, paint)
        paint.shader = null
        for (i in 0..2) {
            val y = bikeY + minDim * (0.13f + i * 0.07f)
            val halfWidth = minDim * (0.095f - i * 0.018f)
            paint.color = withAlpha(red, ((0.14f - i * 0.035f) * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.006f
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(cx - halfWidth, y, cx + halfWidth, y, paint)
        }

        // Small distant rear wheel and body silhouette.
        paint.style = Paint.Style.STROKE
        paint.color = 0xFF08080C.toInt()
        paint.strokeWidth = minDim * 0.012f
        canvas.drawCircle(cx, bikeY + minDim * 0.048f, minDim * 0.052f, paint)
        paint.color = withAlpha(0xFF351019.toInt(), (0.7f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.002f
        canvas.drawCircle(cx, bikeY + minDim * 0.048f, minDim * 0.052f, paint)

        paint.style = Paint.Style.FILL
        paint.color = 0xFF08080C.toInt()
        path.reset()
        path.moveTo(cx - minDim * 0.055f, bikeY - minDim * 0.025f)
        path.lineTo(cx - minDim * 0.032f, bikeY - minDim * 0.064f)
        path.quadTo(cx, bikeY - minDim * 0.078f, cx + minDim * 0.032f, bikeY - minDim * 0.064f)
        path.lineTo(cx + minDim * 0.055f, bikeY - minDim * 0.025f)
        path.lineTo(cx + minDim * 0.035f, bikeY + minDim * 0.024f)
        path.lineTo(cx - minDim * 0.035f, bikeY + minDim * 0.024f)
        path.close()
        canvas.drawPath(path, paint)
        rect.set(cx - minDim * 0.032f, bikeY - minDim * 0.1f, cx + minDim * 0.032f, bikeY - minDim * 0.075f)
        canvas.drawRoundRect(rect, minDim * 0.012f, minDim * 0.012f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = withAlpha(0xFF1A1117.toInt(), (0.9f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.004f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx - minDim * 0.024f, bikeY - minDim * 0.055f, cx + minDim * 0.024f, bikeY - minDim * 0.055f, paint)

        // Compact bright red lamp with a feathered halo.
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            tailX, tailY, minDim * 0.11f,
            intArrayOf(withAlpha(hotRed, (0.52f * dim * flicker * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(tailX, tailY, minDim * 0.11f, paint)
        paint.shader = null
        paint.color = withAlpha(hotRed, (0.8f * dim * flicker * 255).toInt())
        rect.set(tailX - minDim * 0.018f, tailY - minDim * 0.007f, tailX + minDim * 0.018f, tailY + minDim * 0.007f)
        canvas.drawRoundRect(rect, minDim * 0.007f, minDim * 0.007f, paint)
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
