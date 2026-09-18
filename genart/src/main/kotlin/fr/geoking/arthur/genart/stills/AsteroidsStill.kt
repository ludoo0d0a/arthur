package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of "Asteroids" — starfield backdrop plus several tumbling rock silhouettes. */
internal object AsteroidsStill {
    private const val ROCK_COUNT = 5
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
        StarFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)

        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)

        paint.maskFilter = null
        paint.style = Paint.Style.FILL

        for (i in 0 until ROCK_COUNT) {
            val s = i * 613 + 5000
            val driftSpeedMult = seededRange(s + 51, 0.6f, 1.4f)
            val driftPhase = seededRange(s + 61, 0f, 1f)
            val spinSpeedMult = seededRange(s + 71, 0.4f, 1.3f)
            val spinPhase = seededRange(s + 81, 0f, 2f * PI.toFloat())
            val sizeUnit = seededRange(s + 91, 0f, 1f)

            val startXFrac = seededRange(s + 11, -0.15f, 0.2f)
            val startYFrac = seededRange(s + 21, -0.15f, 1.15f)
            val endXFrac = seededRange(s + 31, 0.8f, 1.15f)
            val endYFrac = seededRange(s + 41, -0.15f, 1.15f)

            val basePeriod = 600.0
            val cyclePos = (((generation.toDouble() * driftSpeedMult) % basePeriod) / basePeriod + driftPhase).toFloat()
            val t = phase01(cyclePos)
            val x = startXFrac * w + (endXFrac - startXFrac) * w * t
            val y = startYFrac * h + (endYFrac - startYFrac) * h * t
            val edgeFade = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val alpha = (0.8f * (0.6f + 0.4f * pulse) * edgeFade * 255).toInt().coerceIn(0, 255)
            if (alpha <= 2) continue

            val rockRotationDeg = (rotationDeg * spinSpeedMult + spinPhase * 180f / PI.toFloat()) % 360f
            val rockSize = minDim * (0.018f + sizeUnit * 0.03f)

            val dx2 = seededRange(s + 101, -0.6f, 0.6f) * rockSize
            val dy2 = seededRange(s + 111, -0.6f, 0.6f) * rockSize
            val r2 = seededRange(s + 121, 0.5f, 0.85f) * rockSize
            val dx3 = seededRange(s + 131, -0.6f, 0.6f) * rockSize
            val dy3 = seededRange(s + 141, -0.6f, 0.6f) * rockSize
            val r3 = seededRange(s + 151, 0.4f, 0.7f) * rockSize

            paint.color = Color.argb(alpha, 0x3A, 0x3D, 0x45)
            canvas.save()
            canvas.rotate(rockRotationDeg, x, y)
            canvas.drawCircle(x, y, rockSize, paint)
            canvas.drawCircle(x + dx2, y + dy2, r2, paint)
            canvas.drawCircle(x + dx3, y + dy3, r3, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }

    private fun seededUnit(seed: Int): Float {
        var x = seed * 1103515245 + 12345
        x = (x ushr 16) xor x
        return ((x and 0x7fff).toFloat() / 0x7fff.toFloat()).coerceIn(0f, 1f)
    }

    private fun seededRange(seed: Int, min: Float, max: Float): Float =
        min + seededUnit(seed) * (max - min)

    private fun phase01(t: Float): Float = ((t % 1f) + 1f) % 1f
}
