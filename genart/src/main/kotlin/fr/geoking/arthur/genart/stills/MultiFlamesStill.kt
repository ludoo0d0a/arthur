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

/** Bakes one frozen frame of Multiple Fires for Auto/Ambient album art. */
internal object MultiFlamesStill {
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
        val time = phase * 2f * PI.toFloat() + rotationDeg * PI.toFloat() / 180f * 0.3f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF060308.toInt(), 0xFF120805.toInt(), 0xFF1A0C06.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val clusterCount = 3
        for (c in 0 until clusterCount) {
            val cx = (0.18f + rnd.nextFloat() * 0.64f) * w
            val cy = (0.62f + rnd.nextFloat() * 0.24f) * h
            val scale = 0.7f + rnd.nextFloat() * 0.4f
            val breathPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val breath = 0.85f + 0.15f * sin(time * 0.3f + breathPhase)

            val glowR = minDim * 0.12f * scale
            paint.shader = RadialGradient(
                cx, cy, glowR * 2.2f,
                intArrayOf(
                    Color.argb((90 * dim * breath).toInt().coerceIn(0, 255), 255, 136, 68),
                    Color.argb((30 * dim).toInt(), 255, 102, 34),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, glowR * 2.2f, paint)
            paint.shader = null
            paint.color = Color.argb((200 * dim).toInt(), 42, 24, 16)
            canvas.drawOval(cx - glowR * 0.7f, cy - glowR * 0.12f, cx + glowR * 0.7f, cy + glowR * 0.16f, paint)

            for (i in 0 until 3) {
                val offsetX = (rnd.nextFloat() - 0.5f) * 0.08f * w
                val widthFrac = (0.025f + rnd.nextFloat() * 0.03f) * scale
                val heightFrac = (0.12f + rnd.nextFloat() * 0.16f) * scale
                val flickerPhase = rnd.nextFloat() * 2f * PI.toFloat()
                val flicker = 0.75f + 0.25f * sin(time * 1.2f + flickerPhase)
                val baseX = cx + offsetX
                val height = heightFrac * breath * minDim
                val halfWidth = widthFrac * minDim * flicker
                val tipY = cy - height
                val tint = palette.colorAt(c + i)
                val r = (Color.red(tint) * 0.5f + 255f * 0.5f).toInt().coerceIn(0, 255)
                val g = (Color.green(tint) * 0.5f + 170f * 0.5f).toInt().coerceIn(0, 255)
                val b = (Color.blue(tint) * 0.5f + 68f * 0.5f).toInt().coerceIn(0, 255)
                paint.shader = LinearGradient(
                    baseX, tipY, baseX, cy,
                    intArrayOf(
                        Color.argb(0, r, g, b),
                        Color.argb((100 * dim).toInt(), r, g, b),
                        Color.argb((200 * dim).toInt(), r, g, b),
                        Color.argb((40 * dim).toInt(), r, g, b),
                    ),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f),
                    Shader.TileMode.CLAMP,
                )
                val path = Path().apply {
                    moveTo(baseX - halfWidth, cy)
                    cubicTo(
                        baseX - halfWidth * 1.25f, tipY + height * 0.35f,
                        baseX + halfWidth * 1.25f, tipY + height * 0.35f,
                        baseX + halfWidth * 0.15f, tipY,
                    )
                    cubicTo(
                        baseX + halfWidth * 0.9f, tipY + height * 0.25f,
                        baseX - halfWidth * 0.9f, tipY + height * 0.25f,
                        baseX - halfWidth, cy,
                    )
                    close()
                }
                canvas.drawPath(path, paint)
                paint.shader = null
            }

            for (e in 0 until 6) {
                val life = ((rnd.nextFloat() + phase * 0.3f) % 1f + 1f) % 1f
                val rise = life * minDim * 0.2f * scale
                val ex = cx + (rnd.nextFloat() - 0.5f) * 0.1f * w
                val ey = cy - rise
                val fade = (life / 0.12f).coerceIn(0f, 1f) * ((1f - life) / 0.25f).coerceIn(0f, 1f)
                val er = 1.5f + rnd.nextFloat() * 3f
                paint.shader = RadialGradient(
                    ex, ey, er * 2.2f,
                    Color.argb((140 * fade * dim).toInt().coerceIn(0, 255), 255, 204, 102),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(ex, ey, er * 2.2f, paint)
                paint.shader = null
            }
        }
        paint.alpha = 255
    }
}
