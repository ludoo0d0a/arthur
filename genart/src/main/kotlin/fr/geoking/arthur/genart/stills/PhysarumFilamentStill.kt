package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

internal object PhysarumFilamentStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val teal = palette.colorAt(0)
        val coral = palette.colorAt(2)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF04090B.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.shader = android.graphics.RadialGradient(
            w * 0.48f,
            h * 0.53f,
            minDim * 0.62f,
            intArrayOf(withAlpha(teal, 35), withAlpha(0xFF12352B.toInt(), 25), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.48f, h * 0.53f, minDim * 0.62f, paint)
        paint.shader = null

        val count = 8
        for (i in 0 until count) {
            val startX = seeded(i * 17 + 3, 0.12f, 0.88f)
            val startY = seeded(i * 29 + 7, 0.12f, 0.88f)
            val endX = seeded(i * 41 + 11, 0.12f, 0.88f)
            val endY = seeded(i * 53 + 13, 0.12f, 0.88f)
            val side = if (i % 2 == 0) -1f else 1f
            val baseBend = side * seeded(i * 67 + 19, 0.08f, 0.22f)
            val p = seeded(i * 71 + 23, 0f, 2f * PI.toFloat())
            val noise = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = i * 37 + 5)
            val drift = sin(t01 * 2f * PI.toFloat() + p) * 0.025f
            val sx = (startX + drift) * w
            val sy = (startY + (noise - 0.5f) * 0.035f) * h
            val ex = (endX - drift * 0.7f) * w
            val ey = (endY + (0.5f - noise) * 0.035f) * h
            val dx = ex - sx
            val dy = ey - sy
            val bend = (baseBend + (noise - 0.5f) * 0.08f) * minDim
            val color = if (i % 5 == 0) coral else teal
            val stroke = minDim * seeded(i * 79 + 31, 0.0015f, 0.0032f)

            path.reset()
            path.moveTo(sx, sy)
            path.cubicTo(sx + dx * 0.30f - bend, sy + dy * 0.20f + bend, sx + dx * 0.68f + bend, sy + dy * 0.76f - bend, ex, ey)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = withAlpha(color, (0.13f * pulse * 255f).toInt())
            paint.strokeWidth = stroke * 6f
            canvas.drawPath(path, paint)
            paint.color = withAlpha(color, (0.30f * pulse * 255f).toInt())
            paint.strokeWidth = stroke * 2.7f
            canvas.drawPath(path, paint)
            paint.color = withAlpha(color, (0.78f * pulse * 255f).toInt())
            paint.strokeWidth = stroke
            canvas.drawPath(path, paint)

            for (branch in 0..1) {
                val fraction = if (branch == 0) 0.32f else 0.69f
                val bx = sx + dx * fraction
                val by = sy + dy * fraction + bend * (0.35f - fraction)
                val branchSide = if ((i + branch) % 2 == 0) -1f else 1f
                val reach = minDim * (0.07f + 0.025f * noise)
                path.reset()
                path.moveTo(bx, by)
                path.cubicTo(
                    bx + dx * 0.07f + reach * branchSide * 0.35f,
                    by + reach * branchSide * 0.25f,
                    bx + dx * 0.12f + reach * branchSide,
                    by + reach * branchSide,
                    bx + dx * 0.16f + reach * branchSide * 1.12f,
                    by + reach * branchSide * 1.18f,
                )
                paint.color = withAlpha(color, (0.12f * pulse * 255f).toInt())
                paint.strokeWidth = stroke * 3.5f
                canvas.drawPath(path, paint)
                paint.color = withAlpha(color, (0.60f * pulse * 255f).toInt())
                paint.strokeWidth = stroke * 0.68f
                canvas.drawPath(path, paint)
            }

            paint.style = Paint.Style.FILL
            val bead = minDim * (0.0035f + 0.002f * noise)
            paint.color = withAlpha(color, (0.26f * pulse * 255f).toInt())
            canvas.drawCircle(ex, ey, bead * 3.2f, paint)
            paint.color = withAlpha(color, (0.72f * pulse * 255f).toInt())
            canvas.drawCircle(ex, ey, bead, paint)
        }
    }

    private fun seeded(seed: Int, min: Float, max: Float): Float {
        val n = ((seed * 1103515245L + 12345L) and 0x7fffffffL).toFloat() / 0x7fffffffL.toFloat()
        return min + (max - min) * n
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
