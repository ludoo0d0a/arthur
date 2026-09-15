package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Diamond Weave for Auto/Ambient album art. */
internal object DiamondWeaveStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val bg = palette.colorAt(4)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(255, (Color.red(bg) * 0.4f).toInt(), (Color.green(bg) * 0.4f).toInt(), (Color.blue(bg) * 0.4f).toInt()),
                0xFF08060C.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val cols = 7
        val rows = 11
        val cellW = w / (cols - 0.5f)
        val cellH = h / (rows - 0.5f)
        val rx = cellW * 0.52f
        val ry = cellH * 0.52f
        val gap = minOf(rx, ry) * 0.04f

        for (index in 0 until cols * rows) {
            val col = index % cols
            val row = index / cols
            val stagger = if (row % 2 == 0) 0f else cellW * 0.5f
            val cx = col * cellW + stagger
            val cy = row * cellH * 0.55f + cellH * 0.2f
            val shadeSkew = 0.08f + rnd.nextFloat() * 0.2f
            val pulseAmp = rnd.nextFloat() * 0.06f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val colorMix = rnd.nextFloat()
            val colorIndex = index
            val pulseMul = 1f + pulseAmp * sin(loop * 2f * PI.toFloat() + pulsePhase)
            val halfX = (rx - gap) * pulseMul
            val halfY = (ry - gap) * pulseMul
            val base = mixArgb(palette.colorAt(colorIndex), palette.colorAt(colorIndex + 2), colorMix)
            val light = mixArgb(base, Color.WHITE, shadeSkew)
            val dark = mixArgb(base, Color.BLACK, shadeSkew * 1.35f)

            paint.color = Color.argb((0.28f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            path.reset()
            path.moveTo(cx, cy - halfY + 3f)
            path.lineTo(cx + halfX + 2f, cy + 3f)
            path.lineTo(cx, cy + halfY + 5f)
            path.lineTo(cx - halfX + 1f, cy + 3f)
            path.close()
            canvas.drawPath(path, paint)

            paint.color = withAlpha(light, 0.92f * dim)
            path.reset()
            path.moveTo(cx, cy - halfY)
            path.lineTo(cx - halfX, cy)
            path.lineTo(cx, cy + halfY)
            path.close()
            canvas.drawPath(path, paint)

            paint.color = withAlpha(dark, 0.92f * dim)
            path.reset()
            path.moveTo(cx, cy - halfY)
            path.lineTo(cx + halfX, cy)
            path.lineTo(cx, cy + halfY)
            path.close()
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
    }

    private fun withAlpha(argb: Int, alpha: Float): Int =
        Color.argb(
            (alpha * 255).toInt().coerceIn(0, 255),
            Color.red(argb),
            Color.green(argb),
            Color.blue(argb),
        )

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }
}
