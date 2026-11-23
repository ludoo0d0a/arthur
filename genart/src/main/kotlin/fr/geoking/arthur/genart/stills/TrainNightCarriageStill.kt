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

/** Bakes a quiet, warmly lit night train carriage interior. */
internal object TrainNightCarriageStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val ambient = loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 17)
        val dim = 0.85f + 0.15f * pulse
        val warm = mixArgb(0xFFFFC070.toInt(), palette.colorAt(0), 0.22f)
        val cool = 0xFF657B96.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(0xFF090B10.toInt(), 0xFF0C0E14.toInt(), 0xFF181C28.toInt()),
            floatArrayOf(0f, 0.46f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Night scenery passes behind the two long side windows.
        val winTop = h * 0.15f
        val winBottom = h * 0.57f
        val winHeight = winBottom - winTop
        val winWidth = w * 0.245f
        for (side in 0..1) {
            val x = if (side == 0) w * 0.055f else w * 0.945f - winWidth
            rect.set(x - minDim * 0.012f, winTop - minDim * 0.012f, x + winWidth + minDim * 0.012f, winBottom + minDim * 0.012f)
            paint.color = withAlpha(0xFF292C34.toInt(), (0.84f * dim * 255).toInt())
            canvas.drawRoundRect(rect, minDim * 0.022f, minDim * 0.022f, paint)
            rect.set(x, winTop, x + winWidth, winBottom)
            paint.shader = LinearGradient(0f, winTop, 0f, winBottom,
                intArrayOf(0xFF070B14.toInt(), 0xFF111927.toInt(), 0xFF080B12.toInt()),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, minDim * 0.014f, minDim * 0.014f, paint)
            paint.shader = null

            val save = canvas.save()
            canvas.clipRect(x, winTop, x + winWidth, winBottom)
            val drift = (t01 * 0.34f + side * 0.31f) % 1f
            for (i in 0 until 7) {
                val yFrac = (0.12f + i * 0.17f + side * 0.09f) % 0.9f
                val bandY = winTop + winHeight * yFrac
                val n = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 101 + i * 13 + side * 51)
                val bandW = winWidth * (0.18f + 0.22f * n)
                val bandX = x + ((drift + i * 0.23f) % 1f) * (winWidth + bandW) - bandW
                val tint = if (i % 3 == 0) warm else cool
                paint.shader = LinearGradient(bandX, bandY, bandX + bandW, bandY,
                    intArrayOf(Color.TRANSPARENT, withAlpha(tint, (0.10f * dim * 255).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(bandX, bandY, bandX + bandW, bandY + minDim * (0.012f + 0.006f * (i % 2)), minDim * 0.02f, minDim * 0.02f, paint)
                paint.shader = null
            }
            // A very faint reflection on the glass.
            paint.shader = LinearGradient(x, 0f, x + winWidth, 0f,
                intArrayOf(withAlpha(0xFFD5D9E0.toInt(), (0.05f * dim * 255).toInt()), Color.TRANSPARENT,
                    withAlpha(0xFFD5D9E0.toInt(), (0.035f * dim * 255).toInt())),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(x, winTop, x + winWidth, winBottom, paint)
            paint.shader = null
            canvas.restoreToCount(save)
        }

        // Dark side panels and far door establish a narrow interior aisle.
        path.reset()
        path.moveTo(0f, h * 0.08f); path.lineTo(w * 0.045f, h * 0.15f); path.lineTo(w * 0.045f, h * 0.68f); path.lineTo(0f, h * 0.88f); path.close()
        paint.color = withAlpha(0xFF10131A.toInt(), (0.74f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(w, h * 0.08f); path.lineTo(w * 0.955f, h * 0.15f); path.lineTo(w * 0.955f, h * 0.68f); path.lineTo(w, h * 0.88f); path.close()
        canvas.drawPath(path, paint)

        rect.set(w * 0.425f, h * 0.20f, w * 0.575f, h * 0.57f)
        paint.color = withAlpha(0xFF252933.toInt(), (0.70f * dim * 255).toInt())
        canvas.drawRoundRect(rect, minDim * 0.018f, minDim * 0.018f, paint)
        rect.set(w * 0.442f, h * 0.22f, w * 0.558f, h * 0.52f)
        paint.shader = LinearGradient(0f, h * 0.22f, 0f, h * 0.52f,
            intArrayOf(0xFF080B12.toInt(), 0xFF151A24.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, minDim * 0.012f, minDim * 0.012f, paint)
        paint.shader = null
        paint.color = withAlpha(0xFF69717E.toInt(), (0.20f * dim * 255).toInt())
        canvas.drawRect(w * 0.4985f, h * 0.22f, w * 0.5015f, h * 0.52f, paint)

        // Ceiling panels and four warm lamps recede toward the door.
        path.reset()
        path.moveTo(0f, 0f); path.lineTo(w, 0f); path.lineTo(w * 0.66f, h * 0.24f); path.lineTo(w * 0.34f, h * 0.24f); path.close()
        paint.shader = LinearGradient(0f, 0f, 0f, h * 0.24f, intArrayOf(0xFF171A21.toInt(), 0xFF0D1017.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null
        paint.color = withAlpha(0xFF343741.toInt(), (0.30f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.006f
        canvas.drawLine(w * 0.05f, h * 0.06f, w * 0.38f, h * 0.23f, paint)
        canvas.drawLine(w * 0.95f, h * 0.06f, w * 0.62f, h * 0.23f, paint)
        for (i in 0 until 4) {
            val depth = i / 3f
            val y = h * (0.055f + 0.145f * depth)
            val lampW = minDim * (0.045f - 0.025f * depth)
            val lampH = minDim * (0.018f - 0.010f * depth)
            val breathe = 0.94f + 0.06f * sin(t01 * 2f * PI.toFloat() + i * 0.75f)
            val r = minDim * (0.20f - 0.10f * depth)
            paint.shader = RadialGradient(w * 0.5f, y + lampH, r,
                intArrayOf(withAlpha(warm, (0.14f * breathe * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(w * 0.5f, y + lampH, r, paint)
            paint.shader = null
            paint.color = withAlpha(0xFFFFE2B1.toInt(), (0.68f * breathe * dim * 255).toInt())
            canvas.drawRoundRect(w * 0.5f - lampW / 2f, y, w * 0.5f + lampW / 2f, y + lampH, lampH, lampH, paint)
        }

        // Receding floor and faint reflected light in the center aisle.
        path.reset()
        path.moveTo(w * 0.34f, h * 0.56f); path.lineTo(w * 0.66f, h * 0.56f); path.lineTo(w * 0.99f, h); path.lineTo(w * 0.01f, h); path.close()
        paint.shader = LinearGradient(0f, h * 0.56f, 0f, h,
            intArrayOf(0xFF151821.toInt(), 0xFF080A0F.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null
        paint.color = withAlpha(warm, (0.10f * (0.8f + 0.2f * ambient) * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.012f
        canvas.drawLine(w * 0.5f, h * 0.58f, w * 0.5f, h, paint)

        // Soft, empty seat silhouettes along the aisle.
        for (row in 0 until 4) {
            val depth = row / 3f
            val y = h * (0.49f + depth * 0.105f)
            val seatW = w * (0.19f - depth * 0.07f)
            val seatH = h * (0.19f - depth * 0.07f)
            val gap = w * (0.41f - depth * 0.08f)
            val alpha = (0.90f - depth * 0.22f) * dim
            for (side in 0..1) {
                val cx = if (side == 0) w * 0.5f - gap else w * 0.5f + gap
                val x = cx - seatW / 2f
                rect.set(x, y + seatH * 0.28f, x + seatW, y + seatH)
                paint.color = withAlpha(0xFF07090D.toInt(), (alpha * 255).toInt())
                canvas.drawRoundRect(rect, minDim * 0.022f, minDim * 0.022f, paint)
                rect.set(x + seatW * 0.07f, y, x + seatW * 0.93f, y + seatH * 0.46f)
                paint.color = withAlpha(if (row % 2 == 0) 0xFF171A21.toInt() else 0xFF13161D.toInt(), (alpha * 255).toInt())
                canvas.drawRoundRect(rect, minDim * 0.035f, minDim * 0.035f, paint)
                paint.color = withAlpha(warm, (0.07f * dim * 255).toInt())
                paint.strokeWidth = minDim * (0.002f + 0.001f * depth)
                canvas.drawLine(x + seatW * 0.18f, y + seatH * 0.43f, x + seatW * 0.82f, y + seatH * 0.43f, paint)
            }
        }

        // Window pillars and deep lower vignette.
        paint.color = withAlpha(0xFF06070A.toInt(), (0.70f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.018f
        canvas.drawLine(w * 0.32f, h * 0.13f, w * 0.32f, h * 0.62f, paint)
        canvas.drawLine(w * 0.68f, h * 0.13f, w * 0.68f, h * 0.62f, paint)
        paint.shader = LinearGradient(0f, h * 0.72f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF050609.toInt(), (0.42f * dim * 255).toInt())), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.72f, w, h, paint)
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
