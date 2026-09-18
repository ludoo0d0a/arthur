package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
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

/** Bakes one frozen frame of "Soft Wind Streaks" for Auto/Ambient album art — drifting curved wisps. */
internal object SoftWindStreaksStill {
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

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFE8F1F8.toInt(), 0xFFCBDCE8.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val gust = 0.35f + 0.65f * ((sin(drift.toDouble()).toFloat() + 1f) / 2f)

        val blurRadius = (size * 0.02f).coerceAtLeast(3f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        val streakCount = 9
        val dim = 0.7f + 0.3f * pulse
        for (i in 0 until streakCount) {
            val x0 = rnd.nextFloat()
            val yFrac = 0.08f + rnd.nextFloat() * 0.84f
            val lengthFrac = 0.14f + rnd.nextFloat() * 0.16f
            val thicknessFrac = 0.006f + rnd.nextFloat() * 0.01f
            val curveFrac = 0.02f + rnd.nextFloat() * 0.04f
            val speedMul = 0.05f + rnd.nextFloat() * 0.1f
            val alphaBase = 0.16f + rnd.nextFloat() * 0.24f

            val x = (((x0 + drift * speedMul * gust * 0.05f) % 1f) + 1f) % 1f * w
            val y = yFrac * h
            val len = lengthFrac * w
            val amp = curveFrac * h * gust
            val startX = x - len * 0.5f
            val endX = x + len * 0.5f

            val path = Path()
            path.moveTo(startX, y)
            path.quadTo(x, y - amp, endX, y)

            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 245) / 2
            val g = (Color.green(tint) + 248) / 2
            val b = (Color.blue(tint) + 252) / 2
            paint.strokeWidth = (thicknessFrac * h).coerceAtLeast(1f)
            paint.color = Color.argb((alphaBase * dim * 255).toInt().coerceIn(0, 255), r, g, b)
            canvas.drawPath(path, paint)
        }

        paint.maskFilter = null
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
