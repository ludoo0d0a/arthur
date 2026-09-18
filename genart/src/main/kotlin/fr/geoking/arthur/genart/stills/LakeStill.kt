package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.random.Random

/** Bakes one frozen frame of the Lake Surface look for Auto album art. */
internal object LakeStill {
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
        val horizon = h * 0.5f

        val skyTop = palette.colorAt(0)
        val skyMid = palette.colorAt(1)
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            horizon,
            skyTop,
            skyMid,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, horizon, paint)
        paint.shader = null

        val waterTop = mix(skyMid, 0xFF03080C.toInt(), 0.35f)
        val waterBottom = 0xFF010204.toInt()
        paint.shader = LinearGradient(
            0f,
            horizon,
            0f,
            h,
            waterTop,
            waterBottom,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon, w, h, paint)
        paint.shader = null

        val originCount = 2
        val origins = Array(originCount) {
            floatArrayOf(
                0.22f + rnd.nextFloat() * 0.56f,
                0.6f + rnd.nextFloat() * 0.32f,
            )
        }

        val ringCount = 3
        val minDim = if (w < h) w else h
        val pulseAlpha = 0.85f + pulse * 0.15f
        for (i in 0 until ringCount) {
            val originIndex = i % originCount
            val phaseOffset = rnd.nextFloat()
            val cyclesPerLoop = 0.6f + rnd.nextFloat() * 0.5f
            val maxRadiusFrac = 0.1f + rnd.nextFloat() * 0.12f
            val baseAlpha = 0.14f + rnd.nextFloat() * 0.12f

            val raw = phase * cyclesPerLoop + phaseOffset
            val life = raw - kotlin.math.floor(raw)
            val radius = life * maxRadiusFrac * minDim
            if (radius <= 0.5f) continue

            val origin = origins[originIndex]
            val cx = origin[0] * w
            val cy = horizon + origin[1] * (h - horizon)
            val alpha = (1f - life) * baseAlpha * pulseAlpha
            val strokeWidth = (2.2f - life * 1.2f).coerceAtLeast(0.5f)

            val tint = palette.colorAt(i)
            paint.color = Color.argb(
                (alpha * 255f).toInt().coerceIn(0, 255),
                Color.red(tint),
                Color.green(tint),
                Color.blue(tint),
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.alpha = 255
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        val al = (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * u).toInt().coerceIn(0, 255)
        return Color.argb(al, r, g, bl)
    }
}
