package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the "Aurora Ribbons" engine for Auto/Ambient album art. */
internal object AuroraStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val t01 = ((phase / (2f * PI.toFloat())) % 1f + 1f) % 1f

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF040A1A.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Soft polar glow behind ribbons (depth cue, never a hard flash).
        val skyGlow = 0.08f + 0.10f * pulse
        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.28f,
            w * 0.7f,
            intArrayOf(
                Color.argb((skyGlow * 255).toInt().coerceIn(0, 255), 40, 90, 120),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.28f, w * 0.7f, paint)
        paint.shader = null

        val starCount = 56
        for (i in 0 until starCount) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h * 0.72f
            val twinkle = 0.75f + 0.25f * loopedFbm(t01, radius = 1.1f + i * 0.02f, seedOffset = i * 17)
            val alpha = (0.12f + rnd.nextFloat() * 0.5f) * twinkle * (0.85f + 0.15f * pulse)
            val radius = size * (0.0009f + rnd.nextFloat() * 0.0028f)
            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(x, y, radius, paint)
        }

        val ribbonCount = 6
        val timeAngle = phase + rotationDeg * PI.toFloat() / 180f
        val blurPx = (size * 0.018f).coerceAtLeast(4f)
        paint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)

        for (i in 0 until ribbonCount) {
            val baseYFrac = 0.10f + rnd.nextFloat() * 0.40f
            val amplitudeFrac = 0.018f + rnd.nextFloat() * 0.045f * (0.7f + pulse * 0.3f)
            val cycles = 1f + rnd.nextFloat() * 1.3f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val thicknessFrac = 0.06f + rnd.nextFloat() * 0.08f
            val bottomPhaseShift = 0.3f + rnd.nextFloat() * 0.6f
            val speedMul = 0.55f + rnd.nextFloat() * 0.75f
            val color = palette.colorAt(i)

            val baseY = baseYFrac * h
            val amplitude = amplitudeFrac * h
            val thickness = thicknessFrac * h
            val freqPerPixel = cycles * 2f * PI.toFloat() / w
            val ribbonTime = timeAngle * speedMul

            path.reset()
            val segments = 40
            val topYs = FloatArray(segments + 1)
            val bottomYs = FloatArray(segments + 1)
            for (k in 0..segments) {
                val x = (k / segments.toFloat()) * w
                val warp = (loopedFbm(t01, radius = 1.4f, seedOffset = i * 41 + k) - 0.5f) * amplitude * 0.35f
                topYs[k] = baseY + amplitude * sin(x * freqPerPixel + ribbonTime + phaseOffset) + warp
                bottomYs[k] = baseY + thickness +
                    amplitude * sin(x * freqPerPixel + ribbonTime + phaseOffset + bottomPhaseShift) + warp * 0.6f
            }
            path.moveTo(0f, topYs[0])
            for (k in 1..segments) {
                val x = (k / segments.toFloat()) * w
                path.lineTo(x, topYs[k])
            }
            path.lineTo(w, bottomYs[segments])
            for (k in segments - 1 downTo 0) {
                val x = (k / segments.toFloat()) * w
                path.lineTo(x, bottomYs[k])
            }
            path.close()

            val alpha = ((0.14f + rnd.nextFloat() * 0.22f) * (0.85f + 0.15f * pulse) * 255)
                .toInt()
                .coerceIn(0, 255)
            val topExtent = (baseY - amplitude).coerceAtLeast(0f)
            val bottomExtent = (baseY + thickness + amplitude).coerceAtMost(h)
            paint.shader = LinearGradient(
                0f,
                topExtent,
                0f,
                bottomExtent,
                intArrayOf(
                    Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)
            paint.shader = null
        }
        paint.maskFilter = null
        paint.alpha = 255
    }
}
