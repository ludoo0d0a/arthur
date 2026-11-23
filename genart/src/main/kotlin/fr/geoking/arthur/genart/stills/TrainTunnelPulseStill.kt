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
import kotlin.math.cos

/** Bakes one frozen frame of Train Tunnel Pulse for Auto/Ambient album art. */
internal object TrainTunnelPulseStill {
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
        val minDim = minOf(w, h)
        val cx = w * 0.5f
        val vanishingY = h * 0.39f
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 29)
        val dim = 0.82f + 0.18f * pulse
        val accent = mixArgb(0xFFE0E8F0.toInt(), palette.colorAt(0), 0.28f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080A0E.toInt(), 0xFF14181E.toInt(), 0xFF07090C.toInt()),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            cx, vanishingY, minDim * 0.7f,
            intArrayOf(withAlpha(accent, (0.15f * dim * 255).toInt()), withAlpha(0xFF8593A3.toInt(), (0.05f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, vanishingY, minDim * 0.7f, paint)
        paint.shader = null

        // Receding floor and rail bed.
        path.reset()
        path.moveTo(cx - w * 0.035f, vanishingY)
        path.lineTo(cx + w * 0.035f, vanishingY)
        path.lineTo(w * 0.92f, h)
        path.lineTo(w * 0.08f, h)
        path.close()
        paint.shader = LinearGradient(
            0f, vanishingY, 0f, h,
            intArrayOf(0xFF10141A.toInt(), 0xFF050609.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // Tunnel arch ribs.
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0..5) {
            val depth = 0.12f + i * 0.16f
            val halfWidth = w * (0.025f + 0.48f * depth)
            val baseY = vanishingY + (h - vanishingY) * depth * 0.86f
            val crownY = vanishingY - h * (0.015f + 0.34f * depth)
            path.reset()
            path.moveTo(cx - halfWidth, baseY)
            path.cubicTo(cx - halfWidth, crownY + h * 0.03f, cx - halfWidth * 0.46f, crownY, cx, crownY)
            path.cubicTo(cx + halfWidth * 0.46f, crownY, cx + halfWidth, crownY + h * 0.03f, cx + halfWidth, baseY)
            paint.color = withAlpha(0xFF7C8794.toInt(), ((0.11f + 0.07f * depth) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.003f + 0.003f * depth)
            canvas.drawPath(path, paint)
        }

        // Rails and passing sleepers.
        paint.color = withAlpha(accent, (0.14f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.004f
        canvas.drawLine(cx - w * 0.018f, vanishingY, cx - w * 0.30f, h, paint)
        canvas.drawLine(cx + w * 0.018f, vanishingY, cx + w * 0.30f, h, paint)
        for (i in 0..9) {
            val depth = ((i / 10f + t01) % 1f)
            val y = vanishingY + (h - vanishingY) * depth * depth
            val half = w * (0.025f + 0.27f * depth * depth)
            paint.color = withAlpha(0xFF9AA3AD.toInt(), ((0.04f + 0.08f * depth) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.002f + 0.004f * depth)
            canvas.drawLine(cx - half, y, cx + half, y, paint)
        }

        // Paired, softly blooming lamps stream through the tunnel.
        paint.style = Paint.Style.FILL
        for (i in 0 until 7) {
            val z = ((i / 7f + t01) % 1f)
            val depth = 0.06f + 0.88f * z * z
            val y = vanishingY + (h - vanishingY) * depth * 0.82f
            val xOffset = w * (0.025f + 0.46f * depth)
            val radius = minDim * (0.003f + 0.013f * depth)
            val flicker = 0.78f + 0.22f * (0.5f + 0.5f * cos(2f * PI.toFloat() * t01 + i * 0.8f))
            for (side in -1..1 step 2) {
                val x = cx + side * xOffset
                val lightY = y - minDim * (0.025f + 0.015f * depth)
                paint.shader = RadialGradient(
                    x, lightY, radius * 5f,
                    intArrayOf(withAlpha(accent, (0.23f * flicker * dim * 255).toInt()), withAlpha(accent, (0.10f * flicker * dim * 255).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.32f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(x, lightY, radius * 5f, paint)
                paint.shader = null
                paint.color = withAlpha(accent, (0.32f * flicker * dim * 255).toInt())
                canvas.drawOval(x - radius, lightY - radius * 0.45f, x + radius, lightY + radius * 0.45f, paint)
            }
        }

        // Gentle edge falloff.
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(withAlpha(0xFF050609.toInt(), (0.48f * dim * 255).toInt()), Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(0xFF050609.toInt(), (0.48f * dim * 255).toInt())),
            floatArrayOf(0f, 0.22f, 0.78f, 1f),
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
