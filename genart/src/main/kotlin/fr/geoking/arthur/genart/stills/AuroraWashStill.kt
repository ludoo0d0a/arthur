package fr.geoking.arthur.genart.stills

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

/** Bakes one frozen frame of Aurora Wash for Auto/Ambient album art. */
internal object AuroraWashStill {
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
        paint.color = 0xFF020813.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        val count = 6
        for (i in 0 until count) {
            val yBaseFrac = (i + 1f) / (count + 1f)
            val amplitudeFrac = 0.08f + rnd.nextFloat() * 0.1f
            val freq = 1.2f + rnd.nextFloat() * 1.3f
            val speedMult = 0.8f + rnd.nextFloat() * 0.7f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()

            val c = palette.colorAt(i)

            val path = Path()
            val shadowPath = Path()
            val steps = 40
            val shadowOffsetY = h * 0.04f

            path.moveTo(0f, h)
            shadowPath.moveTo(0f, h)

            for (s in 0..steps) {
                val x = (s.toFloat() / steps) * w
                val angle = (s.toFloat() / steps) * 2f * PI.toFloat() * freq + loop * 2f * PI.toFloat() * speedMult + phaseOffset
                val y = yBaseFrac * h + sin(angle) * amplitudeFrac * h

                if (s == 0) {
                    path.lineTo(x, y)
                    shadowPath.lineTo(x, y + shadowOffsetY)
                } else {
                    path.lineTo(x, y)
                    shadowPath.lineTo(x, y + shadowOffsetY)
                }
            }

            path.lineTo(w, h)
            path.close()
            shadowPath.lineTo(w, h)
            shadowPath.close()

            // Shadow
            paint.shader = LinearGradient(
                0f, yBaseFrac * h, 0f, h,
                Color.argb((0.35f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(shadowPath, paint)

            // Aurora ribbon
            paint.shader = LinearGradient(
                0f, (yBaseFrac - amplitudeFrac) * h, 0f, h,
                intArrayOf(
                    Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.argb((0.15f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
