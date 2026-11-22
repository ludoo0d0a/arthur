package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Harbor Mist for Auto/Ambient album art. */
internal object HarborMistStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val wash = loopedFbm(t01, radius = 1.4f, octaves = 2, seedOffset = 7)
        val dim = 0.85f + 0.15f * pulse
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF143040.toInt(), 0xFF0A1C28.toInt(), 0xFF040A10.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val accent = mixArgb(0xFFE4F0F6.toInt(), palette.colorAt(1), 0.3f)
        val bandY = h * (0.55f + 0.03f * wash)
        paint.shader = LinearGradient(
            0f, bandY - h * 0.03f, 0f, bandY + h * 0.05f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.14f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, bandY - h * 0.03f, w, bandY + h * 0.05f, paint)
        paint.shader = null

        for (i in 0 until 6) {
            val n = loopedFbm(t01, radius = 1.2f + i * 0.05f, seedOffset = i * 73 + 11)
            val x = ((0.08f + i * 0.14f) + 0.06f * (n * 2f - 1f) + 1f) % 1f * w
            val y = ((0.3f + (i % 4) * 0.12f) + 0.02f * sin(t01 * 2f * PI.toFloat() + i) + 1f) % 1f * h
            val rw = w * (0.22f + 0.04f * n)
            val rh = h * (0.1f + 0.03f * n)
            val radius = maxOf(rw, rh)
            val col = mixArgb(0xFFC8D8E4.toInt(), palette.colorAt(i), 0.25f)
            val alpha = ((0.1f + 0.08f * n) * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(withAlpha(col, alpha), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius.coerceAtLeast(1f), paint)
            paint.shader = null
        }
        paint.alpha = 255
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
