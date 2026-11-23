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

/** Bakes a soft spring wash with sunlit foliage and drifting blossom petals. */
internal object SeasonSpringWashStill {
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
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = 29)
        val dim = 0.85f + 0.15f * pulse

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFE3F0D8.toInt(), 0xFFB8E0C8.toInt(), 0xFF6FAE8A.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Sunlit areas blend softly into the mint canopy.
        for (i in 0..1) {
            val cx = w * if (i == 0) (0.24f + 0.08f * drift) else (0.78f - 0.07f * drift)
            val cy = h * if (i == 0) 0.25f else 0.64f
            val radius = minDim * if (i == 0) 0.48f else 0.55f
            val glow = if (i == 0) 0xFFF2F7C8.toInt() else 0xFFD6F0D8.toInt()
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(withAlpha(glow, (0.38f * dim * 255).toInt()), withAlpha(glow, (0.10f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        // Light stems and broad leaves add a quiet sense of spring growth.
        paint.color = withAlpha(0xFF4D9275.toInt(), (0.40f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.005f
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 5) {
            val x = w * (0.08f + i * 0.22f)
            val sway = sin(t01 * 2f * PI.toFloat() + i * 0.9f) * minDim * 0.025f
            val baseY = h * (0.92f - (i % 2) * 0.08f)
            val tipY = h * (0.52f + (i % 3) * 0.06f)
            canvas.drawLine(x, baseY, x + sway, tipY, paint)
            for (side in listOf(-1f, 1f)) {
                val ly = baseY - (baseY - tipY) * (0.34f + (i % 2) * 0.18f)
                val lx = x + sway * 0.55f + side * minDim * 0.065f
                canvas.save()
                canvas.rotate(side * -22f + 8f * sin(t01 * 2f * PI.toFloat() + i), lx, ly)
                paint.shader = RadialGradient(
                    lx, ly, minDim * 0.075f,
                    intArrayOf(withAlpha(0xFFC6E8B6.toInt(), (0.70f * dim * 255).toInt()), withAlpha(0xFF6FAE8A.toInt(), (0.18f * dim * 255).toInt())),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawOval(lx - minDim * 0.07f, ly - minDim * 0.023f, lx + minDim * 0.07f, ly + minDim * 0.023f, paint)
                paint.shader = null
                canvas.restore()
            }
        }

        // A small set of blush petals drift along closed, slowly curving paths.
        for (i in 0 until 7) {
            val angle = t01 * 2f * PI.toFloat() + i * 1.73f
            val n = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = i * 97 + 5)
            val x0 = 0.10f + i * 0.12f
            val y0 = 0.14f + (i % 5) * 0.16f
            val x = ((x0 + 0.045f * sin(angle) + 1f) % 1f) * w
            val y = ((y0 + 0.10f * t01 + 0.035f * sin(angle * 0.7f) + 1f) % 1f) * h
            val r = minDim * (0.013f + 0.012f * n)
            val color = mixArgb(0xFFFFF4F1.toInt(), palette.colorAt(i), 0.25f)
            canvas.save()
            canvas.rotate(360f * t01 + i * 31f, x, y)
            paint.shader = RadialGradient(
                x, y, r,
                intArrayOf(withAlpha(color, (0.78f * dim * 255).toInt()), withAlpha(0xFFE9AFC0.toInt(), (0.12f * dim * 255).toInt())),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(x - r * 0.55f, y - r, x + r * 0.55f, y + r, paint)
            paint.shader = null
            canvas.restore()
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
