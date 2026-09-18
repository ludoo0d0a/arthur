package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Warp Streak for Auto/Ambient album art. */
internal object WarpStreakStill {
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF04060E.toInt(), 0xFF010104.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val dim = 0.65f + 0.35f * pulse
        for (i in 0 until 26) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val radius = 0.6f + rnd.nextFloat() * 1.1f
            val alpha = ((0.15f + rnd.nextFloat() * 0.35f) * dim * 255).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(alpha, 0xE8, 0xF1, 0xFF)
            canvas.drawCircle(x, y, radius, paint)
        }

        val centerX = w * 0.5f
        val centerY = h * 0.5f
        val reach = hypot(w, h) * 0.55f
        val sway = phase + rotationDeg * PI.toFloat() / 180f * 0.2f

        for (i in 0 until 12) {
            val angle = (i.toFloat() / 12) * 2f * PI.toFloat() + (-0.08f + rnd.nextFloat() * 0.16f)
            val lengthFrac = 0.55f + rnd.nextFloat() * 0.4f
            val widthBase = 1.1f + rnd.nextFloat() * 1.3f
            val alphaBase = 0.1f + rnd.nextFloat() * 0.16f
            val swayAmp = 0.008f + rnd.nextFloat() * 0.012f
            val swayFreq = 0.05f + rnd.nextFloat() * 0.08f
            val a = angle + swayAmp * sin(sway * swayFreq)
            val length = reach * lengthFrac
            val endX = centerX + sin(a) * length
            val endY = centerY - cos(a) * length
            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) + 0xCF) / 2)
            val g = ((Color.green(tint) + 0xE8) / 2)
            val b = ((Color.blue(tint) + 0xFF) / 2)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = widthBase
            paint.shader = LinearGradient(
                centerX, centerY, endX, endY,
                Color.argb((alphaBase * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawLine(centerX, centerY, endX, endY, paint)
            paint.shader = null
        }
        paint.style = Paint.Style.FILL

        val glowR = minOf(w, h) * 0.18f
        paint.shader = RadialGradient(
            centerX, centerY, glowR.coerceAtLeast(1f),
            Color.argb((0.3f * dim * 255).toInt(), 0xEA, 0xF4, 0xFF),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(centerX, centerY, glowR, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
