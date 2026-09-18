package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of "Flower Bloom" for Android Auto album art — deterministic per [generation]. */
internal object FlowerStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF12140F.toInt(), 0xFF1B2115.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val swayTime = phase * 2f * PI.toFloat() + rotationDeg * PI.toFloat() / 180f
        val swayAngle = sin(swayTime) * 0.05f
        val breathFactor = 1f + sin(pulse * PI.toFloat()) * 0.06f

        val baseX = w * 0.5f
        val baseY = h * 0.92f
        val stemHeight = h * 0.5f
        val tipX = baseX + sin(swayAngle) * stemHeight
        val tipY = baseY - cos(swayAngle) * stemHeight
        val controlX = baseX + sin(swayAngle) * stemHeight * 0.55f
        val controlY = baseY - stemHeight * 0.55f

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.010f
        paint.color = Color.argb(217, 0x5C, 0x8A, 0x44)
        path.reset()
        path.moveTo(baseX, baseY)
        path.quadTo(controlX, controlY, tipX, tipY)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        val petalCount = 6
        val bloomRadius = minDim * 0.16f * breathFactor
        val petalRadius = minDim * 0.11f * breathFactor
        val pastels = intArrayOf(0xFFF7C6D9.toInt(), 0xFFD8C6F7.toInt(), 0xFFF7EFC6.toInt())

        for (i in 0 until petalCount) {
            val angle = (i.toFloat() / petalCount) * 2f * PI.toFloat()
            val cx = tipX + cos(angle) * bloomRadius
            val cy = tipY + sin(angle) * bloomRadius
            val tint = mixColors(palette.colorAt(i), pastels[i % 3], 0.6f)
            paint.shader = RadialGradient(
                cx, cy, petalRadius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(140, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb(0, Color.red(tint), Color.green(tint), Color.blue(tint)),
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, petalRadius, paint)
            paint.shader = null
        }

        val centerRadius = minDim * 0.045f * breathFactor
        paint.shader = RadialGradient(
            tipX, tipY, centerRadius.coerceAtLeast(1f),
            intArrayOf(Color.argb(191, 0xF2, 0xC9, 0x4C), Color.argb(0, 0xF2, 0xC9, 0x4C)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(tipX, tipY, centerRadius, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun mixColors(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bch = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bch)
    }
}
