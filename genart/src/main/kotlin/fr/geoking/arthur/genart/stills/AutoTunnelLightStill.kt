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

/** Bakes one frozen frame of Auto Tunnel Light Rhythm for Auto/Ambient album art. */
internal object AutoTunnelLightStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rib = Path()

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
        val drift = loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = 37)
        val dim = 0.85f + 0.15f * pulse
        val cx = w * (0.5f + (drift - 0.5f) * 0.012f)
        val horizonY = h * (0.34f + wash * 0.025f)
        val accent = mixArgb(0xFFFFD080.toInt(), palette.colorAt(0), 0.24f)
        val cool = 0xFF849BB8.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0A0E.toInt(), 0xFF16161C.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            cx, horizonY, minDim * 0.48f,
            intArrayOf(withAlpha(accent, (0.30f * dim * 255).toInt()), withAlpha(cool, (0.10f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, horizonY, minDim * 0.48f, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, horizonY, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(cool, (0.06f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(cx - w * 0.12f, horizonY, cx + w * 0.12f, h, paint)
        paint.shader = null

        // Equally spaced tunnel ribs translate toward the viewer and wrap smoothly.
        val bandCount = 7
        for (i in 0 until bandCount) {
            val depth = ((i.toFloat() / bandCount + t01) % 1f)
            val perspective = depth * depth
            val yTop = horizonY + h * (0.025f + perspective * 0.48f)
            val yBottom = horizonY + h * (0.06f + perspective * 0.70f)
            val halfWidth = w * (0.025f + depth * 0.48f)
            val halfHeight = h * (0.018f + depth * 0.11f)
            val left = cx - halfWidth
            val right = cx + halfWidth
            val top = yTop
            val bottom = yBottom
            val bandColor = accent
            val bandAlpha = (0.12f + 0.36f * depth) * dim

            rib.reset()
            rib.moveTo(left, bottom)
            rib.lineTo(left, top + halfHeight * 0.45f)
            rib.cubicTo(left, top - halfHeight * 0.5f, cx - halfWidth * 0.48f, top - halfHeight, cx, top - halfHeight)
            rib.cubicTo(cx + halfWidth * 0.48f, top - halfHeight, right, top - halfHeight * 0.5f, right, top + halfHeight * 0.45f)
            rib.lineTo(right, bottom)

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = withAlpha(bandColor, (bandAlpha * 0.38f * 255).toInt())
            paint.strokeWidth = minDim * (0.018f - depth * 0.010f).coerceAtLeast(0.004f)
            canvas.drawPath(rib, paint)
            paint.color = withAlpha(bandColor, (bandAlpha * 255).toInt())
            paint.strokeWidth = minDim * (0.0048f - depth * 0.002f).coerceAtLeast(0.0015f)
            canvas.drawPath(rib, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(cool, (0.12f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.006f
        canvas.drawLine(cx, horizonY, -w * 0.12f, h * 1.05f, paint)
        canvas.drawLine(cx, horizonY, w * 1.12f, h * 1.05f, paint)
        paint.color = withAlpha(accent, (0.10f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.003f
        canvas.drawLine(cx, horizonY + h * 0.06f, cx, h * 1.03f, paint)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.76f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
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
