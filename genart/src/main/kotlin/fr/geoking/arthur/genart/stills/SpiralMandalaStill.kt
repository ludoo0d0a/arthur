package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Spiral Mandala for Auto/Ambient album art. */
internal object SpiralMandalaStill {
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
        val minDim = w.coerceAtMost(h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = minDim * 0.46f
        val foldCount = 5 + rnd.nextInt(4)
        val segmentCount = 160
        val spiralB = 0.18f + rnd.nextFloat() * 0.14f
        val dualOffset = 0.12f + rnd.nextFloat() * 0.16f
        val breath = 0.82f + 0.18f * pulse
        val dim = 0.65f + 0.35f * pulse
        val rotDeg = phase * 360f + rotationDeg

        paint.color = 0xFF06040F.toInt()
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.maskFilter = null
        canvas.drawRect(0f, 0f, w, h, paint)

        val tint = palette.colorAt(0)
        paint.maskFilter = BlurMaskFilter((minDim * 0.08f).coerceAtLeast(8f), BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, minDim * 0.38f,
            intArrayOf(
                Color.argb((0.4f * dim * 255).toInt().coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint)),
                Color.argb((0.1f * dim * 255).toInt().coerceIn(0, 255), Color.red(tint), Color.green(tint), Color.blue(tint)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.38f, paint)
        paint.maskFilter = null
        paint.shader = null

        canvas.save()
        canvas.rotate(rotDeg, cx, cy)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val foldAngle = 2f * PI.toFloat() / foldCount
        for (fold in 0 until foldCount) {
            val ink = palette.colorAt(fold)
            val ir = (Color.red(ink) + 232) / 2
            val ig = (Color.green(ink) + 213) / 2
            val ib = (Color.blue(ink) + 163) / 2
            val widthScale = 0.7f + rnd.nextFloat() * 0.5f
            val baseAngle = fold * foldAngle
            for (phaseTwin in 0..1) {
                val phaseShift = if (phaseTwin == 0) 0f else dualOffset * 2f * PI.toFloat()
                val alpha = ((if (phaseTwin == 0) 0.72f else 0.38f) * dim * 255).toInt().coerceIn(0, 255)
                paint.color = Color.argb(alpha, ir, ig, ib)
                var prevX = 0f
                var prevY = 0f
                for (s in 0 until segmentCount) {
                    val u = s / (segmentCount - 1).toFloat()
                    val theta = u * u * 5.2f * PI.toFloat()
                    val r = maxR * (1f - exp(-spiralB * theta).toFloat())
                    val a = baseAngle + theta + phaseShift
                    val x = cx + cos(a) * r
                    val y = cy + sin(a) * r
                    if (s > 0) {
                        val taper = (1f - u * 0.55f).coerceIn(0.35f, 1f)
                        paint.strokeWidth = (1.4f + 3.8f * widthScale * breath * taper) *
                            (minDim / 420f).coerceIn(0.6f, 1.8f)
                        canvas.drawLine(prevX, prevY, x, y, paint)
                    }
                    prevX = x
                    prevY = y
                }
            }
        }

        paint.color = Color.argb((0.35f * dim * 255).toInt().coerceIn(0, 255), 240, 224, 176)
        paint.strokeWidth = maxR * 0.012f
        canvas.drawCircle(cx, cy, maxR * 0.06f, paint)
        canvas.restore()
    }
}
