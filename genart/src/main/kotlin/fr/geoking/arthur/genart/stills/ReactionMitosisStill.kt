package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object ReactionMitosisStill {
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
        val minDim = size.toFloat()
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = 0xFF08060C.toInt()
        canvas.drawRect(0f, 0f, w, h, paint)

        for (i in 0 until 8) {
            val n = loopedFbm(t, radius = 1.15f, octaves = 2, seedOffset = i * 29)
            val angle0 = (i * 2.3999632f) + 0.35f
            val orbit = minDim * (0.08f + (i % 3) * 0.035f)
            val cx = w * (0.5f + cos(angle0) * orbit / minDim) + sin(t * 2f * PI.toFloat() + i) * minDim * 0.006f
            val cy = h * (0.5f + sin(angle0) * orbit / minDim) + cos(t * 2f * PI.toFloat() * 0.7f + i) * minDim * 0.005f
            val cellRadius = minDim * (0.075f + 0.012f * n) * (0.94f + 0.06f * pulse)
            val axis = angle0 + 0.10f * sin(t * 2f * PI.toFloat() + i)
            val separation = cellRadius * (0.12f + 0.56f * (0.5f + 0.5f * sin(t * 2f * PI.toFloat() + i * 0.8f)))
            val dx = cos(axis) * separation
            val dy = sin(axis) * separation
            val accent = palette.colorAt(i)

            paint.shader = RadialGradient(
                cx, cy, cellRadius * 2.3f,
                intArrayOf(withAlpha(accent, 46), withAlpha(accent, 20), Color.TRANSPARENT),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, cellRadius * 2.3f, paint)
            paint.shader = null

            paint.color = withAlpha(accent, 138)
            canvas.drawCircle(cx - dx, cy - dy, cellRadius, paint)
            canvas.drawCircle(cx + dx, cy + dy, cellRadius, paint)

            // Dark furrow separates the pair while it slowly opens and closes.
            paint.color = withAlpha(0xFF08060C.toInt(), 218)
            canvas.drawCircle(cx, cy, cellRadius * 0.18f, paint)
            paint.color = withAlpha(Color.WHITE, 90)
            canvas.drawCircle(cx, cy, cellRadius * 0.05f, paint)

            // Soft outer membrane ring keeps each cell legible over neighboring glow.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = cellRadius * 0.045f
            paint.color = withAlpha(accent, (26f + 14f * n).toInt())
            canvas.drawCircle(cx - dx, cy - dy, cellRadius * 0.94f, paint)
            canvas.drawCircle(cx + dx, cy + dy, cellRadius * 0.94f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
