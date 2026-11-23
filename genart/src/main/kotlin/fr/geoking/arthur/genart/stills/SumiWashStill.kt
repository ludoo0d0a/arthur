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
import kotlin.math.sin

/** Bakes one frozen frame of Sumi Wash for Auto/Ambient album art. */
internal object SumiWashStill {
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
        val unit = minOf(w, h)
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val drift = loopedFbm(t, radius = 1.1f, octaves = 2, seedOffset = 61) - 0.5f
        val breathe = 0.94f + 0.06f * sin(t * 2f * PI.toFloat())
        val dim = 0.85f + 0.15f * pulse
        val ink = mixArgb(0xFF17191D.toInt(), palette.colorAt(0), 0.12f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFF5F0E7.toInt(), 0xFFE6DED1.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Diffused pools of ink soften into the warm paper.
        val cx = w * (0.53f + drift * 0.035f)
        val cy = h * 0.48f
        val radius = unit * 0.42f
        paint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(withAlpha(ink, (0.14f * breathe * dim * 255).toInt()), withAlpha(ink, (0.06f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(cx - radius * 0.78f, cy - radius, cx + radius * 0.78f, cy + radius, paint)
        paint.shader = null

        // Broad imperfect brush gestures with a feathery underlayer.
        val sway = drift * unit * 0.06f
        for (i in 0..2) {
            val x = w * listOf(0.22f, 0.55f, 0.84f)[i] + sway * (i - 1)
            val y = h * (0.68f + i * 0.015f)
            val tipX = x + unit * (0.12f + i * 0.025f)
            val tipY = y - unit * listOf(0.60f, 0.75f, 0.50f)[i]
            val path = Path().apply {
                moveTo(x, y)
                cubicTo(x - unit * 0.10f, y - unit * 0.14f, tipX + unit * 0.08f, tipY + unit * 0.18f, tipX, tipY)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(ink, (0.10f * dim * 255).toInt())
            paint.strokeWidth = unit * 0.11f
            canvas.drawPath(path, paint)
            paint.color = withAlpha(ink, (0.30f * dim * 255).toInt())
            paint.strokeWidth = unit * (0.035f + i * 0.006f)
            canvas.drawPath(path, paint)
        }

        // Delicate reed-like branches emerge from the diluted central wash.
        paint.color = withAlpha(ink, (0.13f * dim * 255).toInt())
        paint.strokeWidth = unit * 0.006f
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 5) {
            val side = if (i % 2 == 0) -1f else 1f
            val progress = 0.20f + (i / 2) * 0.17f
            val x = cx + side * unit * (0.06f + progress * 0.08f) + sway * 0.2f
            val y = cy + unit * (0.20f - progress * 0.55f)
            val path = Path().apply {
                moveTo(x, y)
                quadTo(x + side * unit * 0.10f, y - unit * 0.08f, x + side * unit * 0.16f, y - unit * 0.24f)
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
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
