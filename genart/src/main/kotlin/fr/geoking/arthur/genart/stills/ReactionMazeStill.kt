package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

internal object ReactionMazeStill {
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
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        val rose = palette.colorAt(0)
        val cyan = palette.colorAt(1)
        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.5f,
            minDim * 0.75f,
            intArrayOf(withAlpha(cyan, 34), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.5f, minDim * 0.75f, paint)
        paint.shader = null

        val lineCount = 10
        for (lineIndex in 0 until lineCount) {
            val row = lineIndex / 2
            val col = lineIndex % 2
            val band = (row + 1f) / (lineCount / 2f + 1f)
            val sway = 0.05f + (lineIndex % 5) * 0.012f
            val linePhase = lineIndex * 0.73f
            val y = h * band
            path.reset()
            path.moveTo(-w * 0.06f, y)
            for (segment in 0..6) {
                val x0 = w * (segment / 6f - 0.06f)
                val x1 = w * ((segment + 1f) / 6f - 0.06f)
                val midX = (x0 + x1) * 0.5f
                val wave = sin(t * 2f * PI.toFloat() + linePhase + segment * 0.9f) * h * sway * 0.22f
                val offset = if (col == 0) -1f else 1f
                path.quadTo(midX, y + wave + offset * minDim * 0.035f, x1, y + wave * 0.65f)
            }
            val color = if ((lineIndex + row) % 3 == 0) rose else cyan
            val glow = 0.12f + 0.05f * loopedFbm(t, radius = 1.2f, seedOffset = lineIndex * 13)

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = withAlpha(color, (glow * 255f * (0.85f + 0.15f * pulse)).toInt())
            paint.strokeWidth = minDim * 0.022f
            canvas.drawPath(path, paint)
            paint.color = withAlpha(color, 72)
            paint.strokeWidth = minDim * 0.003f
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
