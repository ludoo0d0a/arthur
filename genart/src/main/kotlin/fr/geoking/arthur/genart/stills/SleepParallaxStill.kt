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

/** Bakes one frozen frame of Sleep Parallax for Auto/Ambient album art. */
internal object SleepParallaxStill {
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
        val dim = 0.85f + 0.15f * pulse
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0E1020.toInt(), 0xFF1A1830.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        val accent = mixArgb(0xFFA090C8.toInt(), palette.colorAt(0), 0.25f)
        val minDim = minOf(w, h)
        val driftFar = loopedFbm(t01, radius = 0.95f, octaves = 2, seedOffset = 61) - 0.5f
        val driftNear = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 89) - 0.5f
        val cx = w * (0.5f + 0.12f * driftFar)
        val cy = h * (0.28f + 0.025f * (loopedFbm(t01, radius = 1.4f, seedOffset = 47) - 0.5f))
        val orbRadius = minDim * 0.075f
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.43f,
            intArrayOf(withAlpha(accent, (0.15f * dim * 255).toInt()), withAlpha(accent, (0.08f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.43f, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            cx, cy, orbRadius,
            intArrayOf(0x75E0D8FF, withAlpha(accent, (0.20f * dim * 255).toInt()), withAlpha(accent, (0.02f * dim * 255).toInt())),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, orbRadius, paint)
        paint.shader = null

        fun ridgePath(baseY: Float, amplitude: Float, offset: Float): Path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, baseY)
            for (step in 0..12) {
                val x = w * step / 12f
                val envelope = sin((step / 12f + offset + 1f) * PI.toFloat())
                val ripple = loopedFbm(t01, radius = 1.05f + step * 0.025f, octaves = 2, seedOffset = 120 + step * 13) - 0.5f
                lineTo(x, baseY + envelope * amplitude * 0.38f + ripple * amplitude * 0.32f)
            }
            lineTo(w, h)
            close()
        }
        paint.shader = LinearGradient(
            0f, h * 0.58f, 0f, h,
            intArrayOf(withAlpha(0xFF3B3D68.toInt(), (0.42f * dim * 255).toInt()), withAlpha(0xFF22233F.toInt(), (0.70f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(ridgePath(h * 0.64f, h * 0.075f, driftFar * 0.18f), paint)
        paint.shader = LinearGradient(
            0f, h * 0.68f, 0f, h,
            intArrayOf(withAlpha(0xFF292B4C.toInt(), (0.82f * dim * 255).toInt()), withAlpha(0xFF17182B.toInt(), (0.96f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(ridgePath(h * 0.74f, h * 0.09f, driftNear * 0.3f + 0.36f), paint)
        paint.shader = null
        paint.color = withAlpha(0xFF101222.toInt(), (0.96f * dim * 255).toInt())
        canvas.drawPath(ridgePath(h * 0.86f, h * 0.045f, driftNear * 0.52f + 0.71f), paint)
        paint.shader = LinearGradient(
            0f, h * 0.58f, 0f, h * 0.78f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.08f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.58f, w, h * 0.78f, paint)
        paint.shader = null

        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.25f + i * 0.04f, seedOffset = i * 97 + 5)
            val depth = 0.35f + (i % 4) * 0.18f
            val x = ((0.1f + i * 0.12f) + 0.04f * (n * 2f - 1f) + 1f) % 1f * w
            val y = ((0.18f + (i % 5) * 0.095f) + 0.025f * sin(t01 * 2f * PI.toFloat() + i) + 1f) % 1f * h
            val r = minDim * (0.006f + 0.009f * depth) * (0.8f + 0.35f * n)
            val moteColor = withAlpha(mixArgb(palette.colorAt(i), accent, 0.25f), ((0.22f + 0.22f * n) * dim * 255).toInt())
            paint.shader = RadialGradient(
                x, y, r * 3.5f,
                intArrayOf(withAlpha(moteColor, 204), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 3.5f, paint)
            paint.shader = null
            paint.color = moteColor
            canvas.drawCircle(x, y, r, paint)
        }
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
