package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Soft Caustics" for Auto/Ambient album art. */
internal object SoftCausticsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF04263A.toInt(), 0xFF01121F.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val streakCount = 16
        val blurRadius = (0.012f * minDim).coerceAtLeast(3f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        for (i in 0 until streakCount) {
            val xFrac = rnd.nextFloat()
            val yFrac = rnd.nextFloat()
            val lenFrac = 0.12f + rnd.nextFloat() * 0.16f
            val curveFrac = 0.03f + rnd.nextFloat() * 0.06f
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val thicknessFrac = 0.004f + rnd.nextFloat() * 0.006f
            val shimmerFreq = 0.3f + rnd.nextFloat() * 0.5f
            val shimmerPhase = rnd.nextFloat() * 2f * PI.toFloat()

            val cx = xFrac * w
            val cy = yFrac * h
            val len = lenFrac * minDim
            val curve = curveFrac * minDim
            val dirX = sin(angle)
            val dirY = sin(angle + PI.toFloat() / 2f)
            val normX = -dirY
            val normY = dirX

            val startX = cx - dirX * len / 2f
            val startY = cy - dirY * len / 2f
            val endX = cx + dirX * len / 2f
            val endY = cy + dirY * len / 2f
            val ctrlX = cx + normX * curve
            val ctrlY = cy + normY * curve

            val path = Path()
            path.moveTo(startX, startY)
            path.quadTo(ctrlX, ctrlY, endX, endY)

            val shimmer = (sin(drift * shimmerFreq + shimmerPhase) + 1f) / 2f
            val alpha = (0.18f + 0.22f * shimmer) * (0.7f + 0.3f * pulse)
            val tint = palette.colorAt(i)
            paint.strokeWidth = thicknessFrac * minDim
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 3 * 0x7F) / 4,
                (Color.green(tint) + 3 * 0xE9) / 4,
                (Color.blue(tint) + 3 * 0xFF) / 4,
            )
            canvas.drawPath(path, paint)
        }

        paint.maskFilter = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
