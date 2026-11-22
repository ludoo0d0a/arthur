package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Rain for Auto/Ambient album art. */
internal object RainStill {
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
        val t01 = ((phase / (2f * PI.toFloat())) % 1f + 1f) % 1f

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A1220.toInt(), 0xFF05080F.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Soft mist sheet (blurred) behind streaks.
        val mist = 0.05f + 0.04f * loopedFbm(t01, radius = 1.5f, seedOffset = 11)
        paint.maskFilter = BlurMaskFilter((size * 0.03f).coerceAtLeast(6f), BlurMaskFilter.Blur.NORMAL)
        paint.color = Color.argb(((mist * (0.7f + 0.3f * pulse)) * 255).toInt().coerceIn(0, 255), 140, 160, 190)
        canvas.drawRect(0f, h * 0.35f, w, h, paint)
        paint.maskFilter = null

        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE

        val dim = 0.7f + 0.3f * pulse
        val gust = 0.06f + 0.08f * (loopedFbm(t01, radius = 1.2f, seedOffset = 29) * 2f - 1f)
        for (layer in 0..1) {
            val far = layer == 0
            val drops = if (far) 36 else 48
            val speedScale = if (far) 0.65f else 1.05f
            val alphaScale = if (far) 0.45f else 1f
            val lean = if (far) 0.08f else 0.14f
            for (i in 0 until drops) {
                val x0 = rnd.nextFloat()
                val y0 = rnd.nextFloat()
                val fallSpeed = (0.55f + rnd.nextFloat() * 0.8f) * speedScale
                val lengthFrac = 0.016f + rnd.nextFloat() * (if (far) 0.028f else 0.04f)
                val thickness = (if (far) 0.8f else 1.15f) + rnd.nextFloat() * 1.2f
                val driftAmp = 0.004f + rnd.nextFloat() * 0.016f
                val alphaBase = 0.16f + rnd.nextFloat() * 0.38f
                val fall = ((y0 + fallSpeed * t01) % 1f + 1f) % 1f
                val y = fall * h
                val sway = sin(phase * (0.3f + rnd.nextFloat())) * driftAmp + gust * 0.01f
                val x = (((x0 + sway) % 1f) + 1f) % 1f * w
                val len = lengthFrac * h
                val tint = palette.colorAt(i + layer * 7)
                paint.strokeWidth = thickness
                paint.color = Color.argb(
                    (alphaBase * dim * alphaScale * 255).toInt().coerceIn(0, 255),
                    ((Color.red(tint) + 184) / 2),
                    ((Color.green(tint) + 212) / 2),
                    ((Color.blue(tint) + 240) / 2),
                )
                canvas.drawLine(x, y, x + lean * len, y + len, paint)
            }
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
