package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
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
import kotlin.random.Random

/** Bakes one frozen frame of Lava Sun for Auto/Ambient album art. */
internal object LavaSunStill {
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
        val sunR = minDim * 0.28f
        val dim = 0.65f + 0.35f * pulse
        val time = phase + rotationDeg * PI.toFloat() / 180f

        paint.color = 0xFF080204.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val tint = palette.colorAt(0)
        val cr = (Color.red(tint) + 255) / 2
        val cg = (Color.green(tint) + 170) / 2
        val cb = (Color.blue(tint) + 68) / 2

        paint.maskFilter = BlurMaskFilter((sunR * 0.4f).coerceAtLeast(8f), BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, sunR * 2.4f,
            intArrayOf(
                Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), cr, cg, cb),
                Color.argb((0.15f * dim * 255).toInt().coerceIn(0, 255), cr, cg, cb),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, sunR * 2.4f, paint)
        paint.maskFilter = null
        paint.shader = null

        for (i in 0 until 6) {
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val lengthFrac = 0.18f + rnd.nextFloat() * 0.24f
            val widthFrac = 0.04f + rnd.nextFloat() * 0.06f
            val sway = sin(time * (0.8f + rnd.nextFloat()) + angle) * 0.12f
            val a = angle + sway
            val length = lengthFrac * minDim
            val halfW = widthFrac * minDim
            val baseX = cx + cos(a).toFloat() * sunR * 0.92f
            val baseY = cy + sin(a).toFloat() * sunR * 0.92f
            val tipX = cx + cos(a).toFloat() * (sunR + length)
            val tipY = cy + sin(a).toFloat() * (sunR + length)
            val perpX = (-sin(a)).toFloat()
            val perpY = cos(a).toFloat()
            val flame = palette.colorAt(i)
            val fr = (Color.red(flame) + 255) / 2
            val fg = (Color.green(flame) + 100) / 2
            val fb = (Color.blue(flame) + 34) / 2

            paint.shader = LinearGradient(
                baseX, baseY, tipX, tipY,
                intArrayOf(
                    Color.argb((0.85f * dim * 255).toInt().coerceIn(0, 255), 255, 238, 170),
                    Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), fr, fg, fb),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            val path = Path().apply {
                moveTo(baseX + perpX * halfW, baseY + perpY * halfW)
                cubicTo(
                    baseX + perpX * halfW * 1.4f + cos(a).toFloat() * length * 0.35f,
                    baseY + perpY * halfW * 1.4f + sin(a).toFloat() * length * 0.35f,
                    tipX + perpX * halfW * 0.2f,
                    tipY + perpY * halfW * 0.2f,
                    tipX, tipY,
                )
                cubicTo(
                    tipX - perpX * halfW * 0.2f,
                    tipY - perpY * halfW * 0.2f,
                    baseX - perpX * halfW * 1.4f + cos(a).toFloat() * length * 0.35f,
                    baseY - perpY * halfW * 1.4f + sin(a).toFloat() * length * 0.35f,
                    baseX - perpX * halfW,
                    baseY - perpY * halfW,
                )
                close()
            }
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        paint.shader = RadialGradient(
            cx - sunR * 0.2f, cy - sunR * 0.2f, sunR * 1.2f,
            intArrayOf(0xFFFFF0C8.toInt(), 0xFFFFB84A.toInt(), 0xFFFF6A22.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, sunR, paint)
        paint.shader = null

        for (i in 0 until 6) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val d = (0.15f + rnd.nextFloat() * 0.55f) * sunR
            val r = (0.08f + rnd.nextFloat() * 0.1f) * sunR
            paint.color = Color.argb((0.25f * dim * 255).toInt().coerceIn(0, 255), 255, 68, 17)
            canvas.drawCircle(cx + cos(a).toFloat() * d, cy + sin(a).toFloat() * d, r, paint)
        }
        paint.alpha = 255
    }
}
