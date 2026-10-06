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

/** Bakes one frozen frame of Underwater Light Shafts for Auto/Ambient album art. */
internal object DepthShaftsStill {
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
        val dim = 0.65f + 0.35f * pulse
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.2f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF04263A.toInt(), 0xFF01121F.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val originX = w * 0.5f
        val originY = -h * 0.04f
        val reach = h * 1.4f

        for (i in 0 until 6) {
            val angle = -0.45f + rnd.nextFloat() * 0.9f
            val width = 0.035f + rnd.nextFloat() * 0.055f
            val swayAmp = 0.015f + rnd.nextFloat() * 0.03f
            val swayFreq = 0.12f + rnd.nextFloat() * 0.28f
            val alphaBase = 0.07f + rnd.nextFloat() * 0.13f
            val a = angle + swayAmp * sin(time * swayFreq)
            val leftX = originX + sin(a - width) * reach
            val leftY = originY + cos(a - width) * reach
            val rightX = originX + sin(a + width) * reach
            val rightY = originY + cos(a + width) * reach
            path.reset()
            path.moveTo(originX, originY)
            path.lineTo(leftX, leftY)
            path.lineTo(rightX, rightY)
            path.close()
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 158) / 2
            val g = (Color.green(tint) + 232) / 2
            val b = (Color.blue(tint) + 255) / 2
            val midX = (leftX + rightX) * 0.5f
            val midY = (leftY + rightY) * 0.5f
            paint.shader = LinearGradient(
                originX, originY, midX, midY,
                intArrayOf(
                    Color.argb((alphaBase * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.argb((alphaBase * 0.35f * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        val glowR = (minDim * 0.32f).coerceAtLeast(1f)
        paint.shader = RadialGradient(
            originX, originY, glowR,
            Color.argb((0.3f * dim * 255).toInt().coerceIn(0, 255), 200, 240, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(originX, originY, glowR, paint)
        paint.shader = null

        val blurRadius = (0.012f * minDim).coerceAtLeast(3f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        for (i in 0 until 12) {
            val xFrac = rnd.nextFloat()
            val yFrac = 0.25f + rnd.nextFloat() * 0.6f
            val lenFrac = 0.1f + rnd.nextFloat() * 0.14f
            val curveFrac = 0.025f + rnd.nextFloat() * 0.055f
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val thicknessFrac = 0.0035f + rnd.nextFloat() * 0.0055f
            val shimmerFreq = 0.25f + rnd.nextFloat() * 0.45f
            val shimmerPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val cx = xFrac * w
            val cy = yFrac * h
            val len = lenFrac * minDim
            val curve = curveFrac * minDim
            val dirX = sin(angle)
            val dirY = sin(angle + PI.toFloat() / 2f)
            val normX = -dirY
            val normY = dirX
            path.reset()
            path.moveTo(cx - dirX * len / 2f, cy - dirY * len / 2f)
            path.quadTo(cx + normX * curve, cy + normY * curve, cx + dirX * len / 2f, cy + dirY * len / 2f)
            val shimmer = (sin(time * shimmerFreq + shimmerPhase) + 1f) / 2f
            val alpha = (0.14f + 0.2f * shimmer) * dim
            val tint = palette.colorAt(i)
            paint.strokeWidth = thicknessFrac * minDim
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                (Color.red(tint) + 3 * 0x7F) / 4,
                (Color.green(tint) + 3 * 0xE9) / 4,
                (Color.blue(tint) + 3 * 0xFF) / 4,
            )
            canvas.drawPath(path, paint)
        }
        paint.maskFilter = null
        paint.style = Paint.Style.FILL

        for (j in 0 until 55) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val twinkle = 0.3f + 0.7f * rnd.nextFloat()
            val depthFade = 0.35f + 0.65f * (1f - y / h)
            val moteR = (minDim * (0.0012f + rnd.nextFloat() * 0.0033f) * (0.7f + 0.5f * twinkle))
                .coerceAtLeast(0.5f)
            paint.color = Color.argb(
                (0.45f * twinkle * depthFade * dim * 255).toInt().coerceIn(0, 255),
                184, 232, 248,
            )
            canvas.drawCircle(x, y, moteR, paint)
        }
        paint.alpha = 255
    }
}
