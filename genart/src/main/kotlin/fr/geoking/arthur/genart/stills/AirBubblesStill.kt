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
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Underwater Air Bubbles for Auto/Ambient album art. */
internal object AirBubblesStill {
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
        val dim = 0.65f + 0.35f * pulse
        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A3A52.toInt(), 0xFF041820.toInt(), 0xFF020A10.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val blurRadius = (size * 0.02f).coerceAtLeast(3f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        val originX = w * 0.5f
        val originY = -h * 0.05f
        val reach = h * 1.35f
        val shaftTime = loop * 2f * PI.toFloat()
        for (i in 0 until 4) {
            val angle = -0.3f + rnd.nextFloat() * 0.6f
            val width = 0.03f + rnd.nextFloat() * 0.04f
            val sway = 0.01f + rnd.nextFloat() * 0.015f
            val a = angle + sway * sin(shaftTime * (0.1f + rnd.nextFloat() * 0.15f))
            val leftX = originX + sin(a - width) * reach
            val leftY = originY + cos(a - width) * reach
            val rightX = originX + sin(a + width) * reach
            val rightY = originY + cos(a + width) * reach
            val path = Path().apply {
                moveTo(originX, originY)
                lineTo(leftX, leftY)
                lineTo(rightX, rightY)
                close()
            }
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 62) / 2
            val g = (Color.green(tint) + 200) / 2
            val b = (Color.blue(tint) + 232) / 2
            val alpha = (0.1f * dim * 255).toInt().coerceIn(0, 255)
            paint.shader = LinearGradient(
                originX, originY, (leftX + rightX) * 0.5f, (leftY + rightY) * 0.5f,
                intArrayOf(
                    Color.argb(alpha, r, g, b),
                    Color.argb((alpha * 0.3f).toInt(), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }
        paint.maskFilter = null

        for (i in 0 until 30) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val riseSpeed = 0.2f + rnd.nextFloat() * 0.5f
            val baseRadius = 4f + rnd.nextFloat() * 10f
            val wobbleAmp = 0.015f + rnd.nextFloat() * 0.035f
            val wobbleFreq = 0.4f + rnd.nextFloat() * 1.2f
            val wobblePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val highlightAngle = rnd.nextFloat() * 2f * PI.toFloat()
            val raw = loop * riseSpeed + y0
            val life = raw - floor(raw)
            val depth = 1f - life
            val y = depth * h
            val wobble = sin(loop * 2f * PI.toFloat() * wobbleFreq + wobblePhase) * wobbleAmp
            val x = (((x0 + wobble) % 1f) + 1f) % 1f * w
            val grow = 1f + (1f - depth) * 0.85f
            val radius = baseRadius * grow
            val fade = (life / 0.08f).coerceIn(0f, 1f) * ((1f - life) / 0.15f).coerceIn(0f, 1f)
            val depthFade = 0.45f + 0.55f * (1f - depth * 0.65f)
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 184) / 2
            val g = (Color.green(tint) + 232) / 2
            val b = (Color.blue(tint) + 248) / 2
            val alpha = (0.42f * fade * depthFade * dim * 255).toInt().coerceIn(0, 255)

            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                x - radius * 0.2f, y - radius * 0.25f, radius,
                intArrayOf(
                    Color.argb((alpha * 0.22f).toInt(), 255, 255, 255),
                    Color.argb((alpha * 0.28f).toInt(), r, g, b),
                    Color.argb((alpha * 0.08f).toInt(), r, g, b),
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f
            paint.color = Color.argb(alpha, r, g, b)
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.FILL
            val hx = x + cos(highlightAngle) * radius * 0.35f
            val hy = y + sin(highlightAngle) * radius * 0.35f
            paint.color = Color.argb((alpha * 0.7f).toInt(), 255, 255, 255)
            canvas.drawCircle(hx, hy, radius * 0.22f, paint)
            paint.color = Color.argb((alpha * 0.35f).toInt(), 255, 255, 255)
            canvas.drawCircle(
                x + cos(highlightAngle + 2.2f) * radius * 0.4f,
                y + sin(highlightAngle + 2.2f) * radius * 0.4f,
                radius * 0.1f,
                paint,
            )
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
