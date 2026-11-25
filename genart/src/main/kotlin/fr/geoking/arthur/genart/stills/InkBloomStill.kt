package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Ink Bloom for Auto/Ambient album art. */
internal object InkBloomStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
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
        val unit = minOf(w, h)
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val drift = loopedFbm(t, radius = 1.25f, octaves = 3, seedOffset = 11)
        val bloom = loopedFbm(t, radius = 1.1f, octaves = 2, seedOffset = 29)
        val dim = 0.85f + 0.15f * pulse
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFF4F0E8.toInt(), 0xFFE0D8C8.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cx = w * (0.50f + 0.04f * (drift - 0.5f))
        val cy = h * (0.51f + 0.06f * (bloom - 0.5f))
        val ink = mixArgb(0xFF1A1A22.toInt(), palette.colorAt(0), 0.12f)
        val haloRadius = unit * (0.56f + 0.04f * bloom)
        paint.shader = RadialGradient(
            cx, cy, haloRadius,
            intArrayOf(withAlpha(0xFF716C70.toInt(), (0.16f * dim * 255).toInt()), withAlpha(0xFF8B8580.toInt(), (0.11f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, haloRadius, paint)
        paint.shader = null

        val coreRadius = unit * (0.24f + 0.04f * bloom)
        paint.shader = RadialGradient(
            cx, cy, coreRadius,
            intArrayOf(
                withAlpha(ink, (0.68f * dim * 255).toInt()),
                withAlpha(ink, (0.45f * dim * 255).toInt()),
                withAlpha(0xFF3D3940.toInt(), (0.20f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.42f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, coreRadius, paint)
        paint.shader = null

        val branchCount = 7
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until branchCount) {
            val n = loopedFbm(t, radius = 1.2f + i * 0.025f, octaves = 2, seedOffset = i * 47 + 3)
            val sway = sin(t * 2f * PI.toFloat() + i * 0.83f)
            val startX = 0.32f + seededUnit(i * 19 + 5) * 0.36f
            val startY = 0.38f + seededUnit(i * 29 + 9) * 0.34f
            val reach = unit * (0.22f + seededUnit(i * 37 + 13) * 0.24f) * (0.86f + 0.18f * n)
            val bend = -0.16f + seededUnit(i * 43 + 17) * 0.32f
            val sign = if (i % 2 == 0) 1f else -1f
            val x0 = w * (startX + 0.05f * (drift - 0.5f))
            val y0 = h * (startY + 0.04f * (bloom - 0.5f))
            val tipX = x0 + unit * (bend + 0.025f * sway)
            val tipY = y0 + sign * reach
            path.reset()
            path.moveTo(x0, y0)
            path.cubicTo(
                x0 + unit * (bend * 0.25f + 0.035f * sway), y0 + sign * reach * 0.28f,
                tipX - unit * 0.025f * sway, y0 + sign * reach * 0.72f,
                tipX, tipY,
            )
            paint.strokeWidth = unit * (0.007f + seededUnit(i * 53 + 21) * 0.008f) * 3.6f
            paint.color = withAlpha(ink, ((0.18f + 0.14f * n) * dim * 255).toInt())
            canvas.drawPath(path, paint)
            paint.strokeWidth = unit * (0.007f + seededUnit(i * 53 + 21) * 0.008f)
            paint.color = withAlpha(ink, ((0.36f + 0.28f * n) * dim * 255).toInt())
            canvas.drawPath(path, paint)

            for (side in -1..1 step 2) {
                path.reset()
                path.moveTo(x0 + (tipX - x0) * 0.42f, y0 + sign * reach * 0.42f)
                path.cubicTo(
                    x0 + unit * (bend + side * 0.035f), y0 + sign * reach * 0.50f,
                    tipX + unit * side * 0.045f, tipY - sign * reach * 0.10f,
                    tipX + unit * side * (0.055f + 0.015f * sway), tipY - sign * reach * 0.18f,
                )
                paint.strokeWidth = unit * (0.007f + seededUnit(i * 53 + 21) * 0.008f) * 0.52f
                paint.color = withAlpha(ink, (0.32f * dim * 255).toInt())
                canvas.drawPath(path, paint)
            }
        }

        paint.style = Paint.Style.FILL
        for (i in 0 until 7) {
            val x = w * (0.22f + seededUnit(i * 83 + 7) * 0.56f) + unit * 0.018f * sin(t * 2f * PI.toFloat() + i)
            val y = h * (0.22f + seededUnit(i * 97 + 31) * 0.56f) + unit * 0.015f * sin(t * 2f * PI.toFloat() + i * 1.7f)
            val radius = unit * (0.006f + 0.012f * seededUnit(i * 109 + 41))
            paint.color = withAlpha(ink, (0.16f * dim * 255).toInt())
            canvas.drawCircle(x, y, radius * 2.8f, paint)
            paint.color = withAlpha(ink, (0.45f * dim * 255).toInt())
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
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
