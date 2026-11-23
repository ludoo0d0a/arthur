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
import kotlin.math.PI

/** Bakes one frozen frame of Train Soft Catenary for Auto/Ambient album art. */
internal object TrainCatenaryStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val sway = (loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 29) - 0.5f) * 0.012f
        val horizon = h * (0.49f + 0.025f * (wash - 0.5f))
        val dim = 0.85f + 0.15f * pulse
        val steel = mixArgb(0xFFC0C8D0.toInt(), palette.colorAt(0), 0.12f)
        val silhouette = 0xFF080A0C.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF12161C.toInt(), 0xFF1C222C.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Diffuse dusk haze around the distant vanishing point.
        paint.shader = RadialGradient(
            w * (0.5f + sway), horizon, minDim * 0.63f,
            intArrayOf(withAlpha(steel, (0.17f * dim * 255).toInt()), withAlpha(0xFF8996A8.toInt(), (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * (0.5f + sway), horizon, minDim * 0.63f, paint)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.11f, 0f, horizon + h * 0.11f,
            intArrayOf(Color.TRANSPARENT, withAlpha(steel, (0.06f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.11f, w, horizon + h * 0.11f, paint)
        paint.shader = null

        // Thin cloud bands soften the otherwise cool sky.
        for (i in 0 until 4) {
            val y = horizon - h * (0.05f + i * 0.045f)
            val offset = (loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 51 + i * 13) - 0.5f) * w * 0.07f
            paint.shader = LinearGradient(
                -w * 0.12f + offset, y, w * 1.12f + offset, y,
                intArrayOf(Color.TRANSPARENT, withAlpha(steel, (0.04f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(-w * 0.12f + offset, y, w * 1.12f + offset, y + h * (0.035f + i * 0.003f), paint)
        }
        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(withAlpha(silhouette, (0.16f * dim * 255).toInt()), withAlpha(silhouette, (0.78f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon, w, h, paint)
        paint.shader = null

        val cx = w * (0.5f + sway)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(steel, (0.17f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.004f
        for (side in listOf(-1f, 1f)) {
            val rail = Path().apply {
                moveTo(cx + side * w * 0.006f, horizon + h * 0.005f)
                cubicTo(
                    cx + side * w * 0.025f, horizon + h * 0.15f,
                    cx + side * w * 0.12f, h * 0.78f,
                    cx + side * w * 0.22f, h * 1.04f,
                )
            }
            canvas.drawPath(rail, paint)
        }

        for (side in listOf(-1f, 1f)) {
            val conductor = Path().apply {
                moveTo(cx + side * w * 0.43f, h * 0.035f)
                cubicTo(
                    cx + side * w * 0.22f, h * 0.12f,
                    cx + side * w * 0.075f, horizon - h * 0.04f,
                    cx + side * w * 0.012f, horizon,
                )
            }
            paint.color = withAlpha(steel, (0.14f * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.0018f
            canvas.drawPath(conductor, paint)
        }

        val poleCount = 7
        val phaseOffset = t01 * poleCount
        for (index in poleCount - 1 downTo 0) {
            val u = ((index + phaseOffset) % poleCount) / poleCount
            val depth = u * u
            val y = horizon + (h - horizon) * (0.08f + 0.90f * depth)
            val scale = 0.16f + 0.84f * depth
            val halfSpan = minDim * (0.045f + 0.055f * depth)
            val topY = horizon - h * (0.18f - 0.045f * depth)
            val mastColor = withAlpha(silhouette, ((0.68f + 0.2f * depth) * dim * 255).toInt())
            val wireColor = withAlpha(steel, ((0.19f + 0.12f * (1f - depth)) * dim * 255).toInt())
            val poleStroke = minDim * (0.0022f + 0.004f * depth)
            val leftX = cx - w * (0.035f + 0.19f * depth)
            val rightX = cx + w * (0.035f + 0.19f * depth)
            val crossY = topY + h * 0.035f * scale

            paint.color = mastColor
            paint.strokeWidth = poleStroke
            canvas.drawLine(leftX, topY, leftX - halfSpan * 0.18f, y, paint)
            canvas.drawLine(rightX, topY, rightX + halfSpan * 0.18f, y, paint)
            canvas.drawLine(leftX - halfSpan, crossY, rightX + halfSpan, crossY, paint)
            paint.strokeWidth = poleStroke * 0.65f
            canvas.drawLine(leftX, topY + h * 0.035f * scale, leftX - halfSpan * 0.72f, crossY, paint)
            canvas.drawLine(rightX, topY + h * 0.035f * scale, rightX + halfSpan * 0.72f, crossY, paint)

            val wireY = crossY + h * 0.012f * scale
            paint.strokeWidth = minDim * (0.0025f + 0.003f * depth)
            canvas.drawLine(cx - halfSpan * 0.72f, crossY, cx - halfSpan * 0.72f, wireY, paint)
            canvas.drawLine(cx + halfSpan * 0.72f, crossY, cx + halfSpan * 0.72f, wireY, paint)

            val contact = Path().apply {
                moveTo(cx - halfSpan * 0.72f, wireY)
                quadTo(cx, wireY + h * 0.014f * scale, cx + halfSpan * 0.72f, wireY)
            }
            paint.color = wireColor
            paint.strokeWidth = minDim * (0.0012f + 0.001f * depth)
            canvas.drawPath(contact, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, wireY, minDim * (0.0017f + 0.0015f * depth), paint)
            paint.style = Paint.Style.STROKE

            for (wireOffset in listOf(-0.35f, 0.35f)) {
                val feeder = Path().apply {
                    moveTo(cx + wireOffset * halfSpan, topY - h * 0.025f * scale)
                    quadTo(cx + wireOffset * halfSpan * 0.35f, topY - h * 0.02f * scale, cx, horizon - h * 0.005f)
                }
                paint.color = withAlpha(steel, (0.10f * dim * 255).toInt())
                paint.strokeWidth = minDim * 0.0012f
                canvas.drawPath(feeder, paint)
            }
        }
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(withAlpha(silhouette, (0.42f * dim * 255).toInt()), Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(silhouette, (0.42f * dim * 255).toInt())),
            floatArrayOf(0f, 0.08f, 0.92f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.48f, minDim * 0.78f,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(silhouette, (0.46f * dim * 255).toInt())),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
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
