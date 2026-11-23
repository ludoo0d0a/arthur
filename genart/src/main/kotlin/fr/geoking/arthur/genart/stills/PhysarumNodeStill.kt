package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object PhysarumNodeStill {
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
        paint.color = 0xFF05080A.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        val accent = palette.colorAt(0)
        val minDim = minOf(w, h)
        val centers = listOf(
            0.25f to 0.30f,
            0.72f to 0.28f,
            0.34f to 0.72f,
            0.73f to 0.70f,
        )
        val energy = pulse.coerceIn(0f, 1.4f)

        centers.forEach { (cx, cy) ->
            val x = w * cx
            val y = h * cy
            val radius = minDim * 0.25f
            paint.shader = RadialGradient(
                x,
                y,
                radius,
                intArrayOf(withAlpha(accent, (0.13f * energy * 255).toInt()), withAlpha(accent, (0.045f * energy * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }

        // Four compact nests, each formed from a handful of nodes and curved
        // branching connectors rather than a uniform all-screen mesh.
        for (cluster in centers.indices) {
            val (cx, cy) = centers[cluster]
            val points = ArrayList<Pair<Float, Float>>(6)
            for (j in 0 until 6) {
                val i = cluster * 6 + j
                val angle = j * (2f * PI.toFloat() / 6f) + seededRange(i * 17 + 3, -0.24f, 0.24f)
                val radius = minDim * seededRange(i * 29 + 7, 0.055f, 0.125f)
                val drift = minDim * 0.009f * sin(t01 * 2f * PI.toFloat() + i * 0.63f)
                val noise = loopedFbm(t01, radius = 1.3f, octaves = 2, seedOffset = i * 41 + 11)
                val x = w * cx + cos(angle) * radius + drift + minDim * 0.008f * (noise - 0.5f)
                val y = h * cy + sin(angle) * radius + minDim * 0.009f * cos(t01 * 2f * PI.toFloat() + i * 0.57f)
                points += x to y
            }

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = minDim * 0.003f
            for (j in points.indices) {
                val (x1, y1) = points[j]
                val (x2, y2) = points[(j + 1) % points.size]
                val bend = minDim * seededRange((cluster * 6 + j) * 53 + 13, -0.035f, 0.035f)
                val path = Path().apply {
                    moveTo(x1, y1)
                    quadTo((x1 + x2) * 0.5f + bend, (y1 + y2) * 0.5f - bend, x2, y2)
                }
                paint.color = withAlpha(accent, (0.26f * energy * 255).toInt())
                canvas.drawPath(path, paint)
            }
            // A few cross-links make the nest feel organic and interconnected.
            listOf(0 to 3, 1 to 4, 2 to 5).forEachIndexed { index, pair ->
                val (x1, y1) = points[pair.first]
                val (x2, y2) = points[pair.second]
                val path = Path().apply {
                    moveTo(x1, y1)
                    quadTo((x1 + x2) * 0.5f, (y1 + y2) * 0.5f + minDim * 0.02f, x2, y2)
                }
                paint.color = withAlpha(accent, (0.16f * energy * 255).toInt())
                paint.strokeWidth = minDim * (0.0018f + index * 0.0001f)
                canvas.drawPath(path, paint)
            }

            paint.style = Paint.Style.FILL
            points.forEachIndexed { j, (x, y) ->
                val i = cluster * 6 + j
                val breathe = 0.82f + 0.18f * sin(t01 * 2f * PI.toFloat() + i * 0.71f)
                val radius = minDim * seededRange(i * 67 + 19, 0.007f, 0.013f) * breathe
                paint.shader = RadialGradient(
                    x,
                    y,
                    radius * 3f,
                    intArrayOf(withAlpha(accent, (0.58f * energy * 255).toInt()), withAlpha(accent, (0.17f * energy * 255).toInt()), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.42f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(x, y, radius * 3f, paint)
                paint.shader = null
                paint.color = withAlpha(accent, (0.83f * energy * 255).toInt())
                canvas.drawCircle(x, y, radius * 0.48f, paint)
            }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
