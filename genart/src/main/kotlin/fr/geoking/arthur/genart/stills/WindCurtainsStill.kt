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
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Wind Curtains for Auto/Ambient album art. */
internal object WindCurtainsStill {
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
        val minDim = minOf(w, h)

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.4f, (minDim * 0.9f).coerceAtLeast(1f),
            intArrayOf(0xFF1A1420.toInt(), 0xFF0A0810.toInt(), 0xFF040308.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(30, 232, 213, 184),
                Color.argb(46, 242, 230, 208),
                Color.argb(30, 232, 213, 184),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.2f
        val gust = 0.35f + 0.65f * pulse
        val dim = 0.65f + 0.35f * pulse
        val panelCount = 4 + rnd.nextInt(3)
        val foldsPerPanel = 3 + rnd.nextInt(2)
        val segments = 32

        for (i in 0 until panelCount) {
            val x0 = i / panelCount.toFloat()
            val widthFrac = 1.1f / panelCount
            val panelPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val speedMul = 0.45f + rnd.nextFloat() * 0.65f
            val swayAmp = 0.018f + rnd.nextFloat() * 0.027f
            val rippleCycles = 1.4f + rnd.nextFloat() * 1.4f
            val colorMix = rnd.nextFloat()
            val alpha = 0.28f + rnd.nextFloat() * 0.24f
            val tint = mixArgb(palette.colorAt(i), palette.colorAt(i + 1), colorMix)
            val r = Color.red(tint)
            val g = Color.green(tint)
            val b = Color.blue(tint)

            for (f in 0 until foldsPerPanel) {
                val offset = (f + 0.5f) / foldsPerPanel
                val ampFrac = 0.35f + rnd.nextFloat() * 0.65f
                val foldPhase = rnd.nextFloat() * 2f * PI.toFloat()
                val baseX = (x0 + offset * widthFrac) * w
                val half = widthFrac * w * 0.22f
                val sway = swayAmp * w * gust * ampFrac

                path.reset()
                for (k in 0..segments) {
                    val u = k / segments.toFloat()
                    val y = u * h
                    val hang = u * u
                    val ripple = sin(u * rippleCycles * 2f * PI.toFloat() + time * speedMul + panelPhase + foldPhase)
                    val x = baseX - half + sway * hang * ripple
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                for (k in segments downTo 0) {
                    val u = k / segments.toFloat()
                    val y = u * h
                    val hang = u * u
                    val ripple = sin(u * rippleCycles * 2f * PI.toFloat() + time * speedMul + panelPhase + foldPhase + 0.4f)
                    val x = baseX + half + sway * hang * ripple
                    path.lineTo(x, y)
                }
                path.close()

                paint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.argb((alpha * 0.9f * dim * 255).toInt().coerceIn(0, 255), (r + 255) / 2, (g + 255) / 2, (b + 255) / 2),
                        Color.argb((alpha * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                        Color.argb((alpha * 0.7f * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawPath(path, paint)
                paint.shader = null
                paint.color = Color.argb((0.08f * dim * ampFrac * 255).toInt().coerceIn(0, 255), 0, 0, 0)
                canvas.drawPath(path, paint)
            }
        }

        paint.alpha = 255
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }
}
