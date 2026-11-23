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
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Frozen aerial view above a sleeping city for Auto/Ambient album art. */
internal object AvionNightCityStill {
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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t, radius = 1.25f, octaves = 3, seedOffset = 29)
        val breathing = loopedFbm(t, radius = 1.6f, octaves = 2, seedOffset = 53)
        val dim = 0.85f + 0.15f * pulse
        val horizon = h * (0.40f + 0.018f * (wash - 0.5f))

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF060810.toInt(), 0xFF101828.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.44f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val accent = mixArgb(0xFFFFE080.toInt(), palette.colorAt(0), 0.16f)
        paint.shader = LinearGradient(
            0f, horizon - h * 0.12f, 0f, horizon + h * 0.24f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.14f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.12f, w, horizon + h * 0.24f, paint)
        paint.shader = null

        // Distant low city silhouette.
        val skylineY = horizon + h * 0.035f
        for (i in 0..20) {
            val x = w * (i / 21f)
            val bw = w * (0.012f + seededUnit(i * 37 + 1) * 0.026f)
            val bh = h * (0.018f + seededUnit(i * 47 + 9) * 0.085f)
            paint.color = withAlpha(0xFF090E18.toInt(), (0.54f * dim * 255).toInt())
            canvas.drawRect(x, skylineY - bh, x + bw, skylineY, paint)
        }

        // City avenues recede toward the horizon in a broad perspective grid.
        for (i in -7..7) {
            val bottomX = w * (0.5f + i * 0.115f)
            val topX = w * (0.5f + i * 0.018f)
            val alpha = (0.035f + 0.035f * (1f - kotlin.math.abs(i) / 8f)) * dim
            paint.color = withAlpha(0xFFFFC86A.toInt(), (alpha * 255).toInt())
            paint.strokeWidth = minDim * 0.0018f
            path.reset()
            path.moveTo(w * 0.5f + (topX - w * 0.5f) * 0.28f, horizon + h * 0.025f)
            path.lineTo(bottomX, h * 1.06f)
            canvas.drawPath(path, paint)
        }
        for (row in 0..10) {
            val u = row / 10f
            val y = horizon + h * 0.025f + (h * 1.035f - (horizon + h * 0.025f)) * u * u
            paint.color = withAlpha(0xFFFFD98C.toInt(), ((0.028f + 0.025f * u) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.001f + 0.0014f * u)
            canvas.drawLine(0f, y, w, y, paint)
        }

        for (i in 0 until 112) {
            val row = seededUnit(i * 31 + 13)
            val x0 = seededUnit(i * 17 + 3)
            val size0 = seededRange(i * 41 + 11, 0.0015f, 0.005f)
            val warmth = seededRange(i * 53 + 19, 0.35f, 1f)
            val phaseOffset = seededRange(i * 67 + 23, 0f, 2f * PI.toFloat())
            val n = loopedFbm(t, radius = 1.35f + size0, octaves = 2, seedOffset = i * 97 + 5)
            val x = ((x0 + sin(t * 2f * PI.toFloat() + phaseOffset) * 0.0022f + 1f) % 1f) * w
            val y = horizon + h * (0.035f + row * 0.94f)
            val r = (minDim * size0 * (0.3f + row * 0.9f) * (0.8f + 0.28f * n)).coerceAtLeast(0.8f)
            val warm = mixArgb(0xFFFFE080.toInt(), 0xFFFFA94D.toInt(), 1f - warmth)
            val glowAlpha = ((0.20f + 0.18f * n) * dim * 255).toInt()
            paint.shader = RadialGradient(
                x, y, r * 5.5f,
                intArrayOf(withAlpha(warm, glowAlpha), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r * 5.5f, paint)
            paint.shader = null
            paint.color = withAlpha(warm, ((0.24f + 0.48f * n) * dim * 255).toInt())
            canvas.drawCircle(x, y, r, paint)
        }

        for (i in 0..3) {
            val cx = w * (0.16f + i * 0.22f) + (wash - 0.5f) * w * 0.015f
            val cy = horizon + h * (0.24f + (i % 2) * 0.18f)
            val radius = minDim * (0.14f + 0.02f * breathing)
            val alpha = ((0.045f + 0.025f * breathing) * dim * 255).toInt()
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(withAlpha(0xFFFFC66A.toInt(), alpha), withAlpha(0xFFFFC66A.toInt(), alpha / 3), Color.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        paint.shader = LinearGradient(
            0f, h * 0.91f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.45f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.91f, w, h, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt().coerceIn(0, 255),
        )
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
