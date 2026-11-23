package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.exp

internal object StormStrobeCloudStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val oval = RectF()

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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val drift = loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 17)
        val drift2 = loopedFbm(t01, radius = 1.65f, octaves = 2, seedOffset = 43)
        val localPhase = (t01 + 0.36f) % 1f
        val burst = exp(-((localPhase - 0.20f) * (localPhase - 0.20f)) / 0.0032f)
        val afterglow = exp(-((localPhase - 0.245f) * (localPhase - 0.245f)) / 0.018f) * 0.24f
        val light = (burst * 0.52f + afterglow) * (0.7f + 0.3f * pulse)
        val glowX = w * (0.52f + (drift - 0.5f) * 0.10f)
        val glowY = h * (0.58f + (drift2 - 0.5f) * 0.035f)
        val glowRadius = minDim * (0.34f + 0.10f * burst)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0E18.toInt(), 0xFF111827.toInt(), 0xFF1A2230.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Long, soft cloud shelves recede into the dark sky.
        for (layer in 0..2) {
            val y = h * (0.40f + layer * 0.105f)
            val alpha = (0.40f + layer * 0.12f) * 255f
            paint.shader = LinearGradient(
                0f, y, w, y,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(alpha.toInt().coerceIn(0, 255), 26, 34, 48),
                    Color.argb(alpha.toInt().coerceIn(0, 255), 38, 49, 68),
                    Color.argb(alpha.toInt().coerceIn(0, 255), 26, 34, 48),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.2f, 0.52f, 0.82f, 1f),
                Shader.TileMode.CLAMP,
            )
            oval.set(-w * 0.08f, y, w * 1.08f, y + h * 0.30f)
            canvas.drawOval(oval, paint)
        }
        paint.shader = null

        // The luminous core stays inside the clouds, with a gradual blue-white falloff.
        paint.shader = RadialGradient(
            glowX, glowY, glowRadius,
            intArrayOf(
                Color.argb((0.30f * light * 255).toInt().coerceIn(0, 255), 232, 240, 255),
                Color.argb((0.20f * light * 255).toInt().coerceIn(0, 255), 191, 205, 224),
                Color.argb((0.095f * light * 255).toInt().coerceIn(0, 255), 137, 153, 179),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.38f, 0.72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)
        paint.shader = null

        // Overlapping cloud lobes partly veil the core and catch only its softest light.
        repeat(6) { index ->
            val stagger = (index + 0.5f) / 6f
            val noise = loopedFbm(t01, radius = 1.1f + stagger, octaves = 2, seedOffset = index * 29 + 7)
            val x = w * (stagger + (drift - 0.5f) * 0.035f)
            val y = h * (0.46f + (index % 3) * 0.075f + (noise - 0.5f) * 0.025f)
            val rx = minDim * (0.20f + (index % 3) * 0.055f)
            val ry = minDim * (0.11f + (index % 2) * 0.04f)
            val distance = ((x - glowX) / (minDim * 0.68f)).let { it * it }
            val lit = (light * (0.24f + 0.46f * exp(-distance))).coerceIn(0f, 0.58f)
            val r = (26 + (21 * lit)).toInt().coerceIn(0, 255)
            val g = (34 + (30 * lit)).toInt().coerceIn(0, 255)
            val b = (48 + (40 * lit)).toInt().coerceIn(0, 255)
            oval.set(x - rx, y - ry, x + rx, y + ry)
            paint.shader = RadialGradient(
                x, y, rx,
                intArrayOf(
                    Color.argb((0.86f + 0.08f * noise).times(255).toInt(), r, g, b),
                    Color.argb((0.76f * 255).toInt(), 26, 34, 48),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.74f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(oval, paint)
        }
        paint.shader = null

        // A restrained veil suggests the burst diffusing through cloud gaps, never a whiteout.
        if (light > 0.005f) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((0.055f * light * 255).toInt().coerceIn(0, 255), 232, 240, 255),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }
    }
}
