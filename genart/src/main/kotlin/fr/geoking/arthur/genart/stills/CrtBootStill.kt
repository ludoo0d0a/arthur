package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** Bakes one frozen frame of CRT Boot for Auto/Ambient album art. */
internal object CrtBootStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
    }

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
        val cycle = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.45f, maxOf(w, h) * 0.75f,
            intArrayOf(0xFF0A1810.toInt(), 0xFF041008.toInt(), 0xFF020804.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val tint = palette.colorAt(0)
        val gr = ((0x66 + Color.red(tint)) / 2)
        val gg = ((0xFF + Color.green(tint)) / 2)
        val gb = ((0x88 + Color.blue(tint)) / 2)

        val lines = listOf(
            "SYSTEM READY",
            "MEMORY CHECK .......... OK",
            "DISPLAY INIT ........... OK",
            "AUDIO BUS .............. IDLE",
            "AMBIENT LINK ........... WAIT",
            "LOADING PALETTE ........ DONE",
            ">_",
        )
        val reveal = floor(cycle * (lines.size + 1f)).toInt().coerceIn(0, lines.size)
        val fontSize = h * 0.028f
        textPaint.textSize = fontSize
        val lineGap = fontSize * 1.55f
        val startY = h * 0.28f
        val startX = w * 0.12f
        textPaint.color = Color.argb((220 * dim).toInt(), gr, gg, gb)
        for (i in 0 until reveal) {
            canvas.drawText(lines[i], startX, startY + i * lineGap, textPaint)
        }
        if (reveal > 0 && lines.getOrNull(reveal - 1)?.startsWith(">") == true) {
            val cursorAlpha = 0.25f + 0.55f * ((sin(cycle * 4f * PI.toFloat()) + 1.0) / 2.0).toFloat()
            paint.color = Color.argb((cursorAlpha * dim * 255).toInt(), gr, gg, gb)
            val cx = startX + fontSize * 0.7f
            val cy = startY + (reveal - 1) * lineGap
            canvas.drawRect(cx, cy - fontSize * 0.85f, cx + fontSize * 0.55f, cy + fontSize * 0.1f, paint)
        }

        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        val scanStep = (h / 48f).coerceAtLeast(3f)
        var sy = 0f
        while (sy < h) {
            paint.color = Color.argb((10 * dim).toInt(), gr, gg, gb)
            canvas.drawLine(0f, sy, w, sy, paint)
            sy += scanStep
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}
