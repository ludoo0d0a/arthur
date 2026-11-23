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

/** Bakes one frozen frame of Micro Dew for Auto/Ambient album art. */
internal object MicroDewStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val leafPath = Path()
    private val veinPath = Path()

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
        val dim = 0.85f + 0.15f * pulse
        val shimmer = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 11)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A2820.toInt(), 0xFF14241B.toInt(), 0xFF0E1814.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Broad macro leaf blade crossing the frame.
        leafPath.reset()
        leafPath.moveTo(-w * 0.12f, h * 0.78f)
        leafPath.cubicTo(w * 0.08f, h * 0.42f, w * 0.56f, h * 0.10f, w * 1.10f, h * 0.22f)
        leafPath.cubicTo(w * 0.88f, h * 0.57f, w * 0.39f, h * 0.91f, -w * 0.12f, h * 0.78f)
        leafPath.close()
        paint.shader = LinearGradient(
            w * 0.12f, h * 0.28f, w * 0.88f, h * 0.80f,
            intArrayOf(0xFF355B3D.toInt(), 0xFF1E3B2B.toInt(), 0xFF102118.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(leafPath, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.008f
        paint.color = withAlpha(0xFF8FC29A.toInt(), (0.34f * dim * 255).toInt())
        veinPath.reset()
        veinPath.moveTo(-w * 0.04f, h * 0.80f)
        veinPath.cubicTo(w * 0.28f, h * 0.62f, w * 0.60f, h * 0.38f, w * 1.02f, h * 0.24f)
        canvas.drawPath(veinPath, paint)
        paint.strokeWidth = minDim * 0.003f
        paint.color = withAlpha(0xFF80AC88.toInt(), (0.18f * dim * 255).toInt())
        for (i in 0 until 7) {
            val v = (i + 1) / 8f
            val x = w * (0.04f + v * 0.88f)
            val y = h * (0.77f - v * 0.48f)
            val side = if (i % 2 == 0) -1f else 1f
            canvas.drawLine(x, y, x + side * w * 0.15f, y - h * 0.12f, paint)
        }
        paint.style = Paint.Style.FILL

        val accent = mixArgb(0xFFA0E8C8.toInt(), palette.colorAt(0), 0.35f)
        paint.shader = RadialGradient(
            w * 0.55f, h * 0.44f, minDim * 0.72f,
            intArrayOf(withAlpha(accent, ((0.10f + shimmer * 0.06f) * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.55f, h * 0.44f, minDim * 0.72f, paint)
        paint.shader = null

        val positions = arrayOf(
            Triple(0.24f, 0.30f, 0.075f), Triple(0.56f, 0.23f, 0.105f),
            Triple(0.77f, 0.38f, 0.064f), Triple(0.40f, 0.54f, 0.052f),
            Triple(0.69f, 0.66f, 0.086f), Triple(0.18f, 0.72f, 0.048f),
            Triple(0.88f, 0.80f, 0.04f),
        )
        positions.forEachIndexed { i, drop ->
            val localShimmer = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = i * 37 + 9)
            val cx = w * drop.first
            val cy = h * drop.second
            val r = minDim * drop.third * (0.98f + localShimmer * 0.04f)

            paint.color = withAlpha(0xFF06110C.toInt(), (0.26f * dim * 255).toInt())
            canvas.drawCircle(cx + r * 0.12f, cy + r * 0.22f, r * 0.96f, paint)

            paint.shader = RadialGradient(
                cx - r * 0.25f, cy - r * 0.28f, r * 1.42f,
                intArrayOf(
                    withAlpha(0xFFE9FFF5.toInt(), (0.18f * dim * 255).toInt()),
                    withAlpha(accent, (0.16f * dim * 255).toInt()),
                    withAlpha(0xFF8BCFB0.toInt(), (0.32f * dim * 255).toInt()),
                    withAlpha(0xFFD9FFF0.toInt(), (0.68f * dim * 255).toInt()),
                ),
                floatArrayOf(0f, 0.48f, 0.82f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            paint.color = withAlpha(0xFFE8FFF5.toInt(), ((0.58f + localShimmer * 0.18f) * dim * 255).toInt())
            canvas.drawCircle(cx - r * 0.34f, cy - r * 0.39f, r * 0.13f, paint)
            paint.color = withAlpha(Color.WHITE, ((0.34f + localShimmer * 0.24f) * dim * 255).toInt())
            canvas.drawCircle(cx - r * 0.08f, cy - r * 0.54f, r * 0.045f, paint)
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
