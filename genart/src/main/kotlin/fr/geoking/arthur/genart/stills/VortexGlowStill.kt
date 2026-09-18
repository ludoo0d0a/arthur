package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.min
import kotlin.random.Random

/** Bakes one frozen frame of Vortex Glow for Auto/Ambient album art. */
internal object VortexGlowStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = min(w, h) * 0.7f

        paint.color = 0xFF030308.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val currentRotationDeg = loop * 360f
        val dim = 0.65f + 0.35f * pulse

        val count = 9
        for (i in 0 until count) {
            val angleOffsetDeg = (360f / count) * i
            val radiusScale = 0.45f + rnd.nextFloat() * 0.5f
            val armRadius = maxRadius * radiusScale
            val c = palette.colorAt(i)

            canvas.save()
            canvas.rotate(currentRotationDeg + angleOffsetDeg, cx, cy)

            paint.shader = RadialGradient(
                cx + armRadius * 0.4f, cy, (armRadius * 0.6f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.45f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.argb((0.15f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx + armRadius * 0.4f, cy, armRadius * 0.6f, paint)

            canvas.restore()
        }

        // Dark central shadow core
        paint.shader = RadialGradient(
            cx, cy, (maxRadius * 0.35f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.85f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.argb((0.45f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, maxRadius * 0.35f, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
