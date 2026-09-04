package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Soft Fog for Auto/Ambient album art. */
internal object FogStill {
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
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF101820.toInt(), 0xFF080C12.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        val dim = 0.7f + 0.3f * pulse
        for (i in 0 until 7) {
            val x0 = rnd.nextFloat()
            val yFrac = 0.15f + rnd.nextFloat() * 0.7f
            val speedMul = 0.04f + rnd.nextFloat() * 0.1f
            val widthFrac = 0.35f + rnd.nextFloat() * 0.35f
            val heightFrac = 0.08f + rnd.nextFloat() * 0.14f
            val bobAmp = 0.004f + rnd.nextFloat() * 0.016f
            val bobFreq = 0.2f + rnd.nextFloat() * 0.6f
            val alphaBase = 0.08f + rnd.nextFloat() * 0.14f
            val x = (((x0 + drift * speedMul * 0.1f) % 1f) + 1f) % 1f * w
            val y = yFrac * h + sin(drift * bobFreq) * bobAmp * h
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
        paint.alpha = 255
    }
}
