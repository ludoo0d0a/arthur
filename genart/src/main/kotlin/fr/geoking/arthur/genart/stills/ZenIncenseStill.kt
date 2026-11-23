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

/** Bakes one frozen frame of Zen Incense for Auto/Ambient album art. */
internal object ZenIncenseStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val smokePath = Path()

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
        val unit = size.toFloat()
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val flow = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = 19)
        val sway = sin(t01 * 2f * PI.toFloat())
        val dim = 0.88f + 0.12f * pulse
        val accent = mixArgb(0xFFC8B090.toInt(), palette.colorAt(0), 0.2f)
        val smoke = mixArgb(0xFFB8B0A4.toInt(), accent, 0.3f)
        val cx = w * 0.5f
        val stickX = cx - unit * 0.08f
        val stickY = h * 0.73f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A1612.toInt(), 0xFF2E2820.toInt(), 0xFF120F0C.toInt()),
            floatArrayOf(0f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Subtle warm air around the ember.
        paint.shader = RadialGradient(
            stickX, stickY, unit * 0.52f,
            intArrayOf(withAlpha(accent, (0x22 * dim).toInt()), withAlpha(accent, (0x0E * dim).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(stickX, stickY, unit * 0.52f, paint)
        paint.shader = null

        // Low altar ledge, bowl and incense stick.
        paint.color = 0xFF080706.toInt()
        paint.strokeWidth = unit * 0.018f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w * 0.14f, h * 0.91f, w * 0.86f, h * 0.91f, paint)
        paint.color = brightnessArgb(0xFF342D24.toInt(), dim)
        canvas.drawOval(cx - unit * 0.16f, h * 0.865f, cx + unit * 0.16f, h * 0.92f, paint)
        paint.color = 0xFF191612.toInt()
        canvas.drawOval(cx - unit * 0.115f, h * 0.868f, cx + unit * 0.115f, h * 0.902f, paint)

        paint.color = 0xFF8C6848.toInt()
        paint.strokeWidth = unit * 0.012f
        canvas.drawLine(cx + unit * 0.11f, h * 0.87f, stickX, stickY, paint)
        paint.color = 0x44D1A876
        paint.strokeWidth = unit * 0.0025f
        canvas.drawLine(cx + unit * 0.105f, h * 0.86f, stickX, stickY, paint)

        val emberPulse = 0.86f + 0.14f * (0.5f + 0.5f * sway)
        paint.color = withAlpha(0xFFE9A65F.toInt(), (0x48 * emberPulse * dim).toInt())
        canvas.drawCircle(stickX, stickY, unit * 0.027f, paint)
        paint.shader = RadialGradient(
            stickX, stickY, unit * 0.012f,
            intArrayOf(0xFFFFE2A1.toInt(), 0xFFEAA15A.toInt(), Color.TRANSPARENT),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(stickX, stickY, unit * 0.012f, paint)
        paint.shader = null

        // Curled smoke strands, layered with a broad haze and fine center line.
        for (strand in 0..2) {
            val offset = (strand - 1) * unit * 0.018f
            smokePath.reset()
            smokePath.moveTo(stickX + offset * 0.25f, stickY - unit * 0.012f)
            smokePath.cubicTo(
                stickX - unit * (0.015f + 0.025f * sway) + offset,
                stickY - unit * 0.08f,
                stickX + unit * (0.04f + 0.02f * sway) + offset,
                stickY - unit * 0.15f,
                stickX - unit * (0.01f - 0.018f * sway) + offset,
                stickY - unit * 0.22f,
            )
            smokePath.cubicTo(
                stickX - unit * (0.06f - 0.025f * flow) + offset,
                stickY - unit * 0.31f,
                stickX + unit * (0.045f + 0.025f * flow) + offset,
                stickY - unit * 0.37f,
                stickX + unit * (0.015f + 0.035f * sway) + offset,
                stickY - unit * (0.46f + 0.025f * flow),
            )
            val alpha = (0.13f + strand * 0.035f) * dim
            paint.color = withAlpha(smoke, (alpha * 0.25f * 255).toInt())
            paint.strokeWidth = unit * (0.026f - strand * 0.004f)
            paint.strokeCap = Paint.Cap.ROUND
            paint.style = Paint.Style.STROKE
            canvas.drawPath(smokePath, paint)
            paint.color = withAlpha(smoke, (alpha * 255).toInt())
            paint.strokeWidth = unit * (0.007f - strand * 0.001f)
            canvas.drawPath(smokePath, paint)
        }
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

    private fun brightnessArgb(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(argb), r, g, b)
    }
}
