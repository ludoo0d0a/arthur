package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
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

/** Bakes one frozen frame of Retro-futurist Demo for Auto/Ambient album art. */
internal object RetroWaveStill {
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
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.2f

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0618.toInt(), 0xFF12081F.toInt(), 0xFF05030A.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cyan = mixPalette(0x33F0FF, palette.colorAt(0))
        val magenta = mixPalette(0xFF2BD6, palette.colorAt(1))
        val mixT = ((sin(time) + 1.0) / 2.0).toFloat()
        val tint = Color.rgb(
            (cyan[0] + (magenta[0] - cyan[0]) * mixT).toInt().coerceIn(0, 255),
            (cyan[1] + (magenta[1] - cyan[1]) * mixT).toInt().coerceIn(0, 255),
            (cyan[2] + (magenta[2] - cyan[2]) * mixT).toInt().coerceIn(0, 255),
        )

        val sunX = w * 0.5f
        val sunY = h * 0.38f
        val sunR = minDim * 0.18f
        val sunTint = mixPalette(0xFF6B9D, magenta.let { Color.rgb(it[0], it[1], it[2]) })
        paint.shader = RadialGradient(
            sunX, sunY, sunR * 2.2f,
            intArrayOf(
                Color.argb((140 * dim).toInt(), sunTint[0], sunTint[1], sunTint[2]),
                Color.argb((50 * dim).toInt(), sunTint[0], sunTint[1], sunTint[2]),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sunX, sunY, sunR * 2.2f, paint)
        paint.shader = null
        paint.color = Color.argb((190 * dim).toInt(), sunTint[0], sunTint[1], sunTint[2])
        canvas.drawCircle(sunX, sunY, sunR, paint)
        paint.color = Color.argb((140 * dim).toInt(), 10, 6, 24)
        for (band in 0 until 5) {
            val by = sunY - sunR * 0.7f + band * sunR * 0.32f
            canvas.drawRect(sunX - sunR, by - sunR * 0.03f, sunX + sunR, by + sunR * 0.03f, paint)
        }

        val blurRadius = (size * 0.012f).coerceAtLeast(2f)
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.0015f + 1.5f
        val horizonY = h * 0.48f
        val vanishingX = w * 0.5f
        for (i in 0 until 14) {
            val tt = i / 14f
            val y = horizonY + (h - horizonY) * (1f - 1f / (1f + tt * 6.5f))
            val alpha = ((0.55f - tt * 0.4f).coerceIn(0.1f, 0.55f) * dim * 255).toInt()
            paint.color = Color.argb(alpha, Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawLine(0f, y, w, y, paint)
        }
        for (i in 0 until 7) {
            val ft = i / 6f
            paint.color = Color.argb((0.3f * dim * 255).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawLine(vanishingX, horizonY, ft * w, h, paint)
        }
        paint.maskFilter = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        for (i in 0 until 3) {
            val cx = (0.22f + rnd.nextFloat() * 0.56f) * w
            val cy = (0.12f + rnd.nextFloat() * 0.26f) * h
            val r = minDim * (0.08f + rnd.nextFloat() * 0.08f)
            val sides = 3 + rnd.nextInt(4)
            val angle = time * (0.2f + rnd.nextFloat() * 0.3f) + rnd.nextFloat() * 2f * PI.toFloat()
            val wire = if (i % 2 == 0) cyan else magenta
            paint.color = Color.argb((140 * dim).toInt(), wire[0], wire[1], wire[2])
            val path = Path()
            for (s in 0..sides) {
                val a = angle + s * 2f * PI.toFloat() / sides
                val px = cx + cos(a) * r
                val py = cy + sin(a) * r * 0.72f
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            canvas.drawPath(path, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        val scanStep = (h / 48f).coerceAtLeast(3f)
        var sy = 0f
        while (sy < h) {
            paint.color = Color.argb((12 * dim).toInt().coerceIn(0, 255), cyan[0], cyan[1], cyan[2])
            canvas.drawLine(0f, sy, w, sy, paint)
            sy += scanStep
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun mixPalette(baseRgb: Int, paletteColor: Int): IntArray {
        val br = (baseRgb shr 16) and 0xFF
        val bg = (baseRgb shr 8) and 0xFF
        val bb = baseRgb and 0xFF
        return intArrayOf(
            (br * 0.8f + Color.red(paletteColor) * 0.2f).toInt().coerceIn(0, 255),
            (bg * 0.8f + Color.green(paletteColor) * 0.2f).toInt().coerceIn(0, 255),
            (bb * 0.8f + Color.blue(paletteColor) * 0.2f).toInt().coerceIn(0, 255),
        )
    }
}
