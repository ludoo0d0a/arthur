package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Bakes one frozen frame of the large moon over rippling water with shimmering light bands. */
internal object MoonlightRipplesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val craterCount = 8
    private const val ringCount = 7

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
        val tau = 2f * PI.toFloat()
        val t = ((phase % tau) / tau).let { if (it < 0f) it + 1f else it }
        val glowT = phase01(t * 2.674f)
        val haloPulse = 0.85f + 0.15f * sin01(glowT * 2f * PI.toFloat())
        val dim = 0.85f + 0.15f * pulse
        val minDim = if (w < h) w else h
        val waterTop = h * 0.56f
        val moonCx = w * 0.5f
        val moonCy = h * 0.24f
        val moonRadius = minDim * 0.22f
        val moonBottom = moonCy + moonRadius * 0.98f

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            waterTop,
            intArrayOf(0xFF02060C.toInt(), 0xFF050D16.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, waterTop, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f,
            waterTop,
            0f,
            h,
            intArrayOf(0xFF050E18.toInt(), 0xFF01030A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, waterTop, w, h, paint)
        paint.shader = null

        // Soft moon halo.
        val moonArgb = 0xFFEFF4F8.toInt()
        val moonTint = Color.argb(
            (255f * dim).toInt().coerceIn(0, 255),
            Color.red(moonArgb),
            Color.green(moonArgb),
            Color.blue(moonArgb),
        )
        paint.shader = RadialGradient(
            moonCx,
            moonCy,
            moonRadius * 3.2f,
            Color.argb((0.30f * dim * haloPulse * 255f).toInt().coerceIn(0, 255), 0xEF, 0xF4, 0xFB),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, moonCy, moonRadius * 3.2f, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            moonCx,
            moonCy,
            moonRadius * 4.8f,
            Color.argb((0.09f * dim * haloPulse * 255f).toInt().coerceIn(0, 255), 0xEF, 0xF4, 0xFB),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, moonCy, moonRadius * 4.8f, paint)
        paint.shader = null

        // Moon disk with soft brightening core and subtle night-side terminator.
        paint.color = Color.argb((0.92f * dim * 255f).toInt().coerceIn(0, 255), 255, 255, 255)
        canvas.drawCircle(moonCx, moonCy, moonRadius, paint)
        paint.shader = RadialGradient(
            moonCx - moonRadius * 0.35f,
            moonCy - moonRadius * 0.35f,
            moonRadius * 1.3f,
            Color.argb((0.92f * dim * 255f).toInt().coerceIn(0, 255), 255, 255, 255),
            moonTint,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, moonCy, moonRadius, paint)
        paint.shader = null

        for (i in 0 until craterCount) {
            val xFrac = seededRange(i * 211 + 3, -0.55f, 0.55f)
            val yFrac = seededRange(i * 223 + 7, -0.55f, 0.55f)
            val radiusFrac = seededRange(i * 227 + 11, 0.05f, 0.13f)
            val alpha = 0.25f + seededRange(i * 229 + 17, 0.12f, 0.32f)
            val craterColor = Color.argb(
                (alpha * dim * 255f).toInt().coerceIn(0, 255),
                147,
                162,
                184,
            )
            paint.color = craterColor
            canvas.drawCircle(
                moonCx + xFrac * moonRadius,
                moonCy + yFrac * moonRadius,
                radiusFrac * moonRadius,
                paint,
            )
        }

        // Soft haze resting on the water surface.
        paint.shader = RadialGradient(
            moonCx,
            waterTop,
            minDim * 0.9f,
            Color.argb((0.10f * dim * haloPulse * 255f).toInt().coerceIn(0, 255), 0xEF, 0xF4, 0xFB),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, waterTop, minDim * 0.9f, paint)
        paint.shader = null

        // Concentric ripples in perspective (flattened ellipses) born on the water.
        val origins = arrayOf(
            floatArrayOf(0.5f, 0.68f),
            floatArrayOf(
                0.30f + seededRange(136, 0f, 0.4f) * 0.25f,
                0.60f + seededRange(184, 0f, 0.28f),
            ),
        )
        for (i in 0 until ringCount) {
            val originIndex = i % 2
            val phaseOffset = seededUnit(i * 47 + 3)
            val cyclesPerLoop = seededRange(i * 61 + 7, 1.9f, 3.0f)
            val maxRadiusFrac = seededRange(i * 79 + 13, 0.13f, 0.26f)
            val baseAlpha = seededRange(i * 97 + 17, 0.25f, 0.42f)
            val life = phase01(t * cyclesPerLoop + phaseOffset)
            val radius = life * maxRadiusFrac * minDim
            if (radius > 0.5f) {
                val alpha = (1f - life) * baseAlpha * dim
                val tint = palette.colorAt(i)
                paint.color = Color.argb(
                    (alpha * 255f).toInt().coerceIn(0, 255),
                    Color.red(tint),
                    Color.green(tint),
                    Color.blue(tint),
                )
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (2.2f - life * 1.6f).coerceAtLeast(0.7f)
                val origin = origins[originIndex]
                val cx = origin[0] * w
                val cy = origin[1] * h
                canvas.save()
                canvas.scale(1f, 0.3f, cx, cy)
                canvas.drawCircle(cx, cy, radius, paint)
                canvas.restore()
            }
        }

        // Moon reflection: vertical light bands that break and shimmer over the water.
        val waterColumn = (h - moonBottom).coerceAtLeast(0.5f)
        val fragmentCount = 4
        for (i in 0 until fragmentCount) {
            val phaseOffset = seededUnit(i * 89 + 7)
            val speed = 0.7f + seededRange(i * 101 + 11, 0.5f, 1.1f)
            val offsetFrac = seededUnit(i * 113 + 13) * 0.8f
            val widthFrac = 0.012f + seededUnit(i * 127 + 17) * 0.02f
            val alphaBase = 0.35f + seededRange(i * 131 + 19, 0.3f, 0.6f)
            val shimmer = sin01(2f * PI.toFloat() * phase01(glowT * speed + phaseOffset))
            val length = waterColumn * (0.3f + 0.55f * shimmer)
            val alpha = (0.35f + 0.65f * shimmer) * alphaBase * dim
            val x = moonCx + offsetFrac * moonRadius * 0.9f
            val width = moonRadius * widthFrac
            if (width > 0.5f) {
                val left = x - width * 0.5f
                val top = moonBottom
                val bottom = top + length
                paint.color = Color.argb(
                    (alpha * 255f).toInt().coerceIn(0, 255),
                    0xEF,
                    0xF4,
                    0xF8,
                )
                paint.style = Paint.Style.FILL
                val rx = width * 0.45f
                canvas.drawRoundRect(left, top, left + width, bottom, rx, rx, paint)
            }
        }

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.alpha = 255
    }
}
