package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Chromatic Blobs for Auto/Ambient album art. */
internal object ChromaticBlobsStill {
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
        val minDim = minOf(w, h)
        paint.color = 0xFF08060D.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 7) {
            val xA = rnd.nextFloat()
            val yA = rnd.nextFloat()
            val xB = rnd.nextFloat()
            val yB = rnd.nextFloat()
            val radiusFrac = 0.25f + rnd.nextFloat() * 0.25f
            val shadowOffsetFrac = 0.03f + rnd.nextFloat() * 0.05f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.35f + rnd.nextFloat() * 0.25f

            val morph = ((sin(loop * 2f * PI.toFloat() + phaseOffset) + 1f) / 2f)
            val cx = (xA + (xB - xA) * morph) * w
            val cy = (yA + (yB - yA) * morph) * h
            val radius = radiusFrac * minDim * (0.85f + 0.3f * morph)
            val shadowOffset = shadowOffsetFrac * minDim

            // Shadow layer
            paint.shader = RadialGradient(
                cx + shadowOffset, cy + shadowOffset, (radius * 1.15f).coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.45f * alphaBase * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                    Color.argb((0.15f * alphaBase * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx + shadowOffset, cy + shadowOffset, radius * 1.15f, paint)

            // Color blob
            val tintA = palette.colorAt(i)
            val tintB = palette.colorAt(i + 3)
            val r = (Color.red(tintA) + (Color.red(tintB) - Color.red(tintA)) * morph).toInt()
            val g = (Color.green(tintA) + (Color.green(tintB) - Color.green(tintA)) * morph).toInt()
            val b = (Color.blue(tintA) + (Color.blue(tintB) - Color.blue(tintA)) * morph).toInt()
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                cx, cy, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, r, g, b),
                    Color.argb((alpha * 0.5f).toInt(), r, g, b),
                    Color.argb((0.08f * dim * 255).toInt(), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 0.75f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
