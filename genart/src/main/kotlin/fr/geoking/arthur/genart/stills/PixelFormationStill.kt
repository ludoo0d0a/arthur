package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Pixel Formation for Auto/Ambient album art. */
internal object PixelFormationStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val hues = intArrayOf(0x5EEAD4, 0x7DD3FC, 0xF9A8D4, 0xFDE68A, 0xC4B5FD)

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
        val dim = 0.65f + 0.35f * pulse
        val cycle = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(6, 8, 16)
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.color = Color.argb((60 * dim).toInt(), Color.red(palette.colorAt(0)), Color.green(palette.colorAt(0)), Color.blue(palette.colorAt(0)))
        for (i in 0 until 24) {
            canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, 1.2f, paint)
        }

        val cell = minOf(w, h) * 0.045f
        val formationW = 7 * cell * 1.35f
        val originX = (w - formationW) * 0.5f
        val descend = ((cycle * 0.55f) % 1f + 1f) % 1f
        val originY = -h * 0.15f + descend * h * 1.15f
        val sway = sin(cycle * 2f * PI.toFloat()) * w * 0.04f
        val unitCount = 18

        for (i in 0 until unitCount) {
            val col = i % 7
            val row = i / 7
            val wobbleAmp = 0.01f + rnd.nextFloat() * 0.03f
            val wobblePhase = rnd.nextFloat()
            val wobble = sin((cycle + wobblePhase) * 2f * PI.toFloat()) * wobbleAmp * w
            val x = originX + col * cell * 1.35f + sway + wobble
            val y = originY + row * cell * 1.45f
            if (y < -cell || y > h + cell) continue
            val sizeFrac = 0.7f + rnd.nextFloat() * 0.45f
            val s = cell * sizeFrac
            val hue = hues[i % hues.size]
            val tint = palette.colorAt(i)
            val r = (((hue shr 16) and 0xFF) * 0.75f + Color.red(tint) * 0.25f).toInt().coerceIn(0, 255)
            val g = (((hue shr 8) and 0xFF) * 0.75f + Color.green(tint) * 0.25f).toInt().coerceIn(0, 255)
            val b = ((hue and 0xFF) * 0.75f + Color.blue(tint) * 0.25f).toInt().coerceIn(0, 255)
            val alpha = ((0.55f + 0.35f * ((sin(cycle * 2f * PI.toFloat() + wobblePhase) + 1.0) / 2.0).toFloat()) * dim)
            paint.color = Color.argb((alpha * 255).toInt().coerceIn(0, 255), r, g, b)
            when (i % 3) {
                0 -> {
                    canvas.drawRect(x + s * 0.35f, y, x + s * 0.65f, y + s * 0.7f, paint)
                    canvas.drawRect(x, y + s * 0.35f, x + s, y + s * 0.6f, paint)
                }
                1 -> canvas.drawRect(x + s * 0.15f, y + s * 0.1f, x + s * 0.85f, y + s * 0.8f, paint)
                else -> {
                    canvas.drawRect(x + s * 0.2f, y, x + s * 0.8f, y + s * 0.35f, paint)
                    canvas.drawRect(x, y + s * 0.35f, x + s, y + s * 0.7f, paint)
                }
            }
        }
        paint.alpha = 255
    }
}
