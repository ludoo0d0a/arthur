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
        val t01 = ((phase / (2f * PI.toFloat()) + rotationDeg / 360f) % 1f + 1f) % 1f

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

        val bandCount = 8
        val shimmerScale = 1f
        val pulseMix = 0.9f + pulse * 0.1f
        for (i in 0 until bandCount) {
            val depthFrac = i / (bandCount - 1f)
            val yFrac = 0.42f + depthFrac * 0.5f + (rnd.nextFloat() - 0.5f) * 0.04f
            val thicknessFrac = 0.02f + rnd.nextFloat() * 0.025f
            val widthFrac = 0.82f + rnd.nextFloat() * 0.15f
            val shimmerAmpFrac = 0.025f + rnd.nextFloat() * 0.045f
            val shimmerFreq = 1.4f + rnd.nextFloat() * 1.8f
            val shimmerPhase = rnd.nextFloat() * 2f * PI.toFloat()
            val driftSpeed = 0.05f + rnd.nextFloat() * 0.09f
            val verticalWobble = 0.004f + rnd.nextFloat() * 0.014f
            val alphaBase = 0.06f + rnd.nextFloat() * 0.1f

            val drift = ((t01 * driftSpeed) % 1f + 1f) % 1f * 2f * PI.toFloat()
            val shimmer = sin(t01 * shimmerFreq * 2f * PI.toFloat() + shimmerPhase) *
                shimmerAmpFrac * shimmerScale
            val yWobble = sin(t01 * shimmerFreq * PI.toFloat() + shimmerPhase) *
                verticalWobble * shimmerScale
            val bandWidth = widthFrac * w
            val centerX = w * 0.5f + (shimmer + sin(drift) * 0.01f) * w
            val y = (yFrac + yWobble) * h
            val thickness = thicknessFrac * h * (1f + 0.25f * shimmer.coerceIn(-1f, 1f))
            val tint = palette.colorAt(i)
            val factor = 0.7f + depthFrac * 0.3f
            val alpha = (alphaBase * pulseMix * 255).toInt().coerceIn(0, 56)

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