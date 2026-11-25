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
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI

/** Bakes one frozen frame of Micro Dew for Auto/Ambient album art. */
internal object MicroDewStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val leafPath = Path()
    private val veinPath = Path()

    private val dropPositions = arrayOf(
        Triple(0.24f, 0.30f, 0.075f), Triple(0.56f, 0.23f, 0.105f),
        Triple(0.77f, 0.38f, 0.064f), Triple(0.40f, 0.54f, 0.052f),
        Triple(0.69f, 0.66f, 0.086f), Triple(0.18f, 0.72f, 0.048f),
        Triple(0.88f, 0.80f, 0.04f), Triple(0.51f, 0.84f, 0.057f),
        Triple(0.34f, 0.41f, 0.032f), Triple(0.84f, 0.56f, 0.037f),
        Triple(0.12f, 0.48f, 0.03f),
    )

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        if (size <= 0) return

        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h).coerceAtLeast(1f)
        val t01 = phase01(phase / (2f * PI.toFloat()))
        val dim = 0.85f + 0.15f * pulse.coerceIn(0f, 1f)
        val pulseWobble = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 11) * 2f - 1f
        val pulseNorm = pulseWobble * 0.5f + 0.5f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A2820.toInt(), 0xFF14241B.toInt(), 0xFF0E1814.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

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
        paint.strokeWidth = (minDim * 0.008f).coerceAtLeast(0.5f)
        paint.color = withAlpha(0xFF8FC29A.toInt(), (0.34f * dim * 255).toInt().coerceIn(0, 255))
        veinPath.reset()
        veinPath.moveTo(-w * 0.04f, h * 0.80f)
        veinPath.cubicTo(w * 0.28f, h * 0.62f, w * 0.60f, h * 0.38f, w * 1.02f, h * 0.24f)
        canvas.drawPath(veinPath, paint)
        paint.strokeWidth = (minDim * 0.003f).coerceAtLeast(0.4f)
        paint.color = withAlpha(0xFF80AC88.toInt(), (0.18f * dim * 255).toInt().coerceIn(0, 255))
        for (i in 0 until 7) {
            val v = (i + 1) / 8f
            val x = w * (0.04f + v * 0.88f)
            val y = h * (0.77f - v * 0.48f)
            val side = if (i % 2 == 0) -1f else 1f
            canvas.drawLine(x, y, x + side * w * 0.15f, y - h * 0.12f, paint)
        }
        paint.style = Paint.Style.FILL

        val accent = mixArgb(0xFFA0E8C8.toInt(), palette.colorAt(0), 0.35f)
        val bounceX = w * (0.55f + 0.04f * pulseWobble)
        val bounceY = h * (0.44f + 0.02f * pulseWobble)
        val bounceRadius = (minDim * 0.72f).coerceAtLeast(1f)
        paint.shader = RadialGradient(
            bounceX, bounceY, bounceRadius,
            intArrayOf(withAlpha(accent, ((0.12f + 0.08f * pulseNorm) * dim * 255).toInt().coerceIn(0, 255)), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(bounceX, bounceY, bounceRadius, paint)
        paint.shader = null

        dropPositions.forEachIndexed { i, drop ->
            val localWobble = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = i * 37 + 9) * 2f - 1f
            val cx = w * drop.first
            val cy = h * drop.second
            val r = (minDim * drop.third * (1f + localWobble * 0.06f)).coerceAtLeast(1f)

            paint.shader = null
            paint.color = withAlpha(0xFF06110C.toInt(), (0.26f * dim * 255).toInt().coerceIn(0, 255))
            canvas.drawCircle(cx + r * 0.12f, cy + r * 0.22f, (r * 0.96f).coerceAtLeast(1f), paint)

            val shaderRadius = (r * 1.42f).coerceAtLeast(1f)
            paint.shader = RadialGradient(
                cx - r * 0.25f, cy - r * 0.28f, shaderRadius,
                intArrayOf(
                    withAlpha(0xFFE9FFF5.toInt(), (0.18f * dim * 255).toInt().coerceIn(0, 255)),
                    withAlpha(accent, (0.16f * dim * 255).toInt().coerceIn(0, 255)),
                    withAlpha(0xFF8BCFB0.toInt(), (0.32f * dim * 255).toInt().coerceIn(0, 255)),
                    withAlpha(0xFFD9FFF0.toInt(), (0.68f * dim * 255).toInt().coerceIn(0, 255)),
                ),
                floatArrayOf(0f, 0.48f, 0.82f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            val lightAlpha = ((0.55f + localWobble * 0.22f).coerceIn(0.2f, 0.95f) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = withAlpha(0xFFE8FFF5.toInt(), lightAlpha)
            canvas.drawCircle(cx - r * 0.34f, cy - r * 0.39f, (r * 0.13f).coerceAtLeast(1f), paint)
            val pinAlpha = ((0.32f + localWobble * 0.28f).coerceIn(0.08f, 0.85f) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = withAlpha(Color.WHITE, pinAlpha)
            canvas.drawCircle(cx - r * 0.08f, cy - r * 0.54f, (r * 0.045f).coerceAtLeast(1f), paint)
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