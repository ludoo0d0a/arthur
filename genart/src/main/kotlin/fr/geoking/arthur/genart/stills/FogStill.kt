package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.random.Random

/** Bakes one frozen frame of Soft Fog for Auto/Ambient album art. */
internal object FogStill {
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
        val t01 = ((phase / (2f * PI.toFloat())) % 1f + 1f) % 1f

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF101820.toInt(), 0xFF080C12.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val dim = 0.7f + 0.3f * pulse
        // Soft full-frame veil that breathes with the loop (car-safe).
        val veil = (0.04f + 0.05f * loopedFbm(t01, radius = 1.3f, seedOffset = 3)) * dim
        paint.color = Color.argb((veil * 255).toInt().coerceIn(0, 255), 200, 214, 230)
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.maskFilter = BlurMaskFilter((size * 0.04f).coerceAtLeast(8f), BlurMaskFilter.Blur.NORMAL)
        for (i in 0 until 8) {
            val x0 = rnd.nextFloat()
            val yFrac = 0.15f + rnd.nextFloat() * 0.7f
            val direction = if (i % 2 == 0) 1f else -1f
            val speedMul = (0.08f + rnd.nextFloat() * 0.20f) * direction
            val widthFrac = 0.40f + rnd.nextFloat() * 0.45f
            val heightFrac = 0.08f + rnd.nextFloat() * 0.14f
            val bobAmp = 0.004f + rnd.nextFloat() * 0.016f
            val alphaBase = if (i < 2) 0.16f + rnd.nextFloat() * 0.16f else 0.10f + rnd.nextFloat() * 0.12f
            val x = (((x0 + t01 * speedMul + rotationDeg / 720f) % 1f) + 1f) % 1f * w
            val bobNoise = loopedFbm(t01, radius = 1.25f + i * 0.05f, seedOffset = i * 73) * 2f - 1f
            val y = yFrac * h + bobNoise * bobAmp * h
            val rw = widthFrac * w
            val rh = heightFrac * h
            val tint = palette.colorAt(i)
            val r = ((Color.red(tint) + 216) / 2)
            val g = ((Color.green(tint) + 228) / 2)
            val b = ((Color.blue(tint) + 240) / 2)
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)
            val radius = maxOf(rw, rh) * 0.65f
            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
            paint.color = Color.argb((alpha * 0.7f).toInt(), r, g, b)
            canvas.drawOval(RectF(x - rw * 0.5f, y - rh * 0.5f, x + rw * 0.5f, y + rh * 0.5f), paint)
        }
        paint.maskFilter = null
        paint.alpha = 255
    }
}
