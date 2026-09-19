package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.flowAngle01
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Flow Ribbons for Auto/Ambient album art. */
internal object FlowRibbonsStill {
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
        val rnd = Random(generation xor (rotationDeg * 1000f).toLong())
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)
        val ribbonCount = 28
        val samplesPerRibbon = 30
        val dim = 0.6f + 0.4f * pulse
        val fieldDrift = phase * 0.35f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF070B14.toInt(), 0xFF02040A.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        for (i in 0 until ribbonCount) {
            val ink = palette.colorAt(i)
            val ir = Color.red(ink)
            val ig = Color.green(ink)
            val ib = Color.blue(ink)
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val stepFrac = 0.012f + rnd.nextFloat() * 0.016f
            val fieldScale = 1.6f + rnd.nextFloat() * 1.6f
            val fieldSeed = i * 97 + 19
            val widthScale = 0.6f + rnd.nextFloat() * 0.8f

            var x = ((x0 + fieldDrift * 0.15f) % 1f + 1f) % 1f
            var y = ((y0 + phase * 0.05f) % 1f + 1f) % 1f
            var prevX = x * w
            var prevY = y * h
            for (s in 0 until samplesPerRibbon) {
                val nx = x * fieldScale + fieldDrift
                val ny = y * fieldScale
                val angle = flowAngle01(nx, ny, fieldSeed)
                x = ((x + cos(angle) * stepFrac) % 1f + 1f) % 1f
                y = ((y + sin(angle) * stepFrac) % 1f + 1f) % 1f
                val px = x * w
                val py = y * h
                val fade = 1f - s / samplesPerRibbon.toFloat()
                val alpha = ((0.08f + 0.55f * fade) * dim * 255).toInt().coerceIn(0, 255)
                paint.color = Color.argb(alpha, ir, ig, ib)
                paint.strokeWidth = (1.1f + 2.8f * widthScale * fade) *
                    (minDim / 480f).coerceIn(0.55f, 1.6f)
                canvas.drawLine(prevX, prevY, px, py, paint)
                prevX = px
                prevY = py
            }
        }
    }
}
