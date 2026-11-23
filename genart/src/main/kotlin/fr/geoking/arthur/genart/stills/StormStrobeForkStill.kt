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
import kotlin.math.exp

internal object StormStrobeForkStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val trunk = Path()
    private val branch = Path()

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
        val drift = (loopedFbm(t01, radius = 1.3f, octaves = 2, seedOffset = 9) - 0.5f) * w * 0.04f

        paint.style = Paint.Style.FILL
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080C16.toInt(), 0xFF111A28.toInt(), 0xFF1A2230.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Draw one distant event, repeated on the seamless still phase.
        val local = (t01 + 0.19f) % 1f
        val flash = exp(-((local - 0.16f) * (local - 0.16f)) / 0.0016f) * (0.7f + 0.3f * pulse)
        val cx = w * (0.46f + 0.04f * loopedFbm(t01, seedOffset = 17)) + drift
        val cy = h * 0.43f
        if (flash > 0.025f) {
            val radius = minDim * (0.30f + 0.12f * flash)
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    Color.argb((0.12f * flash * 255).toInt().coerceIn(0, 255), 220, 232, 255),
                    Color.argb((0.07f * flash * 255).toInt().coerceIn(0, 255), 174, 198, 230),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null

            // Thin, branching fork, kept low contrast so the storm remains soft.
            trunk.reset()
            trunk.moveTo(cx, cy - minDim * 0.10f)
            trunk.lineTo(cx - minDim * 0.018f, cy - minDim * 0.01f)
            trunk.lineTo(cx + minDim * 0.006f, cy + minDim * 0.055f)
            trunk.lineTo(cx - minDim * 0.014f, cy + minDim * 0.13f)
            trunk.lineTo(cx + minDim * 0.01f, cy + minDim * 0.22f)
            paint.color = Color.argb((0.58f * flash * 255).toInt().coerceIn(0, 255), 220, 232, 255)
            paint.strokeWidth = minDim * 0.004f
            paint.strokeCap = Paint.Cap.ROUND
            paint.style = Paint.Style.STROKE
            canvas.drawPath(trunk, paint)

            branch.reset()
            branch.moveTo(cx + minDim * 0.006f, cy + minDim * 0.055f)
            branch.lineTo(cx + minDim * 0.056f, cy + minDim * 0.09f)
            branch.lineTo(cx + minDim * 0.14f, cy + minDim * 0.15f)
            paint.color = Color.argb((0.38f * flash * 255).toInt().coerceIn(0, 255), 199, 216, 242)
            paint.strokeWidth = minDim * 0.0025f
            canvas.drawPath(branch, paint)
            paint.style = Paint.Style.FILL
        }

        // Layered low clouds partly veil the fork and catch a little of its glow.
        paint.color = 0xFF0A0E18.toInt()
        canvas.drawOval(-w * 0.1f + drift, h * 0.28f, w * 0.62f + drift, h * 0.58f, paint)
        canvas.drawOval(w * 0.24f - drift, h * 0.22f, w * 1.1f - drift, h * 0.56f, paint)
        paint.color = Color.argb((0.13f * flash * 255).toInt().coerceIn(0, 255), 150, 172, 205)
        canvas.drawOval(w * 0.03f + drift, h * 0.41f, w * 0.63f + drift, h * 0.53f, paint)
        canvas.drawOval(w * 0.47f - drift, h * 0.40f, w * 1.09f - drift, h * 0.52f, paint)

        paint.shader = android.graphics.LinearGradient(
            0f, h * 0.48f, 0f, h,
            intArrayOf(Color.TRANSPARENT, 0x1F080C16, 0x73080C16),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.48f, w, h, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }
}
