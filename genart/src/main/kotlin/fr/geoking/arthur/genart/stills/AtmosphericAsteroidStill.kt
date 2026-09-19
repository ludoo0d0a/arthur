package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Atmospheric Asteroid for Auto/Ambient album art. */
internal object AtmosphericAsteroidStill {
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

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A1428.toInt(), 0xFF1A0A08.toInt(), 0xFF060308.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val dim = 0.65f + 0.35f * pulse
        paint.shader = RadialGradient(
            w * 0.75f, h * 0.9f, minDim * 0.7f,
            intArrayOf(
                Color.argb((0.22f * dim * 255).toInt().coerceIn(0, 255), 255, 136, 68),
                Color.argb((0.08f * dim * 255).toInt().coerceIn(0, 255), 255, 85, 34),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.75f, h * 0.9f, minDim * 0.7f, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val life = ((time % 1f) + 1f) % 1f
        val angle = 0.65f
        val dirX = cos(angle).toFloat()
        val dirY = sin(angle).toFloat()
        val travel = life * 1.35f - 0.15f
        val x = (-0.05f + dirX * travel) * w
        val y = (0.05f + dirY * travel) * h
        val edgeFade = sin(life * PI.toFloat()).toFloat().coerceAtLeast(0f)
        val tint = palette.colorAt(0)
        val hr = ((Color.red(tint) + 255) / 2)
        val hg = ((Color.green(tint) + 170) / 2)
        val hb = ((Color.blue(tint) + 85) / 2)

        val trailLen = minDim * 0.5f
        val sx = x - dirX * trailLen
        val sy = y - dirY * trailLen

        paint.maskFilter = BlurMaskFilter((minDim * 0.04f).coerceAtLeast(4f), BlurMaskFilter.Blur.NORMAL)
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.08f
        paint.shader = LinearGradient(
            sx, sy, x, y,
            Color.TRANSPARENT,
            Color.argb((0.5f * dim * edgeFade * 255).toInt().coerceIn(0, 255), hr, hg, hb),
            Shader.TileMode.CLAMP,
        )
        canvas.drawLine(sx, sy, x, y, paint)
        paint.maskFilter = null
        paint.shader = null

        paint.strokeWidth = minDim * 0.018f
        paint.shader = LinearGradient(
            sx, sy, x, y,
            Color.TRANSPARENT,
            Color.argb((0.7f * dim * edgeFade * 255).toInt().coerceIn(0, 255), hr, hg, hb),
            Shader.TileMode.CLAMP,
        )
        canvas.drawLine(sx, sy, x, y, paint)
        paint.shader = null

        paint.style = Paint.Style.FILL
        for (i in 0 until 14) {
            val along = rnd.nextFloat() * trailLen
            val side = (rnd.nextFloat() * 2f - 1f) * minDim * 0.03f
            val px = x - dirX * along + (-dirY) * side
            val py = y - dirY * along + dirX * side
            paint.color = Color.argb(
                (0.5f * dim * edgeFade * 255).toInt().coerceIn(0, 255),
                hr, hg, hb,
            )
            canvas.drawCircle(px, py, minDim * (0.004f + rnd.nextFloat() * 0.006f), paint)
        }

        val rockSize = minDim * 0.045f
        paint.color = Color.argb((0.95f * dim * edgeFade * 255).toInt().coerceIn(0, 255), 74, 58, 50)
        canvas.drawCircle(x, y, rockSize, paint)
        canvas.drawCircle(x + rockSize * 0.4f, y - rockSize * 0.3f, rockSize * 0.65f, paint)
        canvas.drawCircle(x - rockSize * 0.35f, y + rockSize * 0.25f, rockSize * 0.5f, paint)
        paint.color = Color.argb((0.55f * dim * edgeFade * 255).toInt().coerceIn(0, 255), hr, hg, hb)
        canvas.drawCircle(x + dirX * rockSize * 0.35f, y + dirY * rockSize * 0.35f, rockSize * 0.35f, paint)
        paint.alpha = 255
    }
}
