package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Heat Haze for Auto/Ambient album art. */
internal object HeatHazeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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

        val groundTop = palette.colorAt(0)
        val groundMid = palette.colorAt(1)
        val groundLow = palette.colorAt(2)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(groundTop, groundMid, groundLow),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val bandCount = 6
        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val pulseMix = 0.9f + pulse * 0.1f
        for (i in 0 until bandCount) {
            val depthFrac = i / (bandCount - 1f)
            val yFrac = 0.42f + depthFrac * 0.5f + (rnd.nextFloat() - 0.5f) * 0.04f
            val thicknessFrac = 0.02f + rnd.nextFloat() * 0.025f
            val widthFrac = 0.82f + rnd.nextFloat() * 0.15f
            val shimmerAmpFrac = 0.01f + rnd.nextFloat() * 0.025f
            val shimmerFreq = 1.4f + rnd.nextFloat() * 1.8f
            val shimmerPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.04f + rnd.nextFloat() * 0.08f

            val shimmer = sin(drift * shimmerFreq + shimmerPhase) * shimmerAmpFrac * pulseMix
            val bandWidth = widthFrac * w
            val centerX = w * 0.5f + shimmer * w
            val y = yFrac * h
            val thickness = thicknessFrac * h
            val tint = palette.colorAt(i)
            val factor = 0.7f + depthFrac * 0.3f
            val alpha = (alphaBase * 255).toInt().coerceIn(0, 36)

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(
                alpha,
                (Color.red(tint) * factor).toInt().coerceIn(0, 255),
                (Color.green(tint) * factor).toInt().coerceIn(0, 255),
                (Color.blue(tint) * factor).toInt().coerceIn(0, 255),
            )
            canvas.drawRect(
                centerX - bandWidth / 2f,
                y - thickness / 2f,
                centerX + bandWidth / 2f,
                y + thickness / 2f,
                paint,
            )
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
