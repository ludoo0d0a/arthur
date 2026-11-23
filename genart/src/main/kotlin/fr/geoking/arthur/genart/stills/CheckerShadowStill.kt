package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Bakes one frozen frame of Checker Shadow for Auto/Ambient album art. */
internal object CheckerShadowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val oval = RectF()

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
        val minDim = min(w, h)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = 0xFF1A1C22.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.06f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val ang = loop * 2f * PI.toFloat()
        val sin01 = (sin(ang) + 1f) * 0.5f
        val dim = 0.68f + 0.32f * pulse
        val grid = 9
        val boardLeft = w * 0.08f
        val boardTop = h * 0.12f
        val boardW = w * 0.84f
        val boardH = h * 0.76f
        val cellW = boardW / grid
        val cellH = boardH / grid
        val matchedGray = 0xFF787878.toInt()
        val lightTile = 0xFFE8E8E8.toInt()
        val darkTile = mixArgb(0xFF404040.toInt(), palette.colorAt(3), 0.1f)
        val cylCx = boardLeft + boardW * (0.72f + 0.04f * (sin01 * 2f - 1f))
        val cylTop = boardTop + boardH * 0.08f
        val cylBottom = boardTop + boardH * 0.92f
        val cylR = minDim * 0.09f
        val shadowCx = cylCx - boardW * 0.22f + boardW * 0.03f * (sin(ang + 0.7f))
        val shadowCy = boardTop + boardH * 0.5f
        val shadowRx = boardW * 0.28f
        val shadowRy = boardH * 0.42f
        val aCol = (grid * 0.28f).toInt().coerceIn(1, grid - 2)
        val aRow = (grid * 0.42f).toInt().coerceIn(1, grid - 2)
        val bCol = (grid * 0.55f).toInt().coerceIn(1, grid - 2)
        val bRow = aRow

        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val x = boardLeft + col * cellW
                val y = boardTop + row * cellH
                val cx = x + cellW * 0.5f
                val cy = y + cellH * 0.5f
                val nx = (cx - shadowCx) / shadowRx
                val ny = (cy - shadowCy) / shadowRy
                val shadowT = exp(-0.5f * (nx * nx + ny * ny)).coerceIn(0f, 1f)
                val isLightChecker = (row + col) % 2 == 0
                val isMatch = (col == aCol && row == aRow) || (col == bCol && row == bRow)
                val base = when {
                    isMatch -> matchedGray
                    isLightChecker -> lightTile
                    else -> darkTile
                }
                val shaded = if (isMatch) matchedGray else mixArgb(base, 0xFF2A2A2A.toInt(), shadowT * 0.55f)
                paint.color = withAlpha(shaded, 0.96f * dim)
                canvas.drawRect(x, y, x + cellW + 0.5f, y + cellH + 0.5f, paint)
            }
        }

        paint.shader = RadialGradient(
            shadowCx, shadowCy, maxOf(shadowRx, shadowRy),
            intArrayOf(
                Color.argb((0.42f * dim * 255).toInt().coerceIn(0, 255), 26, 21, 32),
                Color.argb((0.18f * dim * 255).toInt().coerceIn(0, 255), 26, 21, 32),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + boardH, paint)
        paint.shader = null

        paint.color = withAlpha(matchedGray, 0.96f * dim)
        canvas.drawRect(
            boardLeft + aCol * cellW, boardTop + aRow * cellH,
            boardLeft + aCol * cellW + cellW + 0.5f, boardTop + aRow * cellH + cellH + 0.5f, paint,
        )
        canvas.drawRect(
            boardLeft + bCol * cellW, boardTop + bRow * cellH,
            boardLeft + bCol * cellW + cellW + 0.5f, boardTop + bRow * cellH + cellH + 0.5f, paint,
        )

        val cylLeft = cylCx - cylR
        paint.shader = LinearGradient(
            cylLeft, 0f, cylCx + cylR, 0f,
            intArrayOf(
                withAlpha(0xFF4A5568.toInt(), 0.55f * dim),
                withAlpha(0xFFCBD5E1.toInt(), 0.85f * dim),
                withAlpha(0xFF334155.toInt(), 0.7f * dim),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        oval.set(cylLeft, cylTop, cylCx + cylR, cylTop + cylR * 0.55f)
        canvas.drawOval(oval, paint)
        paint.shader = LinearGradient(
            cylLeft, 0f, cylCx + cylR, 0f,
            intArrayOf(
                withAlpha(0xFF3B4554.toInt(), 0.75f * dim),
                withAlpha(0xFF94A3B8.toInt(), 0.9f * dim),
                withAlpha(0xFF1E293B.toInt(), 0.85f * dim),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(
            cylLeft, cylTop + cylR * 0.28f,
            cylCx + cylR, cylBottom - cylR * 0.27f, paint,
        )
        paint.shader = RadialGradient(
            cylCx - cylR * 0.15f, cylBottom, cylR * 1.6f,
            intArrayOf(
                Color.argb((0.35f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        oval.set(cylCx - cylR * 1.4f, cylBottom - cylR * 0.25f, cylCx + cylR * 1.2f, cylBottom + cylR * 0.3f)
        canvas.drawOval(oval, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun withAlpha(argb: Int, alpha: Float): Int =
        Color.argb(
            (alpha * 255).toInt().coerceIn(0, 255),
            Color.red(argb),
            Color.green(argb),
            Color.blue(argb),
        )

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }
}
