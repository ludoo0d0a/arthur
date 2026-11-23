package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Zen En So for Auto/Ambient album art. */
internal object ZenEnSoStill {
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
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val bleed = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 31)
        val dim = 0.85f + 0.15f * pulse

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFF5F2EA.toInt(), 0xFFE0D8C8.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cx = w * 0.5f
        val cy = h * 0.51f
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.47f,
            intArrayOf(withAlpha(0xFFC5B8A2.toInt(), (0.11f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.47f, paint)
        paint.shader = null

        val ink = mixArgb(0xFF2A2A28.toInt(), palette.colorAt(0), 0.12f)
        val radius = minDim * 0.285f
        val start = -PI.toFloat() * 0.77f + (bleed - 0.5f) * 0.018f
        val sweep = PI.toFloat() * 1.72f

        // Diffused ink underneath the dry, incomplete brush circle.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.025f
        paint.color = withAlpha(ink, ((0.10f + bleed * 0.06f) * dim * 255).toInt())
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL

        val samples = 120
        for (i in 0..samples) {
            val u = i / samples.toFloat()
            val angle = start + sweep * u
            val breath = loopedFbm(t01, radius = 1.15f + u * 0.2f, octaves = 2, seedOffset = 73 + i / 8)
            val wobble = (breath - 0.5f) * minDim * 0.012f
            val taper = 0.48f + 0.52f * sin(PI.toFloat() * u).coerceAtLeast(0f)
            val bristle = 0.82f + 0.18f * loopedFbm(t01, radius = 1.4f, octaves = 1, seedOffset = 211 + i * 3)
            val dotRadius = minDim * 0.0105f * taper * bristle
            val x = cx + cos(angle) * (radius + wobble)
            val y = cy + sin(angle) * (radius + wobble)
            paint.color = withAlpha(ink, ((0.84f + 0.12f * breath) * dim * 255).toInt())
            canvas.drawCircle(x, y, dotRadius, paint)
            if (i % 4 == 0 && (u < 0.2f || u > 0.8f)) {
                val fiberOffset = minDim * 0.015f * (0.5f + 0.5f * breath)
                paint.color = withAlpha(ink, (0.17f * dim * 255).toInt())
                canvas.drawCircle(
                    cx + cos(angle) * (radius + wobble + fiberOffset),
                    cy + sin(angle) * (radius + wobble + fiberOffset),
                    dotRadius * 0.22f,
                    paint,
                )
            }
        }

        // Slightly pooled ends make the open brush stroke read as hand painted.
        paint.color = withAlpha(ink, ((0.22f + 0.04f * bleed) * dim * 255).toInt())
        for (i in 0..1) {
            val angle = if (i == 0) start else start + sweep
            canvas.drawCircle(
                cx + cos(angle) * radius,
                cy + sin(angle) * radius,
                minDim * if (i == 0) 0.014f else 0.010f,
                paint,
            )
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
