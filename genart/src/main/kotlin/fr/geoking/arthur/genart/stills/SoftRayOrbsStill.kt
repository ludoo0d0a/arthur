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
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Raymarch Orbs for Auto/Ambient album art. */
internal object SoftRayOrbsStill {
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
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val angle = (phase + rotationDeg * PI.toFloat() / 180f * 0.2f)

        paint.color = 0xFF050814.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.maskFilter = BlurMaskFilter(minDim * 0.04f, BlurMaskFilter.Blur.NORMAL)
        for (i in 0 until 3) {
            val a = angle * (0.35f + i * 0.12f) + i * 2.1f
            val orbit = minDim * (0.18f + i * 0.05f)
            val cx = w * 0.5f + cos(a) * orbit
            val cy = h * 0.48f + sin(a * 0.9f) * orbit * 0.55f
            val r = minDim * (0.14f - i * 0.02f)
            val tint = palette.colorAt(i)
            paint.shader = RadialGradient(
                cx - r * 0.25f, cy - r * 0.3f, r * 1.2f,
                intArrayOf(
                    Color.argb((140 * dim).toInt(), 255, 255, 255),
                    Color.argb((110 * dim).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r * 1.2f, paint)
            paint.shader = null
        }
        paint.maskFilter = null

        for (i in 0 until 3) {
            val a = angle * (0.35f + i * 0.12f) + i * 2.1f + rnd.nextFloat() * 0.01f
            val orbit = minDim * (0.18f + i * 0.05f)
            val cx = w * 0.5f + cos(a) * orbit
            val cy = h * 0.48f + sin(a * 0.9f) * orbit * 0.55f
            val r = minDim * (0.12f - i * 0.018f)
            val tint = palette.colorAt(i)
            paint.shader = RadialGradient(
                cx - r * 0.3f, cy - r * 0.35f, r,
                intArrayOf(
                    Color.argb((180 * dim).toInt(), 255, 255, 255),
                    Color.argb((140 * dim).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb((200 * dim).toInt(), 16, 24, 40),
                    0xFF050814.toInt(),
                ),
                floatArrayOf(0f, 0.4f, 0.85f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = RadialGradient(
                cx - r * 0.35f, cy - r * 0.4f, r * 0.22f,
                intArrayOf(Color.argb((170 * dim).toInt(), 255, 255, 255), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.4f, r * 0.22f, paint)
            paint.shader = null
        }
    }
}
