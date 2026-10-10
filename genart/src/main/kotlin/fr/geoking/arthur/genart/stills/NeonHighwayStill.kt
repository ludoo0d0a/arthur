package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI

/** Bakes one frozen frame of Neon Highway for Auto/Ambient album art. */
internal object NeonHighwayStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

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
        val dim = 0.65f + 0.35f * pulse
        val scroll = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f
        val vanishX = w * 0.5f
        val vanishY = h * 0.36f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0618.toInt(), 0xFF12081F.toInt(), 0xFF04020A.toInt()),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val magenta = mix(0xFF2BD6, palette.colorAt(1))
        paint.shader = RadialGradient(
            vanishX, vanishY, w * 0.35f,
            intArrayOf(Color.argb((55 * dim).toInt(), magenta[0], magenta[1], magenta[2]), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(vanishX, vanishY, w * 0.35f, paint)
        paint.shader = null

        path.reset()
        path.moveTo(vanishX - w * 0.015f, vanishY)
        path.lineTo(vanishX + w * 0.015f, vanishY)
        path.lineTo(w * 0.92f, h)
        path.lineTo(w * 0.08f, h)
        path.close()
        paint.shader = LinearGradient(
            0f, vanishY, 0f, h,
            intArrayOf(0xFF1A1228.toInt(), 0xFF0A0810.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        val cyan = mix(0x33F0FF, palette.colorAt(0))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        paint.color = Color.argb((90 * dim).toInt(), cyan[0], cyan[1], cyan[2])
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.FILL
        val markerCount = 12
        val step = 1f / markerCount
        for (i in 0 until markerCount) {
            val depth = ((i * step + scroll * step) % 1f + 1f) % 1f
            val nearness = 1f / (1f + depth * markerCount * 0.55f)
            val y = vanishY + (h - vanishY) * (1f - nearness)
            val half = (w * 0.015f) + (w * 0.42f) * (1f - nearness)
            val mw = half * 0.1f
            val mh = mw * 2.8f
            val alpha = (0.2f + 0.7f * (1f - nearness)).coerceIn(0.1f, 0.85f)
            paint.color = Color.argb((alpha * dim * 255).toInt(), 0xE8, 0xE4, 0xD8)
            canvas.drawRect(vanishX - mw * 0.5f, y - mh * 0.5f, vanishX + mw * 0.5f, y + mh * 0.5f, paint)
        }

        val postCount = 12
        for (side in listOf(-1f, 1f)) {
            for (i in 0 until postCount) {
                val depth = ((i / postCount.toFloat() + scroll) % 1f + 1f) % 1f
                val nearness = 1f / (1f + depth * postCount * 0.45f)
                val y = vanishY + (h - vanishY) * (1f - nearness)
                val half = (w * 0.02f) + (w * 0.48f) * (1f - nearness)
                val x = vanishX + side * half
                val postH = 8f + 28f * (1f - nearness)
                val alpha = (0.15f + 0.7f * (1f - nearness)).coerceIn(0.1f, 0.85f)
                paint.strokeWidth = 1.5f + 2.5f * (1f - nearness)
                paint.style = Paint.Style.STROKE
                paint.color = Color.argb((alpha * dim * 255).toInt(), magenta[0], magenta[1], magenta[2])
                canvas.drawLine(x, y, x, y - postH, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.argb((alpha * 0.9f * dim * 255).toInt(), cyan[0], cyan[1], cyan[2])
                canvas.drawCircle(x, y - postH, 2f + 3f * (1f - nearness), paint)
            }
        }
        paint.alpha = 255
    }

    private fun mix(baseRgb: Int, paletteColor: Int): IntArray {
        val br = (baseRgb shr 16) and 0xFF
        val bg = (baseRgb shr 8) and 0xFF
        val bb = baseRgb and 0xFF
        return intArrayOf(
            (br * 0.75f + Color.red(paletteColor) * 0.25f).toInt().coerceIn(0, 255),
            (bg * 0.75f + Color.green(paletteColor) * 0.25f).toInt().coerceIn(0, 255),
            (bb * 0.75f + Color.blue(paletteColor) * 0.25f).toInt().coerceIn(0, 255),
        )
    }
}
