package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Waterfall Mist for Auto/Ambient album art. */
internal object WaterfallMistStill {
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
            0f, 0f, 0f, h,
            intArrayOf(0xFF10161C.toInt(), 0xFF05080B.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val rockWidth = w * 0.22f
        paint.color = Color.rgb(0x23, 0x20, 0x1D)
        canvas.drawRect(0f, 0f, rockWidth, h, paint)
        canvas.drawRect(w - rockWidth, 0f, w, h, paint)

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val dim = 0.7f + 0.3f * pulse
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val bandLeft = w * 0.38f
        val bandWidth = w * 0.24f
        for (i in 0 until 80) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val fallSpeed = 0.95f + rnd.nextFloat() * 0.95f
            val lengthFrac = 0.03f + rnd.nextFloat() * 0.05f
            val thickness = 1f + rnd.nextFloat() * 1.2f
            val driftAmp = 0.002f + rnd.nextFloat() * 0.006f
            val driftFreq = 0.3f + rnd.nextFloat() * 0.9f
            val alphaBase = 0.22f + rnd.nextFloat() * 0.38f
            val fall = ((y0 + fallSpeed * (phase / (2f * PI.toFloat()))) % 1f + 1f) % 1f
            val y = fall * h
            val sway = sin(drift * driftFreq) * driftAmp
            val x = bandLeft + (((x0 + sway) % 1f + 1f) % 1f) * bandWidth
            val len = lengthFrac * h
            val tint = palette.colorAt(i)
            paint.strokeWidth = thickness
            paint.color = Color.argb(
                (alphaBase * dim * 255).toInt().coerceIn(0, 255),
                ((Color.red(tint) + 220) / 2),
                ((Color.green(tint) + 238) / 2),
                ((Color.blue(tint) + 255) / 2).coerceAtMost(255),
            )
            canvas.drawLine(x, y, x, y + len, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL

        val mistCount = 6
        for (i in 0 until mistCount) {
            val x0 = 0.38f + rnd.nextFloat() * 0.24f
            val riseSpeed = 0.05f + rnd.nextFloat() * 0.09f
            val startDelay = rnd.nextFloat()
            val widthFrac = 0.16f + rnd.nextFloat() * 0.18f
            val heightFrac = 0.1f + rnd.nextFloat() * 0.12f
            val swayAmp = 0.01f + rnd.nextFloat() * 0.02f
            val swayFreq = 0.2f + rnd.nextFloat() * 0.5f
            val alphaBase = 0.1f + rnd.nextFloat() * 0.16f

            val rise = (((phase / (2f * PI.toFloat())) + startDelay) % 1f + 1f) % 1f
            val y = h * (0.92f - rise * riseSpeed * 6f)
            val fade = (1f - rise).coerceIn(0f, 1f)
            val sway = sin(drift * swayFreq) * swayAmp
            val x = (((x0 + sway) % 1f + 1f) % 1f) * w
            val rw = widthFrac * w * (0.7f + rise * 0.6f)
            val rh = heightFrac * h * (0.7f + rise * 0.6f)
            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) + 232) / 2)
            val g = ((Color.green(tint) + 242) / 2)
            val b = ((Color.blue(tint) + 255) / 2).coerceAtMost(255)
            val alpha = (alphaBase * fade * dim * 255).toInt().coerceIn(0, 255)
            val radius = maxOf(rw, rh) * 0.6f
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius.coerceAtLeast(1f), paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
