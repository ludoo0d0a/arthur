package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.random.Random

/** Bakes one frozen frame of Ink in Water for Auto/Ambient album art. */
internal object InkInWaterStill {
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
        val minDim = minOf(w, h)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.85f,
            intArrayOf(0xFF0A1220.toInt(), 0xFF03050C.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * kotlin.math.PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.6f + 0.4f * pulse

        val count = 3
        for (i in 0 until count) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val phaseOffset = rnd.nextFloat()
            val cycleFrac = 0.7f + rnd.nextFloat() * 0.6f
            val maxRadiusFrac = 0.22f + rnd.nextFloat() * 0.2f
            val bloom = (((loop * cycleFrac + phaseOffset) % 1f) + 1f) % 1f
            val radius = (bloom * maxRadiusFrac * minDim).coerceAtLeast(1f)
            val alpha = (1f - bloom).coerceIn(0f, 1f)
            val x = x0 * w
            val y = y0 * h
            val baseTint = palette.colorAt(i)
            val ink = mixColors(baseTint, 0xFF05030F.toInt(), 0.65f)
            val edge = mixColors(mixColors(baseTint, 0xFF05030F.toInt(), 0.65f), 0xFF2A2050.toInt(), 0.5f)
            val coreAlpha = (alpha * 0.55f * dim * 255).toInt().coerceIn(0, 255)
            val edgeAlpha = (alpha * 0.28f * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                x, y, radius,
                intArrayOf(
                    Color.argb(coreAlpha, Color.red(ink), Color.green(ink), Color.blue(ink)),
                    Color.argb(edgeAlpha, Color.red(edge), Color.green(edge), Color.blue(edge)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }

    private fun mixColors(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = Color.red(a) + (Color.red(b) - Color.red(a)) * u
        val g = Color.green(a) + (Color.green(b) - Color.green(a)) * u
        val bl = Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u
        return Color.rgb(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), bl.toInt().coerceIn(0, 255))
    }
}
