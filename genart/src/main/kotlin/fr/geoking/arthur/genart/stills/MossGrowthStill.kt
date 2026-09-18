package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Moss Growth for Auto/Ambient album art. */
internal object MossGrowthStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private const val MOSS_GREEN = 0xFF4C6B3A.toInt()

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
        val minDim = minOf(w, h)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.85f,
            intArrayOf(0xFF2B2620.toInt(), 0xFF171410.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val dim = 0.6f + 0.4f * pulse

        for (i in 0 until 10) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val radiusBaseFrac = 0.05f + rnd.nextFloat() * 0.04f
            val radiusGrowFrac = 0.06f + rnd.nextFloat() * 0.08f
            val growPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val colorMixT = 0.35f + rnd.nextFloat() * 0.4f
            val alphaBase = 0.22f + rnd.nextFloat() * 0.18f
            val patchPulse = ((sin(time * 2f * PI.toFloat() + growPhase) + 1.0) / 2.0).toFloat()
            val radius = ((radiusBaseFrac + radiusGrowFrac * patchPulse) * minDim).coerceAtLeast(1f)
            val base = palette.colorAt(i)
            val tint = mixColor(base, MOSS_GREEN, colorMixT)
            val alpha = (alphaBase * (0.6f + 0.4f * patchPulse) * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius,
                intArrayOf(
                    Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((alpha * 0.35f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }

    private fun mixColor(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = Color.red(a) + (Color.red(b) - Color.red(a)) * u
        val g = Color.green(a) + (Color.green(b) - Color.green(a)) * u
        val bl = Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u
        return Color.rgb(r.toInt(), g.toInt(), bl.toInt())
    }
}
