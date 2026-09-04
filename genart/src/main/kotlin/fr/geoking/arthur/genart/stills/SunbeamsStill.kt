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

/** Bakes one frozen frame of Sunbeams Through Haze for Auto/Ambient album art. */
internal object SunbeamsStill {
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
            intArrayOf(0xFF1A1408.toInt(), 0xFF08060A.toInt(), 0xFF040408.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val originX = w * 0.5f
        val originY = -h * 0.05f
        val reach = h * 1.35f
        val dim = 0.65f + 0.35f * pulse
        val swayBase = phase + rotationDeg * PI.toFloat() / 180f * 0.2f

        for (i in 0 until 7) {
            val angle = -0.55f + rnd.nextFloat() * 1.1f
            val width = 0.04f + rnd.nextFloat() * 0.06f
            val swayAmp = 0.02f + rnd.nextFloat() * 0.04f
            val swayFreq = 0.15f + rnd.nextFloat() * 0.35f
            val alphaBase = 0.06f + rnd.nextFloat() * 0.12f
            val a = angle + swayAmp * sin(swayBase * swayFreq)
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
            val r = ((Color.red(tint) + 245) / 2)
            val g = ((Color.green(tint) + 215) / 2)
            val b = ((Color.blue(tint) + 138) / 2)
            val midX = (leftX + rightX) * 0.5f
            val midY = (leftY + rightY) * 0.5f
            paint.shader = LinearGradient(
                originX, originY, midX, midY,
                Color.argb((alphaBase * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }
        val glowR = minOf(w, h) * 0.35f
        paint.shader = RadialGradient(
            originX, originY, glowR.coerceAtLeast(1f),
            Color.argb((0.35f * dim * 255).toInt(), 255, 240, 200),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(originX, originY, glowR, paint)
        paint.shader = null
        paint.alpha = 255
    }
}
