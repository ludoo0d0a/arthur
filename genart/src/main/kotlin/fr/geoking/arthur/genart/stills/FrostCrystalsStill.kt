package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one static frame of the First Frost Crystals sparkle scatter for Auto album art. */
internal object FrostCrystalsStill {
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

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            0xFF060B14.toInt(),
            0xFF0C1A26.toInt(),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val count = 30
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        val radii = FloatArray(count)
        val lineCounts = IntArray(count)
        val rotations = FloatArray(count)
        val freqs = FloatArray(count)
        val phaseOffsets = FloatArray(count)
        for (i in 0 until count) {
            xs[i] = rnd.nextFloat() * w
            ys[i] = rnd.nextFloat() * h
            radii[i] = (3f + rnd.nextFloat() * 4f) * (w / 64f)
            lineCounts[i] = if (rnd.nextFloat() < 0.5f) 2 else 3
            rotations[i] = rnd.nextFloat() * 2f * PI.toFloat()
            freqs[i] = 0.4f + rnd.nextFloat() * 0.5f
            phaseOffsets[i] = rnd.nextFloat()
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (1.1f * (w / 64f)).coerceAtLeast(1f)
        paint.strokeCap = Paint.Cap.ROUND
        val timeUnit = phase / (2f * PI.toFloat())
        for (i in 0 until count) {
            val cyclePos = phase01(timeUnit * freqs[i] + phaseOffsets[i])
            val envelope = frostEnvelope(cyclePos)
            if (envelope <= 0.001f) continue
            val c = palette.colorAt(i)
            val alpha = (envelope * pulse.coerceIn(0.35f, 1f)).coerceIn(0f, 1f)
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(c) * 0.7f + 255 * 0.3f).toInt().coerceIn(0, 255),
                (Color.green(c) * 0.7f + 255 * 0.3f).toInt().coerceIn(0, 255),
                (Color.blue(c) * 0.7f + 255 * 0.3f).toInt().coerceIn(0, 255),
            )
            val armLength = radii[i] * (0.75f + 0.25f * envelope)
            val angleStep = PI.toFloat() / lineCounts[i]
            for (line in 0 until lineCounts[i]) {
                val angle = rotations[i] + line * angleStep
                val dx = cos(angle) * armLength
                val dy = sin(angle) * armLength
                canvas.drawLine(xs[i] - dx, ys[i] - dy, xs[i] + dx, ys[i] + dy, paint)
            }
        }
    }

    private fun phase01(t: Float): Float = ((t % 1f) + 1f) % 1f

    private fun frostEnvelope(cyclePos: Float): Float {
        val fadeIn = 0.35f
        val fadeOut = 0.65f
        return when {
            cyclePos < fadeIn -> smoothStep(cyclePos / fadeIn)
            cyclePos < fadeOut -> 1f
            else -> smoothStep((1f - cyclePos) / (1f - fadeOut))
        }
    }

    private fun smoothStep(x: Float): Float {
        val u = x.coerceIn(0f, 1f)
        return u * u * (3f - 2f * u)
    }
}
