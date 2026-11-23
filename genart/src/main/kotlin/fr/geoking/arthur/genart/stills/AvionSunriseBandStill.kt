package fr.geoking.arthur.genart.stills

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
import kotlin.math.sin

/** Bakes one frozen frame of Avion Sunrise Band for Auto/Ambient album art. */
internal object AvionSunriseBandStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 3, seedOffset = 11)
        val drift = sin(t01 * 2f * PI.toFloat())
        val dim = 0.85f + 0.15f * pulse
        val horizonY = h * (0.57f + 0.012f * drift)
        val coral = mixArgb(0xFFE08060.toInt(), palette.colorAt(0), 0.22f)
        val gold = mixArgb(0xFFFFE0A0.toInt(), palette.colorAt(1), 0.18f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF2A1840.toInt(), 0xFF665071.toInt(), coral, gold, 0xFF423044.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.38f, 0.53f, 0.60f, 0.70f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val glowRadius = minDim * 0.58f
        paint.shader = RadialGradient(
            w * 0.53f, horizonY + h * 0.025f, glowRadius,
            intArrayOf(withAlpha(gold, (0.48f * dim * 255).toInt()), withAlpha(coral, (0.20f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.53f, horizonY + h * 0.025f, glowRadius, paint)
        paint.shader = LinearGradient(
            0f, horizonY - h * 0.07f, 0f, horizonY + h * 0.08f,
            intArrayOf(Color.TRANSPARENT, withAlpha(gold, (0.24f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizonY - h * 0.07f, w, horizonY + h * 0.08f, paint)
        paint.shader = null

        for (i in 0 until 7) {
            val n = loopedFbm(t01, radius = 1.2f + i * 0.06f, octaves = 2, seedOffset = 47 + i * 29)
            val baseX = ((i * 0.31f + n * 0.19f + drift * 0.025f + 1f) % 1f) * w
            val y = horizonY + h * (0.045f + (i % 4) * 0.075f) + h * 0.009f * sin(t01 * 2f * PI.toFloat() + i)
            val cloudW = w * (0.20f + 0.25f * n)
            val cloudH = h * (0.035f + 0.035f * n)
            val cloudColor = if (i % 3 == 0) coral else 0xFF171522.toInt()
            val alpha = (if (y < horizonY + h * 0.12f) 0.27f else 0.72f) * dim
            paint.color = withAlpha(cloudColor, (alpha * 255).toInt())
            canvas.drawOval(baseX - cloudW * 0.5f, y, baseX + cloudW * 0.5f, y + cloudH, paint)
            paint.color = withAlpha(cloudColor, (alpha * 0.72f * 255).toInt())
            canvas.drawOval(baseX - cloudW * 0.2f, y - cloudH * 0.38f, baseX + cloudW * 0.38f, y + cloudH * 0.34f, paint)
            paint.color = withAlpha(gold, (0.16f * n * dim * 255).toInt())
            canvas.drawOval(baseX - cloudW * 0.45f, y + cloudH * 0.08f, baseX + cloudW * 0.33f, y + cloudH * 0.30f, paint)
        }

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.018f
        paint.color = withAlpha(0xFF080A0C.toInt(), (0.28f * dim * 255).toInt())
        rect.set(w * 0.025f, h * 0.018f, w * 0.975f, h * 0.982f)
        canvas.drawRoundRect(rect, minDim * 0.065f, minDim * 0.065f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
