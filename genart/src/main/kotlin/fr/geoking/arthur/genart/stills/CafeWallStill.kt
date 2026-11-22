package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Café Wall for Auto/Ambient album art. */
internal object CafeWallStill {
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
        val w = size.toFloat()
        val h = size.toFloat()
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF2A2D35.toInt(), 0xFF12141A.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.05f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val mortarPhase = sin(loop * 2f * PI.toFloat()) * 0.42f
        val dim = 0.68f + 0.32f * pulse
        val rows = 14
        val cols = 11
        val mortarH = h / (rows * 2.15f)
        val brickH = mortarH * 1.55f
        val brickW = w / cols
        val lightBrick = mixArgb(mixArgb(palette.colorAt(2), Color.WHITE, 0.25f), 0xFFE8EAED.toInt(), 0.62f)
        val darkBrick = mixArgb(mixArgb(palette.colorAt(4), Color.BLACK, 0.35f), 0xFF101218.toInt(), 0.55f)
        val mortar = 0xFF838896.toInt()
        for (row in 0 until rows) {
            val y = row * (brickH + mortarH) + mortarH * 0.5f
            val stagger = if (row % 2 == 0) 0f else brickW * (0.5f + mortarPhase * 0.08f)
            paint.color = Color.argb((0.85f * dim * 255).toInt().coerceIn(0, 255), Color.red(mortar), Color.green(mortar), Color.blue(mortar))
            canvas.drawRect(0f, y + brickH, w, y + brickH + mortarH, paint)
            for (col in -1..cols) {
                val x = col * brickW + stagger - mortarPhase * brickW * 0.06f
                val brick = if ((row + col) % 2 == 0) lightBrick else darkBrick
                paint.color = withAlpha(brick, 0.92f * dim)
                canvas.drawRect(x, y, x + brickW * 0.96f, y + brickH, paint)
            }
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