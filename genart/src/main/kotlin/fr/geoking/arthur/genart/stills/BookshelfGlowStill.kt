package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of the warmly lit bookshelf scene. */
internal object BookshelfGlowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val spineColors = intArrayOf(
        0xFF6E3024.toInt(), 0xFF9B6B3B.toInt(), 0xFF405044.toInt(),
        0xFF8A5234.toInt(), 0xFF564050.toInt(), 0xFFB17A42.toInt(),
    )

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
        val glowPulse = 0.88f + 0.12f * loopedFbm(t, radius = 1.3f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val gold = mixArgb(0xFFE8C080.toInt(), palette.colorAt(0), 0.18f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1C1410.toInt(), 0xFF3A2A1C.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.48f, minDim * 0.76f,
            intArrayOf(withAlpha(gold, (0.20f * glowPulse * dim * 255).toInt()), withAlpha(0xFF9B6337.toInt(), (0.09f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.48f, minDim * 0.76f, paint)
        paint.shader = null

        val frame = withAlpha(0xFF291A12.toInt(), (0.78f * dim * 255).toInt())
        val startX = w * 0.085f
        val endX = w * 0.915f
        val shelfWidth = endX - startX
        val rowHeight = h * 0.255f
        val top = h * 0.105f
        val gap = h * 0.033f
        rect.set(startX - minDim * 0.025f, top - minDim * 0.025f, endX + minDim * 0.025f, top + rowHeight * 3f + gap * 2f + minDim * 0.06f)
        paint.color = frame
        canvas.drawRoundRect(rect, minDim * 0.018f, minDim * 0.018f, paint)
        rect.set(startX, top, endX, top + rowHeight * 3f + gap * 2f)
        paint.color = withAlpha(0xFF211710.toInt(), (0.92f * dim * 255).toInt())
        canvas.drawRoundRect(rect, minDim * 0.008f, minDim * 0.008f, paint)

        for (row in 0 until 3) {
            val rowTop = top + row * (rowHeight + gap)
            val bookBottom = rowTop + rowHeight - minDim * 0.022f
            var cursorX = startX + shelfWidth * 0.018f
            for (index in 0 until 12) {
                val widthFraction = seededRange(row * 101 + index * 17 + 3, 0.052f, 0.082f)
                val heightFraction = seededRange(row * 103 + index * 19 + 7, 0.19f, 0.28f)
                val inset = seededRange(row * 107 + index * 23 + 11, 0f, 0.035f)
                val bookWidth = shelfWidth * widthFraction
                val bookHeight = rowHeight * heightFraction
                if (cursorX + bookWidth < endX - shelfWidth * 0.012f) {
                    val bookTop = bookBottom - bookHeight - rowHeight * inset
                    paint.color = withAlpha(spineColors[(row * 13 + index) % spineColors.size], (0.92f * dim * 255).toInt())
                    rect.set(cursorX, bookTop, cursorX + bookWidth, bookTop + bookHeight)
                    canvas.drawRoundRect(rect, minDim * 0.004f, minDim * 0.004f, paint)
                    paint.color = withAlpha(0xFFE9BD78.toInt(), (0.20f * dim * 255).toInt())
                    paint.strokeWidth = minDim * 0.002f
                    canvas.drawLine(cursorX + bookWidth * 0.18f, bookTop + bookHeight * 0.12f, cursorX + bookWidth * 0.18f, bookTop + bookHeight * 0.88f, paint)
                    paint.color = withAlpha(gold, (0.42f * dim * 255).toInt())
                    paint.strokeWidth = minDim * 0.003f
                    canvas.drawLine(cursorX + bookWidth * 0.35f, bookTop + bookHeight * 0.32f, cursorX + bookWidth * 0.72f, bookTop + bookHeight * 0.32f, paint)
                    cursorX += bookWidth + shelfWidth * 0.008f
                }
            }

            val boardY = rowTop + rowHeight
            paint.shader = LinearGradient(
                0f, boardY - minDim * 0.018f, 0f, boardY + minDim * 0.047f,
                intArrayOf(withAlpha(gold, (0.14f * glowPulse * dim * 255).toInt()), withAlpha(0xFF4A3020.toInt(), (0.96f * dim * 255).toInt()), withAlpha(0xFF291A12.toInt(), (0.98f * dim * 255).toInt())),
                floatArrayOf(0f, 0.18f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(startX - minDim * 0.01f, boardY - minDim * 0.018f, endX + minDim * 0.01f, boardY + minDim * 0.047f, paint)
            paint.shader = null
            paint.color = withAlpha(gold, (0.26f * glowPulse * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.003f
            canvas.drawLine(startX, boardY - minDim * 0.016f, endX, boardY - minDim * 0.016f, paint)
        }

        for (i in 0 until 7) {
            val n = loopedFbm(t, radius = 1.25f, octaves = 2, seedOffset = i * 97 + 5)
            val cycle = ((i * 0.173f + t * 0.20f + 1f) % 1f)
            val x = (0.18f + 0.64f * ((i / 7f + 0.06f * sin(t * 2f * PI.toFloat() + i) + 1f) % 1f)) * w
            val y = (0.12f + 0.76f * cycle) * h
            paint.color = withAlpha(gold, ((0.25f + 0.32f * n) * dim * 255).toInt())
            canvas.drawCircle(x + (n - 0.5f) * minDim * 0.012f, y, minDim * (0.0018f + 0.0024f * n), paint)
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
