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

/** Bakes one frozen frame of Cherry Blossom Petals for Auto/Ambient album art. */
internal object CherryBlossomsStill {
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF18101A.toInt(), 0xFF08060C.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val loop = ((phase / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val dim = 0.55f + 0.45f * pulse

        for (i in 0 until 28) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val fallSpeed = 0.1f + rnd.nextFloat() * 0.18f
            val petalSize = 6f + rnd.nextFloat() * 8f
            val swayAmp = 0.05f + rnd.nextFloat() * 0.13f
            val swayFreq = 0.2f + rnd.nextFloat() * 0.6f
            val swayPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val rotSpeed = 0.15f + rnd.nextFloat() * 0.55f
            val phaseOffset = rnd.nextFloat() * 360f
            val fall = ((y0 + fallSpeed * loop) % 1f + 1f) % 1f
            val y = fall * h
            val sway = sin(loop * 2f * PI.toFloat() * swayFreq + swayPhase)
            val x = (((x0 + swayAmp * sway) % 1f) + 1f) % 1f * w
            val rotation = loop * 360f * rotSpeed + phaseOffset + rotationDeg * 0.1f
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 242) / 2
            val g = (Color.green(tint) + 182) / 2
            val b = (Color.blue(tint) + 200) / 2
            paint.color = Color.argb((0.7f * dim * 255).toInt().coerceIn(0, 255), r, g, b)
            val half = petalSize * 0.5f
            path.reset()
            path.moveTo(0f, half * 0.85f)
            path.quadTo(half * 0.95f, half * 0.15f, 0f, -half)
            path.quadTo(-half * 0.95f, half * 0.15f, 0f, half * 0.85f)
            path.close()
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rotation)
            canvas.drawPath(path, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }
}
