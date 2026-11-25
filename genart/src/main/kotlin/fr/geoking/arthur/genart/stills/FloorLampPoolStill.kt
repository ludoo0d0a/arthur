package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.RectF
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Floor Lamp Pool for Auto/Ambient album art. */
internal object FloorLampPoolStill {
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
        val m = size.toFloat()
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val flicker = 0.88f + 0.12f * loopedFbm(t, radius = 1.25f, octaves = 2, seedOffset = 17)
        val dim = 0.85f + 0.15f * pulse
        val accent = mixArgb(0xFFFFD8A0.toInt(), palette.colorAt(0), 0.18f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0E1014.toInt(), 0xFF171A20.toInt(), 0xFF101116.toInt()),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val floorY = h * 0.78f
        paint.shader = LinearGradient(0f, floorY, 0f, h, 0xFF15161A.toInt(), 0xFF090A0D.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, floorY, w, h, paint)
        paint.shader = null
        paint.color = withAlpha(0xFF38312A.toInt(), (0.42f * dim * 255).toInt())
        paint.strokeWidth = m * 0.002f
        canvas.drawLine(0f, floorY, w, floorY, paint)

        val cx = w * 0.58f
        val cy = h * 0.805f
        paint.shader = RadialGradient(
            cx, cy, m * 0.66f,
            intArrayOf(
                withAlpha(accent, (0.42f * flicker * dim * 255).toInt()),
                withAlpha(accent, (0.26f * flicker * dim * 255).toInt()),
                withAlpha(accent, (0.10f * flicker * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        rect.set(w * 0.10f, h * 0.60f, w * 1.04f, h * 1.10f)
        canvas.drawOval(rect, paint)
        paint.shader = null

        val lampX = w * 0.66f
        path.reset()
        path.moveTo(lampX - m * 0.11f, h * 0.34f)
        path.lineTo(w * 0.22f, floorY)
        path.lineTo(w * 0.98f, floorY)
        path.lineTo(lampX + m * 0.11f, h * 0.34f)
        path.close()
        paint.shader = LinearGradient(
            0f, h * 0.34f, 0f, floorY,
            intArrayOf(withAlpha(accent, (0.16f * flicker * dim * 255).toInt()), withAlpha(accent, (0.025f * flicker * dim * 255).toInt())),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.color = 0xFF29241F.toInt()
        paint.strokeWidth = m * 0.022f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(lampX, h * 0.35f, lampX, floorY, paint)
        paint.color = withAlpha(0xFF68523B.toInt(), 130)
        paint.strokeWidth = m * 0.0025f
        canvas.drawLine(lampX - m * 0.005f, h * 0.36f, lampX - m * 0.005f, floorY * 0.98f, paint)
        paint.color = 0xFF211D19.toInt()
        rect.set(lampX - m * 0.13f, floorY - m * 0.018f, lampX + m * 0.13f, floorY + m * 0.024f)
        canvas.drawOval(rect, paint)

        path.reset()
        path.moveTo(lampX - m * 0.12f, h * 0.21f)
        path.quadTo(lampX, h * 0.19f, lampX + m * 0.12f, h * 0.21f)
        path.lineTo(lampX + m * 0.19f, h * 0.35f)
        path.quadTo(lampX, h * 0.37f, lampX - m * 0.19f, h * 0.35f)
        path.close()
        paint.shader = LinearGradient(0f, h * 0.19f, 0f, h * 0.37f, 0xFF6B5640.toInt(), 0xFF2E261F.toInt(), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null
        paint.color = withAlpha(0xFFB2956A.toInt(), (0.45f * dim * 255).toInt())
        paint.strokeWidth = m * 0.007f
        canvas.drawLine(lampX - m * 0.19f, h * 0.35f, lampX + m * 0.19f, h * 0.35f, paint)

        paint.shader = RadialGradient(
            lampX, h * 0.36f, m * 0.30f,
            intArrayOf(
                withAlpha(accent, (0.82f * flicker * dim * 255).toInt()),
                withAlpha(accent, (0.28f * flicker * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(lampX - m * 0.30f, h * 0.28f, lampX + m * 0.30f, h * 0.50f, paint)
        paint.shader = null

        for (i in 0 until 7) {
            val x = w * (0.18f + (i * 0.137f) % 0.72f)
            val y = h * (0.72f + ((i * 0.061f + t * 0.14f) % 0.20f))
            val proximity = (1f - kotlin.math.abs(x - lampX) / (m * 0.50f)).coerceIn(0f, 1f)
            paint.color = withAlpha(accent, ((0.12f + 0.30f * proximity) * dim * 255).toInt())
            canvas.drawCircle(x + sin(t * 2f * PI.toFloat() + i) * m * 0.012f, y, m * 0.0038f, paint)
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, blue)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}