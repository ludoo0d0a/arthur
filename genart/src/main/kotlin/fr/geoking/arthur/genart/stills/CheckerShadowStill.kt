package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Bakes one frozen frame of Checker Shadow for Auto/Ambient album art. */
internal object CheckerShadowStill {
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
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = 0xFF14161C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.06f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val shadowCenterX = w * (0.38f + 0.24f * sin(loop * 2f * PI.toFloat()))
        val dim = 0.68f + 0.32f * pulse
        val grid = 10
        val cellW = w / grid
        val cellH = h / grid
        val lightA = 0xFFFAFAFA.toInt()
        val darkB = 0xFF141414.toInt()
        val matchedGray = 0xFF6E6E6E.toInt()
        val shadowRadius = w * 0.34f
        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val x = col * cellW
                val y = row * cellH
                val cx = x + cellW * 0.5f
                val inShadow = abs(cx - shadowCenterX) < shadowRadius
                val shadowT = if (inShadow) {
                    val d = abs(cx - shadowCenterX) / shadowRadius
                    (1f - d * d).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val checkerLight = (row + col) % 2 == 0
                val color = when {
                    inShadow && ((checkerLight && col < grid / 2) || (!checkerLight && col >= grid / 2)) ->
                        matchedGray
                    checkerLight -> mixArgb(lightA, 0xFFE5E7EB.toInt(), 0.08f)
                    else -> mixArgb(darkB, palette.colorAt(3), 0.12f)
                }
                val shaded = if (shadowT > 0.01f && color != matchedGray) {
                    mixArgb(color, 0xFF2A2A2A.toInt(), shadowT * 0.55f)
                } else {
                    color
                }
                paint.color = withAlpha(shaded, 0.95f * dim)
                canvas.drawRect(x, y, x + cellW - 1f, y + cellH - 1f, paint)
            }
        }
        paint.shader = RadialGradient(
            shadowCenterX, h * 0.52f, shadowRadius,
            intArrayOf(
                Color.argb((0.38f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.argb((0.12f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
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