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
import kotlin.math.abs

/** Frozen POV night ride with a single warm motorcycle headlamp. */
internal object MotoHeadlightStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val beamPath = Path()
    private val roadPath = Path()
    private val fairingPath = Path()

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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 11)
        val sway = (loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 11) - 0.5f) * w * 0.035f
        val flicker = 0.93f + 0.07f * loopedFbm(t01, radius = 1.45f, octaves = 2, seedOffset = 29)
        val dim = 0.85f + 0.15f * pulse
        val gold = mixArgb(0xFFFFF0C0.toInt(), palette.colorAt(0), 0.25f)
        val horizon = h * 0.52f
        val lampX = w * 0.5f + sway
        val lampY = h * 0.79f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(brightness(0xFF080A0D.toInt(), wash), 0xFF141820.toInt(), 0xFF07090C.toInt()),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.5f, horizon, unit * 0.72f,
            intArrayOf(withAlpha(gold, (0.10f * dim * 255).toInt()), withAlpha(0xFF87909A.toInt(), (0.035f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        rect.set(-w * 0.18f, horizon - unit * 0.24f, w * 1.18f, horizon + unit * 0.24f)
        canvas.drawOval(rect, paint)
        paint.shader = null

        beamPath.reset()
        beamPath.moveTo(lampX - unit * 0.06f, lampY)
        beamPath.lineTo(w * 0.18f + sway * 0.22f, horizon)
        beamPath.quadTo(w * 0.5f + sway, horizon - unit * 0.06f, w * 0.82f + sway * 0.22f, horizon)
        beamPath.lineTo(lampX + unit * 0.06f, lampY)
        beamPath.close()
        paint.shader = LinearGradient(
            0f, horizon, 0f, lampY,
            intArrayOf(withAlpha(gold, (0.015f * dim * 255).toInt()), withAlpha(gold, (0.055f * flicker * dim * 255).toInt()), withAlpha(gold, (0.16f * flicker * dim * 255).toInt())),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(beamPath, paint)
        paint.shader = null

        roadPath.reset()
        roadPath.moveTo(w * 0.44f, h)
        roadPath.lineTo(w * 0.47f, horizon)
        roadPath.quadTo(w * 0.5f + sway * 0.12f, horizon - unit * 0.01f, w * 0.53f, horizon)
        roadPath.lineTo(w * 0.70f, h)
        roadPath.close()
        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(0xFF11151A.toInt(), withAlpha(0xFF9A8D69.toInt(), (0.15f * dim * 255).toInt())),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPath(roadPath, paint)
        paint.shader = null

        for (i in 0..3) {
            val progress = (i / 3f + t01) % 1f
            val y = horizon + unit * 0.04f + (h * 0.96f - horizon - unit * 0.04f) * progress * progress
            val dashW = unit * (0.003f + progress * 0.012f)
            paint.color = withAlpha(gold, ((0.16f + 0.20f * progress) * flicker * dim * 255).toInt())
            rect.set(w * 0.5f + sway * 0.12f - dashW / 2f, y, w * 0.5f + sway * 0.12f + dashW / 2f, y + unit * (0.008f + progress * 0.045f))
            canvas.drawRoundRect(rect, dashW, dashW, paint)
        }

        // A few soft suspended particles crossing the beam.
        for (i in 0 until 7) {
            val x0 = ((i * 37 % 97) / 97f)
            val y0 = ((i * 53 % 89) / 89f)
            val n = loopedFbm(t01, radius = 1.1f + i * 0.04f, octaves = 2, seedOffset = i * 53 + 7)
            val x = x0 * w + (n - 0.5f) * unit * 0.07f + sway * 0.2f
            val y = ((y0 - t01 * (0.08f + i * 0.003f) + 1f) % 1f) * h
            val proximity = (1f - abs(x - lampX) / (unit * 0.46f)).coerceIn(0f, 1f)
            if (proximity > 0f) {
                val r = unit * (0.003f + 0.004f * n)
                paint.shader = RadialGradient(
                    x, y, r * 3f,
                    intArrayOf(withAlpha(gold, (0.28f * proximity * dim * 255).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(x, y, r * 3f, paint)
                paint.shader = null
            }
        }

        fairingPath.reset()
        fairingPath.moveTo(w * 0.28f, h)
        fairingPath.quadTo(w * 0.34f, h * 0.84f, lampX, h * 0.86f)
        fairingPath.quadTo(w * 0.66f, h * 0.84f, w * 0.72f, h)
        fairingPath.close()
        paint.shader = LinearGradient(0f, h * 0.84f, 0f, h, intArrayOf(0xFF15191E.toInt(), 0xFF050609.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(fairingPath, paint)
        paint.shader = null

        val lampRadius = unit * 0.075f
        paint.shader = RadialGradient(
            lampX, lampY, lampRadius,
            intArrayOf(withAlpha(0xFFFFF8DE.toInt(), (0.95f * flicker * dim * 255).toInt()), withAlpha(gold, (0.76f * flicker * dim * 255).toInt()), withAlpha(gold, (0.17f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.22f, 0.62f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(lampX, lampY, lampRadius, paint)
        paint.shader = null
        paint.color = withAlpha(gold, (0.48f * dim * 255).toInt())
        rect.set(lampX - unit * 0.038f, lampY - unit * 0.006f, lampX + unit * 0.038f, lampY + unit * 0.006f)
        canvas.drawOval(rect, paint)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }

    private fun brightness(color: Int, wash: Float): Int {
        val amount = 0.92f + 0.08f * wash
        return Color.rgb((Color.red(color) * amount).toInt(), (Color.green(color) * amount).toInt(), (Color.blue(color) * amount).toInt())
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
