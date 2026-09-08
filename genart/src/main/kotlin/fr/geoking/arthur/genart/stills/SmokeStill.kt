package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Rising Smoke" for Auto/Ambient album art. */
internal object SmokeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private const val WISP_COUNT = 6

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

        paint.maskFilter = null
        val top = palette.primary
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
                Color.argb(
                    255,
                    (Color.red(top) * 0.18f).toInt().coerceIn(0, 255),
                    (Color.green(top) * 0.18f).toInt().coerceIn(0, 255),
                    (Color.blue(top) * 0.18f).toInt().coerceIn(0, 255),
                ),
                0xFF07080A.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val blurRadius = (size * 0.035f).coerceAtLeast(6f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val pulseMix = 0.85f + pulse * 0.15f

        for (i in 0 until WISP_COUNT) {
            val x0 = rnd.nextFloat()
            val startOffset = rnd.nextFloat()
            val riseSpeedFactor = 0.55f + rnd.nextFloat() * 0.7f
            val radiusBaseFrac = 0.05f + rnd.nextFloat() * 0.06f
            val swayFreq = 0.3f + rnd.nextFloat() * 0.6f
            val swayPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val swayAmpFrac = 0.08f + rnd.nextFloat() * 0.12f
            val alphaBase = 0.14f + rnd.nextFloat() * 0.16f

            val raw = drift * riseSpeedFactor + startOffset
            val heightFrac = raw - floor(raw)
            val sway = sin(drift * swayFreq + swayPhase) * swayAmpFrac * heightFrac * w
            val x = x0 * w + sway
            val y = (1f - heightFrac) * h
            val radius = radiusBaseFrac * minDim * (1f + heightFrac * 2.4f) * pulseMix
            val fadeIn = (heightFrac / 0.1f).coerceIn(0f, 1f)
            val alpha = (alphaBase * (1f - heightFrac) * fadeIn).coerceIn(0f, 1f)

            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) * 0.25f) + (199 * 0.75f)).toInt().coerceIn(0, 255)
            val g = ((Color.green(tint) * 0.25f) + (201 * 0.75f)).toInt().coerceIn(0, 255)
            val b = ((Color.blue(tint) * 0.25f) + (206 * 0.75f)).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                x,
                y,
                radius.coerceAtLeast(1f),
                Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b),
                Color.argb(0, r, g, b),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }
        paint.maskFilter = null
        paint.alpha = 255
    }
}
