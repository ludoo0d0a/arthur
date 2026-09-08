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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Sunshine for Auto/Ambient album art. */
internal object SunshineStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF8ED6FB.toInt(), 0xFF5CB3EA.toInt(), 0xFF2E86D4.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val sunX = w * 0.82f
        val sunY = h * 0.18f
        val reach = maxOf(w, h) * 1.3f
        val dim = 0.75f + 0.25f * pulse
        val swayBase = phase + rotationDeg * PI.toFloat() / 180f * 0.2f

        for (i in 0 until 7) {
            val angle = -1.8f + rnd.nextFloat() * 3.6f
            val widthHalf = 0.1f + rnd.nextFloat() * 0.1f
            val swayAmp = 0.015f + rnd.nextFloat() * 0.03f
            val swayFreq = 0.08f + rnd.nextFloat() * 0.2f
            val alphaBase = 0.05f + rnd.nextFloat() * 0.07f
            val a = -PI.toFloat() / 4f + angle + swayAmp * sin(swayBase * swayFreq)
            val leftX = sunX + sin(a - widthHalf) * reach
            val leftY = sunY + cos(a - widthHalf) * reach
            val rightX = sunX + sin(a + widthHalf) * reach
            val rightY = sunY + cos(a + widthHalf) * reach
            path.reset()
            path.moveTo(sunX, sunY)
            path.lineTo(leftX, leftY)
            path.lineTo(rightX, rightY)
            path.close()
            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) + 255 * 3) / 4)
            val g = ((Color.green(tint) + 246 * 3) / 4)
            val b = ((Color.blue(tint) + 217 * 3) / 4)
            val midX = (leftX + rightX) * 0.5f
            val midY = (leftY + rightY) * 0.5f
            paint.shader = LinearGradient(
                sunX, sunY, midX, midY,
                Color.argb((alphaBase * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }
        val glowR = (minOf(w, h) * 0.3f * (0.95f + 0.05f * dim)).coerceAtLeast(1f)
        paint.shader = RadialGradient(
            sunX, sunY, glowR,
            intArrayOf(
                Color.argb((0.9f * dim * 255).toInt().coerceIn(0, 255), 255, 253, 231),
                Color.argb((0.5f * dim * 255).toInt().coerceIn(0, 255), 255, 224, 130),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sunX, sunY, glowR, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
