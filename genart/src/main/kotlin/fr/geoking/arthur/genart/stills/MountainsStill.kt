package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.random.Random

/** Bakes one static frame of the Layered Mountains look for Auto album art. */
internal object MountainsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val hazeColor = Color.rgb(0x3B, 0x4A, 0x6B)
    private val moonColor = Color.rgb(0xF7, 0xF1, 0xDE)
    private const val LAYER_COUNT = 5

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

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF10142B.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val moonXFrac = 0.62f + rnd.nextFloat() * 0.24f
        val moonYFrac = 0.12f + rnd.nextFloat() * 0.16f
        val moonRadius = minOf(w, h) * 0.07f
        val moonCx = w * moonXFrac
        val moonCy = h * moonYFrac
        val glowStrength = (0.7f + pulse * 0.3f).coerceIn(0f, 1f)

        paint.shader = RadialGradient(
            moonCx,
            moonCy,
            moonRadius * 4.5f,
            intArrayOf(
                Color.argb((0.85f * 255 * glowStrength).toInt().coerceIn(0, 255), Color.red(moonColor), Color.green(moonColor), Color.blue(moonColor)),
                Color.argb((0.16f * 255 * glowStrength).toInt().coerceIn(0, 255), Color.red(moonColor), Color.green(moonColor), Color.blue(moonColor)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(moonCx, moonCy, moonRadius * 4.5f, paint)
        paint.shader = null

        paint.color = Color.argb(230, Color.red(moonColor), Color.green(moonColor), Color.blue(moonColor))
        canvas.drawCircle(moonCx, moonCy, moonRadius, paint)

        val starCount = 26
        for (i in 0 until starCount) {
            val sx = rnd.nextFloat() * w
            val sy = (0.03f + rnd.nextFloat() * 0.39f) * h
            val sAlpha = (0.25f + rnd.nextFloat() * 0.6f)
            val sRadius = 0.8f + rnd.nextFloat() * 1.4f
            paint.color = Color.argb((sAlpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(sx, sy, sRadius, paint)
        }

        for (i in 0 until LAYER_COUNT) {
            val distanceT = 1f - i / (LAYER_COUNT - 1).coerceAtLeast(1).toFloat()
            val peakCount = 6 + rnd.nextInt(5)
            val peakHeights = FloatArray(peakCount) { rnd.nextFloat() }
            val baselineFrac = lerp(0.78f, 0.35f, distanceT)
            val amplitudeFrac = lerp(0.22f, 0.05f, distanceT)
            val speedFactor = lerp(1f, 0.15f, distanceT)

            val baseColor = palette.colorAt(i)
            val tinted = mixArgb(baseColor, hazeColor, distanceT * 0.7f)
            val brightnessFactor = lerp(0.9f, 0.35f, distanceT)
            val alpha = lerp(0.95f, 0.3f, distanceT)
            val color = scaleBrightness(tinted, brightnessFactor)

            val driftFrac = (((phase / (2f * PI.toFloat())) * speedFactor) % 1f + 1f) % 1f
            val driftOffset = driftFrac * w

            buildRidgePath(peakHeights, baselineFrac, amplitudeFrac, w, h)
            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

            canvas.save()
            canvas.translate(-driftOffset, 0f)
            canvas.drawPath(path, paint)
            canvas.restore()

            canvas.save()
            canvas.translate(-driftOffset + w, 0f)
            canvas.drawPath(path, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }

    private fun buildRidgePath(
        peakHeights: FloatArray,
        baselineFrac: Float,
        amplitudeFrac: Float,
        w: Float,
        h: Float,
    ) {
        path.reset()
        val baselineY = h * baselineFrac
        val amplitude = h * amplitudeFrac
        val peakCount = peakHeights.size
        val step = w / peakCount
        path.moveTo(0f, h)
        path.lineTo(0f, baselineY - peakHeights[0] * amplitude)
        for (j in 1..peakCount) {
            val x = j * step
            val heightSeed = peakHeights[j % peakCount]
            val y = baselineY - heightSeed * amplitude
            path.lineTo(x, y)
        }
        path.lineTo(w, h)
        path.close()
    }

    private fun lerp(from: Float, to: Float, t: Float): Float =
        from + (to - from) * t.coerceIn(0f, 1f)

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun scaleBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
