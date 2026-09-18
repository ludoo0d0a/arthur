package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Superdrive Vibes for Auto/Ambient album art. */
internal object SuperdriveStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        isDither = true
    }

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
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse

        // Dark background
        paint.color = 0xFF07040F.toInt()
        paint.style = Paint.Style.FILL
        paint.shader = null
        canvas.drawRect(0f, 0f, w, h, paint)

        val centerCol = palette.colorAt(0)
        val subCol = palette.colorAt(1)
        val centerAlpha = (0.45f * dim * 255).toInt().coerceIn(0, 255)
        val subAlpha = (0.20f * dim * 255).toInt().coerceIn(0, 255)

        // Radial glow
        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.5f,
            (minDim * 0.7f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb(centerAlpha, Color.red(centerCol), Color.green(centerCol), Color.blue(centerCol)),
                Color.argb(subAlpha, Color.red(subCol), Color.green(subCol), Color.blue(subCol)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.5f, minDim * 0.7f, paint)
        paint.shader = null

        val timeRad = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f) * 2f * PI.toFloat()

        // 1. Concentric vibe rings
        val ringCount = 7
        for (i in 0 until ringCount) {
            val radiusFrac = 0.15f + rnd.nextFloat() * 0.7f
            val vibeFreq = 4f + rnd.nextFloat() * 8f
            val vibeAmp = 8f + rnd.nextFloat() * 16f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val ringRadius = radiusFrac * minDim * 0.5f
            val ringCol = palette.colorAt(i + 2)

            val path = Path()
            val points = 60
            for (p in 0..points) {
                val angle = (p.toFloat() / points) * 2f * PI.toFloat()
                val vibe = sin(angle * vibeFreq + timeRad * 2f + phaseOffset) * vibeAmp
                val r = ringRadius + vibe
                val px = w * 0.5f + r * cos(angle)
                val py = h * 0.5f + r * sin(angle)
                if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.argb(
                (0.55f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(ringCol),
                Color.green(ringCol),
                Color.blue(ringCol),
            )
            canvas.drawPath(path, paint)
        }

        // 2. Multi-directional vibe lines
        val lineCount = 18
        for (i in 0 until lineCount) {
            val angleRad = rnd.nextFloat() * 2f * PI.toFloat()
            val amplitude = 20f + rnd.nextFloat() * 40f
            val frequency = 2f + rnd.nextFloat() * 4f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val strokeWidth = 2f + rnd.nextFloat() * 3f
            val alphaBase = 0.4f + rnd.nextFloat() * 0.45f
            val lineCol = palette.colorAt(i)

            val cosA = cos(angleRad)
            val sinA = sin(angleRad)
            val length = minDim * 1.2f
            val startX = w * 0.5f - length * 0.5f * cosA
            val startY = h * 0.5f - length * 0.5f * sinA

            val path = Path()
            val steps = 50
            for (s in 0..steps) {
                val frac = s.toFloat() / steps
                val dist = frac * length
                val osc = sin(frac * frequency * PI.toFloat() * 2f + timeRad * 3f + phaseOffset) * amplitude
                val px = startX + dist * cosA - osc * sinA
                val py = startY + dist * sinA + osc * cosA
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.color = Color.argb(
                (alphaBase * dim * 255).toInt().coerceIn(0, 255),
                Color.red(lineCol),
                Color.green(lineCol),
                Color.blue(lineCol),
            )
            canvas.drawPath(path, paint)
        }

        // 3. Vibe energy particles
        val particleCount = 40
        paint.style = Paint.Style.FILL
        for (i in 0 until particleCount) {
            val xFrac = rnd.nextFloat()
            val yFrac = rnd.nextFloat()
            val radius = 2f + rnd.nextFloat() * 4f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val pCol = palette.colorAt(i + 1)

            val oscX = sin(timeRad * 2f + phaseOffset) * 15f
            val oscY = cos(timeRad * 2f + phaseOffset) * 15f
            val px = xFrac * w + oscX
            val py = yFrac * h + oscY

            paint.color = Color.argb(
                (0.8f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(pCol),
                Color.green(pCol),
                Color.blue(pCol),
            )
            canvas.drawCircle(px, py, radius, paint)
        }

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
