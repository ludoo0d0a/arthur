package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the "Falling Leaves" engine for Auto/Ambient album art. */
internal object FallingLeavesStill {
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

        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.45f,
            size * 0.85f,
            intArrayOf(0xFF10161F.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val loopPhase = ((phase % 1f) + 1f) % 1f
        val leafCount = 30
        for (i in 0 until leafCount) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val fallSpeed = 0.12f + rnd.nextFloat() * 0.18f
            val leafSize = size * (0.02f + rnd.nextFloat() * 0.03f)
            val swayAmp = 0.06f + rnd.nextFloat() * 0.1f
            val swayFreq = 0.2f + rnd.nextFloat() * 0.5f
            val swayPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val rotSpeed = 0.1f + rnd.nextFloat() * 0.5f
            val phaseOffset = rnd.nextFloat() * 360f
            val glow = 0.4f + rnd.nextFloat() * 0.6f

            val fallPhase = ((y0 + fallSpeed * loopPhase) % 1f + 1f) % 1f
            val y = fallPhase * h
            val swayTime = loopPhase * 2f * PI.toFloat() * swayFreq + swayPhase
            val x = (((x0 + swayAmp * sin(swayTime)) % 1f + 1f) % 1f) * w
            val rotation = loopPhase * 360f * rotSpeed + phaseOffset + rotationDeg * 0.1f

            val tintColor = palette.colorAt(i)
            val r = Color.red(tintColor)
            val g = Color.green(tintColor)
            val b = Color.blue(tintColor)
            val alpha = ((0.55f + 0.4f * glow) * (0.85f + 0.15f * pulse)).coerceIn(0f, 1f)

            val half = leafSize * 0.5f
            path.reset()
            path.moveTo(0f, half)
            path.quadTo(half * 0.9f, half * 0.2f, 0f, -half)
            path.quadTo(-half * 0.9f, half * 0.2f, 0f, half)
            path.close()

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rotation)
            paint.shader = null
            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }
}
