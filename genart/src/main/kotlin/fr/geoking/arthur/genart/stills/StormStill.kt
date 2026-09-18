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
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of "Soft Storm" for Auto/Ambient album art — blurred clouds + crisp rain. */
internal object StormStill {
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

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF11151D.toInt(), 0xFF04070B.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val drift = phase + rotationDeg * PI.toFloat() / 180f

        // Blurred layer: distant soft glow (heat-lightning stand-in, never a hard flash) + cloud silhouettes.
        val blurRadius = (size * 0.05f).coerceAtLeast(6f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        val glowAlpha = 0.10f + 0.18f * pulse
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.22f, w * 0.55f,
            intArrayOf(
                Color.argb((glowAlpha * 255).toInt().coerceIn(0, 255), 174, 185, 204),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.22f, w * 0.55f, paint)
        paint.shader = null

        val cloudCount = 6
        for (i in 0 until cloudCount) {
            val x0 = rnd.nextFloat()
            val yFrac = 0.05f + rnd.nextFloat() * 0.35f
            val speedMul = 0.03f + rnd.nextFloat() * 0.06f
            val scale = 0.7f + rnd.nextFloat() * 0.6f
            val tint = palette.colorAt(i)
            val x = (((x0 + drift * speedMul * 0.15f) % 1f) + 1f) % 1f * w
            val y = yFrac * h
            val radius = scale * w.coerceAtMost(h) * 0.42f
            paint.color = Color.argb(
                (0.8f * 255).toInt(),
                (Color.red(tint) * 0.25f).toInt(),
                (Color.green(tint) * 0.25f).toInt(),
                (Color.blue(tint) * 0.25f).toInt(),
            )
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.maskFilter = null

        // Crisp layer on top: rain streaks, kept sharp, leaning together with a shared gust angle.
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val dim = 0.7f + 0.3f * pulse
        val gust = 0.08f + 0.15f * sin(drift)
        for (i in 0 until 90) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val fallSpeed = 0.7f + rnd.nextFloat() * 0.9f
            val lengthFrac = 0.02f + rnd.nextFloat() * 0.04f
            val thickness = 1f + rnd.nextFloat() * 1.2f
            val alphaBase = 0.2f + rnd.nextFloat() * 0.4f
            val fall = ((y0 + fallSpeed * (phase / (2f * PI.toFloat()))) % 1f + 1f) % 1f
            val y = fall * h
            val x = (((x0 + gust * 0.02f) % 1f) + 1f) % 1f * w
            val len = lengthFrac * h
            val tint = palette.colorAt(i)
            paint.strokeWidth = thickness
            paint.color = Color.argb(
                (alphaBase * dim * 255).toInt().coerceIn(0, 255),
                ((Color.red(tint) + 174) / 2),
                ((Color.green(tint) + 190) / 2),
                ((Color.blue(tint) + 216) / 2),
            )
            canvas.drawLine(x, y, x + gust * len, y + len, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}
