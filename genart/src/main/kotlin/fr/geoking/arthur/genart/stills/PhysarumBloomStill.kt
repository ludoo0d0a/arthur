package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.seededRange

internal object PhysarumBloomStill {
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
        val unit = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val centerX = w * (0.5f + 0.035f * sin(2f * PI.toFloat() * t01))
        val centerY = h * 0.5f
        val accent = palette.colorAt(0)
        val coral = 0xFFFF806E.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = 0xFF05080A.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val ambientRadius = unit * 0.54f
        paint.shader = RadialGradient(
            centerX, centerY, ambientRadius,
            intArrayOf(withAlpha(accent, 34), withAlpha(accent, 14), Color.TRANSPARENT),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(centerX, centerY, ambientRadius, paint)
        paint.shader = null

        val count = 14
        for (i in 0 until count) {
            val angle = i.toFloat() / count * 2f * PI.toFloat() + seededRange(i * 17 + 3, -0.15f, 0.15f)
            val length = unit * seededRange(i * 29 + 7, 0.20f, 0.40f)
            val bend = seededRange(i * 41 + 11, -0.14f, 0.14f) * unit
            val phaseOffset = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat())
            val thickness = unit * seededRange(i * 67 + 19, 0.0022f, 0.0045f)
            val noise = loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = i * 37 + 5) - 0.5f
            val pulseFactor = 0.86f + 0.14f * sin(2f * PI.toFloat() * t01 + phaseOffset)
            val liveAngle = angle + noise * 0.12f
            val dx = cos(liveAngle)
            val dy = sin(liveAngle)
            val nx = -dy
            val ny = dx
            val endX = centerX + dx * length
            val endY = centerY + dy * length
            val controlX = centerX + dx * length * 0.5f + nx * (bend + noise * unit * 0.025f)
            val controlY = centerY + dy * length * 0.5f + ny * (bend + noise * unit * 0.025f)
            val color = if (i % 5 == 0) coral else accent
            val alpha = (0.60f + 0.24f * pulseFactor) * pulse

            path.reset()
            path.moveTo(centerX, centerY)
            path.quadTo(controlX, controlY, endX, endY)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(color, (0.16f * alpha * 255f).toInt())
            paint.strokeWidth = thickness * 5.5f
            canvas.drawPath(path, paint)
            paint.color = withAlpha(color, (alpha * 255f).toInt())
            paint.strokeWidth = thickness
            canvas.drawPath(path, paint)

            for (side in intArrayOf(-1, 1)) {
                val branchDistance = length * (0.48f + 0.10f * (i % 3))
                val branchX = centerX + dx * branchDistance + nx * bend * 0.35f
                val branchY = centerY + dy * branchDistance + ny * bend * 0.35f
                val twigAngle = liveAngle + side * (0.58f + 0.12f * sin(phaseOffset + t01 * 2f * PI.toFloat()))
                val twigLength = length * (0.20f + 0.035f * ((i + side + 2) % 3))
                val tx = cos(twigAngle)
                val ty = sin(twigAngle)
                path.reset()
                path.moveTo(branchX, branchY)
                path.quadTo(
                    branchX + tx * twigLength * 0.55f + nx * side * bend * 0.18f,
                    branchY + ty * twigLength * 0.55f + ny * side * bend * 0.18f,
                    branchX + tx * twigLength,
                    branchY + ty * twigLength,
                )
                paint.color = withAlpha(color, (0.12f * alpha * 255f).toInt())
                paint.strokeWidth = thickness * 3f
                canvas.drawPath(path, paint)
                paint.color = withAlpha(color, (alpha * 0.72f * 255f).toInt())
                paint.strokeWidth = thickness * 0.68f
                canvas.drawPath(path, paint)
            }

            paint.style = Paint.Style.FILL
            paint.color = withAlpha(color, (0.45f * alpha * 255f).toInt())
            canvas.drawCircle(endX, endY, thickness * 1.35f, paint)
        }

        val coreRadius = unit * 0.13f * (0.94f + 0.06f * pulse)
        paint.shader = RadialGradient(
            centerX, centerY, coreRadius,
            intArrayOf(withAlpha(0xFFD8FFE8.toInt(), (0.52f * pulse * 255f).toInt()), withAlpha(accent, (0.64f * pulse * 255f).toInt()), withAlpha(accent, (0.18f * pulse * 255f).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.18f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, coreRadius, paint)
        paint.shader = null
        paint.color = withAlpha(coral, (0.12f * pulse * 255f).toInt())
        canvas.drawCircle(centerX + unit * 0.035f, centerY - unit * 0.025f, unit * 0.045f, paint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
