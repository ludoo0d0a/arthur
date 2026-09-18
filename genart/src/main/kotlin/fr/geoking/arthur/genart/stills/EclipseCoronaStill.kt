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
import kotlin.random.Random

/** Bakes one frozen frame of "Eclipse Corona" for Auto/Ambient album art — starfield, blurred corona ring, dark disc. */
internal object EclipseCoronaStill {
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

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF02030A.toInt(), Color.BLACK),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val starCount = 44
        for (i in 0 until starCount) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val x = (((x0 + drift * 0.01f) % 1f) + 1f) % 1f * w
            val y = y0 * h
            val radius = (0.004f + rnd.nextFloat() * 0.006f) * minDim
            val alpha = 0.25f + rnd.nextFloat() * 0.45f
            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(x, y, radius, paint)
        }

        val cx = w * 0.5f
        val cy = h * 0.5f
        val discRadius = minDim * 0.22f
        val coronaRadius = minDim * 0.42f

        // Corona shimmer capped tight (alpha ~0.5..0.7), never a hard flash — matches Storm's glow-pulse pattern.
        val ringAlpha = 0.5f + 0.2f * pulse
        val tint = palette.colorAt(0)
        val ringR = (Color.red(tint) + 255 * 2) / 3
        val ringG = (Color.green(tint) + 243 * 2) / 3
        val ringB = (Color.blue(tint) + 214 * 2) / 3

        val blurRadius = (size * 0.04f).coerceAtLeast(6f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, coronaRadius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((ringAlpha * 255).toInt().coerceIn(0, 255), ringR, ringG, ringB),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, (discRadius / coronaRadius).coerceIn(0.05f, 0.95f), 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, coronaRadius, paint)
        paint.shader = null
        paint.maskFilter = null

        paint.color = Color.argb(255, 5, 6, 10)
        canvas.drawCircle(cx, cy, discRadius, paint)
        paint.alpha = 255
    }
}
