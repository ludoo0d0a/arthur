package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.CornerPathEffect
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object ReactionCoralsStill {
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
        paint.style = Paint.Style.FILL
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        val minDim = minOf(w, h)
        val coral = palette.colorAt(0)
        val cyan = palette.colorAt(1)
        paint.pathEffect = CornerPathEffect(minDim * 0.01f)
        for (i in 0 until 8) {
            val n = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = i * 31 + 7)
            val cx = (0.13f + (i % 4) * 0.25f + 0.025f * sin(t01 * 2f * PI.toFloat() + i)) * w
            val cy = (0.20f + (i / 4) * 0.54f + 0.025f * sin(t01 * 2f * PI.toFloat() + i * 1.7f)) * h
            val radius = minDim * (0.055f + 0.045f * n) * (0.9f + 0.1f * pulse)
            val branches = 4 + i % 3
            for (branch in 0 until branches) {
                val angle = branch * (2f * PI.toFloat() / branches) + i * 0.42f + t01 * 0.16f
                val length = radius * (1.5f + 0.4f * n)
                val path = Path().apply {
                    moveTo(cx, cy)
                    val segments = 8
                    for (step in 1..segments) {
                        val f = step / segments.toFloat()
                        val bend = sin(f * PI.toFloat() * 1.5f + i + branch) * radius * 0.22f
                        val x = cx + cos(angle) * length * f - sin(angle) * bend
                        val y = cy + sin(angle) * length * f + cos(angle) * bend
                        lineTo(x, y)
                    }
                    // Short side branches form the coral-like diffusion structure.
                    for (step in 2..6 step 2) {
                        val f = step / segments.toFloat()
                        val bx = cx + cos(angle) * length * f
                        val by = cy + sin(angle) * length * f
                        val side = if ((step + branch) % 2 == 0) 1f else -1f
                        val twig = Path().apply {
                            moveTo(bx, by)
                            lineTo(bx + cos(angle + side * 0.85f) * radius * 0.42f, by + sin(angle + side * 0.85f) * radius * 0.42f)
                        }
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeWidth = minDim * 0.003f
                        paint.color = withAlpha(if ((i + branch) % 2 == 0) Color.CYAN else coral, 150)
                        canvas.drawPath(twig, paint)
                    }
                }
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = minDim * 0.014f
                paint.color = withAlpha(if ((i + branch) % 2 == 0) cyan else coral, 42)
                canvas.drawPath(path, paint)
                paint.strokeWidth = minDim * 0.0035f
                paint.color = withAlpha(if ((i + branch) % 2 == 0) coral else cyan, 190)
                canvas.drawPath(path, paint)
                val tipX = cx + cos(angle) * length
                val tipY = cy + sin(angle) * length
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(cyan, 125)
                canvas.drawCircle(tipX, tipY, minDim * 0.008f, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(cyan, 35)
            canvas.drawCircle(cx, cy, radius * 1.05f, paint)
            paint.color = withAlpha(coral, 190)
            canvas.drawCircle(cx, cy, minDim * 0.009f, paint)
        }
        paint.pathEffect = null
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
