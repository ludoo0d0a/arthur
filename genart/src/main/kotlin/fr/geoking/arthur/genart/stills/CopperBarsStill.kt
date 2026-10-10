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

/** Bakes one frozen frame of Copper Bars for Auto/Ambient album art. */
internal object CopperBarsStill {
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
        val cycle = (((phase / (2f * PI.toFloat())) + rotationDeg / 360f) % 1f + 1f) % 1f
        val horizon = h * 0.52f

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0614.toInt(), 0xFF120A1C.toInt(), 0xFF050308.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val rows = 10
        val scrollFloor = ((cycle * 0.4f) % 1f + 1f) % 1f
        for (row in 0 until rows) {
            val depth = (row + scrollFloor) / rows.toFloat()
            val nearness = 1f / (1f + depth * 4f)
            val y0 = horizon + (h - horizon) * (1f - nearness)
            val nextDepth = (row + 1 + scrollFloor) / rows.toFloat()
            val nextNear = 1f / (1f + nextDepth * 4f)
            val y1 = horizon + (h - horizon) * (1f - nextNear)
            val halfFar = w * 0.04f
            val halfNear = w * 0.55f
            val half0 = halfFar + (halfNear - halfFar) * (1f - nearness)
            val half1 = halfFar + (halfNear - halfFar) * (1f - nextNear)
            for (col in 0 until rows) {
                val u0 = col / rows.toFloat()
                val u1 = (col + 1) / rows.toFloat()
                val dark = ((row + col) % 2 == 0)
                val base = if (dark) Color.rgb(0x1A, 0x14, 0x28) else Color.rgb(0x2A, 0x20, 0x38)
                val fade = (0.4f + 0.6f * (1f - nearness)) * dim
                paint.color = Color.argb(
                    (200 * fade).toInt().coerceIn(0, 255),
                    (Color.red(base) * 0.85f + Color.red(palette.colorAt(col)) * 0.15f).toInt().coerceIn(0, 255),
                    (Color.green(base) * 0.85f + Color.green(palette.colorAt(col)) * 0.15f).toInt().coerceIn(0, 255),
                    (Color.blue(base) * 0.85f + Color.blue(palette.colorAt(col)) * 0.15f).toInt().coerceIn(0, 255),
                )
                path.reset()
                path.moveTo(w * 0.5f - half0 + u0 * half0 * 2f, y0)
                path.lineTo(w * 0.5f - half0 + u1 * half0 * 2f, y0)
                path.lineTo(w * 0.5f - half1 + u1 * half1 * 2f, y1)
                path.lineTo(w * 0.5f - half1 + u0 * half1 * 2f, y1)
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        val hues = intArrayOf(0xFF6B2D, 0xFFB347, 0xFF3D7A, 0xFFD166, 0xE85D04, 0xFF8FAB)
        val barCount = 10
        for (i in 0 until barCount) {
            val raw = ((i / barCount.toFloat() + cycle) % 1f + 1f) % 1f
            val y = raw * h
            val thickness = h * (0.018f + 0.012f * ((sin(raw * 2f * PI.toFloat() + i) + 1f) * 0.5f))
            val hue = hues[i % hues.size]
            val hr = (hue shr 16) and 0xFF
            val hg = (hue shr 8) and 0xFF
            val hb = hue and 0xFF
            val alpha = ((0.35f + 0.35f * sin(raw * PI.toFloat()).toFloat()) * dim).coerceIn(0.15f, 0.7f)
            paint.shader = LinearGradient(
                0f, y - thickness, 0f, y + thickness,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((alpha * 255).toInt(), hr, hg, hb),
                    Color.argb((alpha * 0.5f * 255).toInt(), hr, hg, hb),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, y - thickness, w, y + thickness, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }
}
