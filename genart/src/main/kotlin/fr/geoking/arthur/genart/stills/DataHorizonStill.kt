package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette

/** Bakes one frozen frame of "Data Horizon" for Auto/Ambient album art — perspective grid, cool cross-fade tint. */
internal object DataHorizonStill {
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
        val w = size.toFloat()
        val h = size.toFloat()

        paint.maskFilter = null
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF05060C.toInt(), 0xFF000000.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cyan = mixWithPalette(0x33F0FF, palette.colorAt(0))
        val violet = mixWithPalette(0xA855F7, palette.colorAt(1))
        val blue = mixWithPalette(0x3B82F6, palette.colorAt(2))
        val drift = phase + rotationDeg * kotlin.math.PI.toFloat() / 180f
        val cycle = (((drift / (2f * kotlin.math.PI.toFloat())) % 1f + 1f) % 1f) * 3f
        val (from, to, localT) = when {
            cycle < 1f -> Triple(cyan, violet, cycle)
            cycle < 2f -> Triple(violet, blue, cycle - 1f)
            else -> Triple(blue, cyan, cycle - 2f)
        }
        val mixT = (kotlin.math.sin(localT * kotlin.math.PI) ).toFloat().coerceIn(0f, 1f)
        val r = (from[0] + (to[0] - from[0]) * mixT).toInt()
        val g = (from[1] + (to[1] - from[1]) * mixT).toInt()
        val b = (from[2] + (to[2] - from[2]) * mixT).toInt()
        val tint = Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

        val dim = 0.6f + 0.4f * pulse
        val blurRadius = (size * 0.015f).coerceAtLeast(2f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        val horizonY = h * 0.42f
        val vanishingX = w * 0.5f
        val spacingFactor = 6f
        val lineCount = 16

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.0015f + 1.5f
        for (i in 0 until lineCount) {
            val t = i / lineCount.toFloat()
            val y = horizonY + (h - horizonY) * (1f - 1f / (1f + t * spacingFactor))
            val alpha = ((0.5f - t * 0.35f).coerceIn(0.08f, 0.5f) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawLine(0f, y, w, y, paint)
        }

        val fanCount = 7
        for (i in 0 until fanCount) {
            val t = i / (fanCount - 1).coerceAtLeast(1).toFloat()
            val bottomX = t * w
            val alpha = (0.28f * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawLine(vanishingX, horizonY, bottomX, h, paint)
        }
        paint.maskFilter = null
        paint.style = Paint.Style.FILL

        val glowAlpha = (0.35f * dim * 255).toInt().coerceIn(0, 255)
        paint.shader = RadialGradient(
            vanishingX, horizonY, w * 0.35f,
            intArrayOf(
                Color.argb(glowAlpha, Color.red(tint), Color.green(tint), Color.blue(tint)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(vanishingX, horizonY, w * 0.35f, paint)
        paint.shader = null
    }

    private fun mixWithPalette(base: Int, paletteColor: Int, t: Float = 0.25f): IntArray {
        val baseR = (base shr 16) and 0xFF
        val baseG = (base shr 8) and 0xFF
        val baseB = base and 0xFF
        val r = baseR + (Color.red(paletteColor) - baseR) * t
        val g = baseG + (Color.green(paletteColor) - baseG) * t
        val b = baseB + (Color.blue(paletteColor) - baseB) * t
        return intArrayOf(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
    }
}
