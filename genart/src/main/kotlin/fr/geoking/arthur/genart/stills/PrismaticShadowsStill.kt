package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Prismatic Shadows for Auto/Ambient album art. */
internal object PrismaticShadowsStill {
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
        paint.color = 0xFF04060A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse

        for (i in 0 until 6) {
            val xA = rnd.nextFloat()
            val yA = rnd.nextFloat()
            val xB = rnd.nextFloat()
            val yB = rnd.nextFloat()
            val wFrac = 0.25f + rnd.nextFloat() * 0.2f
            val hFrac = 0.2f + rnd.nextFloat() * 0.2f
            val rotationA = rnd.nextFloat() * 180f
            val rotationB = 180f + rnd.nextFloat() * 180f
            val shadowDist = 0.04f + rnd.nextFloat() * 0.06f

            val morph = ((sin(loop * 2f * PI.toFloat() + xA * 10f) + 1f) / 2f)
            val cx = (xA + (xB - xA) * morph) * w
            val cy = (yA + (yB - yA) * morph) * h
            val sw = wFrac * minDim
            val sh = hFrac * minDim
            val rot = rotationA + (rotationB - rotationA) * morph
            val shadowOff = shadowDist * minDim

            val c = palette.colorAt(i)

            canvas.save()
            canvas.rotate(rot, cx, cy)

            // Shadow
            val shadowRadius = maxOf(sw, sh) * 0.9f
            paint.shader = RadialGradient(
                cx + shadowOff, cy + shadowOff, shadowRadius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0),
                    Color.argb((0.2f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            val rectShadow = RectF(cx - sw / 2f + shadowOff, cy - sh / 2f + shadowOff, cx + sw / 2f + shadowOff, cy + sh / 2f + shadowOff)
            canvas.drawRoundRect(rectShadow, minDim * 0.05f, minDim * 0.05f, paint)

            // Shape
            paint.shader = LinearGradient(
                cx - sw / 2f, cy - sh / 2f, cx + sw / 2f, cy + sh / 2f,
                Color.argb((0.65f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                Color.argb((0.25f * dim * 255).toInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c)),
                Shader.TileMode.CLAMP,
            )
            val rectShape = RectF(cx - sw / 2f, cy - sh / 2f, cx + sw / 2f, cy + sh / 2f)
            canvas.drawRoundRect(rectShape, minDim * 0.05f, minDim * 0.05f, paint)

            canvas.restore()
            paint.shader = null
        }
        paint.alpha = 255
    }
}
