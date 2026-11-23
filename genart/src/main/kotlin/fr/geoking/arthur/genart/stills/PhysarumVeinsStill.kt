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

internal object PhysarumVeinsStill {
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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val accent = palette.colorAt(0)
        val r = Color.red(accent)
        val g = Color.green(accent)
        val b = Color.blue(accent)

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            w * 0.47f,
            h * 0.48f,
            minDim * 0.82f,
            intArrayOf(0xFF0B1714.toInt(), 0xFF05080A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val rootCount = 14
        for (i in 0 until rootCount) {
            val x0 = seededRange(i * 17 + 3, 0.08f, 0.92f) * w
            val y0 = seededRange(i * 29 + 7, 0.08f, 0.92f) * h
            val angle0 = seededRange(i * 41 + 11, 0f, 2f * PI.toFloat())
            val length = seededRange(i * 53 + 13, 0.16f, 0.29f) * minDim
            val width = seededRange(i * 67 + 19, 0.0015f, 0.0035f) * minDim
            val phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat())
            val noise = loopedFbm(t, radius = 1.35f, octaves = 3, seedOffset = i * 31 + 7)
            val angle = angle0 + sin(t * 2f * PI.toFloat() + phaseOffset) * 0.08f
            val sway = sin(t * 2f * PI.toFloat() + phaseOffset) * minDim * 0.012f
            val startX = x0 + sway
            val startY = y0 + (noise - 0.5f) * minDim * 0.025f
            val endX = startX + cos(angle) * length * (0.83f + noise * 0.25f)
            val endY = startY + sin(angle) * length * (0.83f + noise * 0.25f)
            val normalX = -sin(angle)
            val normalY = cos(angle)
            val bend = (noise - 0.5f) * length * 0.42f

            // A faint pool follows the root so the veins feel embedded in living tissue.
            paint.shader = RadialGradient(
                (startX + endX) * 0.5f,
                (startY + endY) * 0.5f,
                length * 0.58f,
                intArrayOf(Color.argb((12f * pulse).toInt().coerceIn(0, 20), r, g, b), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle((startX + endX) * 0.5f, (startY + endY) * 0.5f, length * 0.58f, paint)
            paint.shader = null

            val main = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX + cos(angle) * length * 0.34f + normalX * bend,
                    startY + sin(angle) * length * 0.34f + normalY * bend,
                    startX + cos(angle) * length * 0.72f - normalX * bend * 0.4f,
                    startY + sin(angle) * length * 0.72f - normalY * bend * 0.4f,
                    endX,
                    endY,
                )
            }
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.argb((34f * pulse).toInt().coerceIn(0, 60), r, g, b)
            paint.strokeWidth = width * 5.5f
            canvas.drawPath(main, paint)
            paint.color = Color.argb((145f * pulse).toInt().coerceIn(0, 190), r, g, b)
            paint.strokeWidth = width
            canvas.drawPath(main, paint)

            for (branch in 0..1) {
                val sign = if (branch == 0) -1f else 1f
                val branchAngle = angle + sign * (0.52f + 0.16f * noise)
                val junctionX = startX + cos(angle) * length * (0.48f + branch * 0.12f) + normalX * bend * 0.18f
                val junctionY = startY + sin(angle) * length * (0.48f + branch * 0.12f) + normalY * bend * 0.18f
                val branchLength = length * (0.42f + 0.10f * loopedFbm(t, radius = 1.1f, seedOffset = i * 37 + branch))
                val branchEndX = junctionX + cos(branchAngle) * branchLength
                val branchEndY = junctionY + sin(branchAngle) * branchLength
                val branchNormalX = -sin(branchAngle)
                val branchNormalY = cos(branchAngle)
                val branchBend = (noise - 0.5f) * branchLength * 0.36f
                val branchPath = Path().apply {
                    moveTo(junctionX, junctionY)
                    cubicTo(
                        junctionX + cos(branchAngle) * branchLength * 0.32f + branchNormalX * branchBend,
                        junctionY + sin(branchAngle) * branchLength * 0.32f + branchNormalY * branchBend,
                        junctionX + cos(branchAngle) * branchLength * 0.72f - branchNormalX * branchBend * 0.3f,
                        junctionY + sin(branchAngle) * branchLength * 0.72f - branchNormalY * branchBend * 0.3f,
                        branchEndX,
                        branchEndY,
                    )
                }
                paint.color = Color.argb((98f * pulse).toInt().coerceIn(0, 140), r, g, b)
                paint.strokeWidth = width * 0.68f
                canvas.drawPath(branchPath, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.argb((110f * pulse).toInt().coerceIn(0, 150), r, g, b)
                canvas.drawCircle(branchEndX, branchEndY, width * 1.1f, paint)
                paint.style = Paint.Style.STROKE
            }
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                startX,
                startY,
                minDim * 0.026f,
                intArrayOf(Color.argb((120f * pulse).toInt().coerceIn(0, 150), r, g, b), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(startX, startY, minDim * 0.026f, paint)
            paint.shader = null
        }
        paint.style = Paint.Style.FILL
    }
}
