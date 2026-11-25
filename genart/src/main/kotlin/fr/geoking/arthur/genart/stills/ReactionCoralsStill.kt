package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.CornerPathEffect
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val REACTION_CORALS_STILL_COUNT = 8
private const val REACTION_CORALS_BRANCH_CYCLES = 0.65f

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
        val branchSpin = t01 * 2f * PI.toFloat() * REACTION_CORALS_BRANCH_CYCLES
        val energy = 0.9f + 0.1f * pulse
        paint.style = Paint.Style.FILL
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)
        val minDim = minOf(w, h)
        val coral = palette.colorAt(0)
        val cyan = palette.colorAt(1)
        paint.pathEffect = CornerPathEffect(minDim * 0.01f)
        for (i in 0 until REACTION_CORALS_STILL_COUNT) {
            val n = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = i * 31 + 7)
            val cx = seededUnit(i * 19 + 5) * w
            val cy = seededUnit(i * 31 + 9) * h
            val colonyR = minDim * seededRange(i * 43 + 13, 0.06f, 0.18f) * (1.05f + n * 0.4f) * energy
            val phaseOffset = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat())
            val branches = 3 + i % 3
            val baseAngle = phaseOffset + branchSpin
            for (branch in 0 until branches) {
                val angle = baseAngle + branch * (2f * PI.toFloat() / branches)
                val length = colonyR * (1.45f + n * 0.35f)
                val path = Path().apply {
                    moveTo(cx, cy)
                    val segments = 8
                    for (step in 1..segments) {
                        val f = step / segments.toFloat()
                        val bend = sin(f * PI.toFloat() * 1.5f + baseAngle) * minDim * 0.022f
                        val x = cx + cos(angle) * length * f - sin(angle) * bend
                        val y = cy + sin(angle) * length * f + cos(angle) * bend
                        lineTo(x, y)
                    }
                    for (step in 2..6 step 2) {
                        val f = step / segments.toFloat()
                        val bx = cx + cos(angle) * length * f
                        val by = cy + sin(angle) * length * f
                        val side = if ((step + branch) % 2 == 0) 1f else -1f
                        val twig = Path().apply {
                            moveTo(bx, by)
                            lineTo(
                                bx + cos(angle + side * 0.82f) * colonyR * 0.48f,
                                by + sin(angle + side * 0.82f) * colonyR * 0.48f,
                            )
                        }
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeWidth = minDim * 0.0035f
                        paint.color = withAlpha(if ((i + branch) % 2 == 0) Color.CYAN else coral, (0.55f * energy * 255).toInt())
                        canvas.drawPath(twig, paint)
                    }
                }
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = minDim * 0.032f
                paint.color = withAlpha(if ((i + branch) % 2 == 0) cyan else coral, (0.16f * energy * 255).toInt())
                canvas.drawPath(path, paint)
                paint.strokeWidth = minDim * 0.005f
                paint.color = withAlpha(if ((i + branch) % 2 == 0) coral else cyan, (0.82f * energy * 255).toInt())
                canvas.drawPath(path, paint)
                val tipX = cx + cos(angle) * length
                val tipY = cy + sin(angle) * length
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(cyan, (0.52f * energy * 255).toInt())
                canvas.drawCircle(tipX, tipY, minDim * 0.013f, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(cyan, (0.22f * energy * 255).toInt())
            canvas.drawCircle(cx, cy, colonyR * 1.05f, paint)
            paint.color = withAlpha(coral, (0.78f * energy * 255).toInt())
            canvas.drawCircle(cx, cy, minDim * 0.01f, paint)
        }
        paint.pathEffect = null
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}