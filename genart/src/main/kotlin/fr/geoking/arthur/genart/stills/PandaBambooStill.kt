package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Panda and Bamboo for Auto/Ambient album art. */
internal object PandaBambooStill {
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
        val minDim = w.coerceAtMost(h)

        // Deep bamboo forest background
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1A2C1E.toInt(), 0xFF243028.toInt(), 0xFF101812.toInt()),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Canopy light
        val canopyLight = mixArgb(0xFF8AAE68.toInt(), palette.colorAt(1), 0.2f)
        paint.shader = RadialGradient(
            w * 0.45f, h * 0.08f, w * 0.5f,
            intArrayOf(Color.argb(46, Color.red(canopyLight), Color.green(canopyLight), Color.blue(canopyLight)), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h * 0.4f, paint)
        paint.shader = null

        // Forest floor
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.82f, minDim * 0.5f,
            intArrayOf(Color.argb(128, 0x2A, 0x38, 0x28), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(w * 0.5f - minDim * 0.5f, h * 0.72f, w * 0.5f + minDim * 0.5f, h * 0.92f, paint)
        paint.shader = null

        val bamboo = mixArgb(0xFF4A6B3A.toInt(), palette.colorAt(2), 0.2f)
        val leafCol = mixArgb(0xFF3A5A2E.toInt(), palette.colorAt(2), 0.25f)
        paint.color = bamboo
        paint.strokeCap = Paint.Cap.ROUND

        // Bamboo stalks
        paint.style = Paint.Style.STROKE
        val stalkXs = floatArrayOf(0.15f, 0.32f, 0.48f, 0.72f, 0.88f)
        stalkXs.forEachIndexed { i, nx ->
            val baseX = nx * w
            val tipX = baseX + sin(phase + i) * minDim * 0.012f
            paint.strokeWidth = minDim * (0.014f + i * 0.0015f)
            paint.color = bamboo
            canvas.drawLine(baseX, h * 0.95f, tipX, h * 0.05f, paint)
            // Nodes
            paint.strokeWidth = minDim * 0.006f
            paint.color = Color.argb(128, Color.red(bamboo), Color.green(bamboo), Color.blue(bamboo))
            for (n in 1..3) {
                val ny = h * (0.95f - n * 0.22f)
                val nodX = baseX + (tipX - baseX) * (1f - ny / h)
                val hw = minDim * (0.014f + i * 0.0015f) * 0.8f
                canvas.drawLine(nodX - hw, ny, nodX + hw, ny, paint)
            }
            // Leaves
            paint.color = leafCol
            paint.strokeWidth = minDim * 0.006f
            for (n in 1..2) {
                val ny = h * (0.95f - n * 0.22f)
                val nodX = baseX + (tipX - baseX) * (1f - ny / h)
                val side = if (n % 2 == 0) 1f else -1f
                canvas.drawLine(nodX, ny, nodX + side * minDim * 0.03f, ny - minDim * 0.015f, paint)
            }
        }

        // Panda
        paint.style = Paint.Style.FILL
        val breath = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.02f * breath * pulse
        val white = 0xFFE6E2DA.toInt()
        val black = 0xFF12100E.toInt()
        val cx = w * 0.55f
        val cy = h * 0.62f
        val bodyRx = minDim * 0.16f * scale
        val bodyRy = minDim * 0.14f * scale
        paint.color = white
        rect.set(cx - bodyRx, cy - bodyRy, cx + bodyRx, cy + bodyRy)
        canvas.drawOval(rect, paint)
        // Belly
        paint.color = Color.argb(115, Color.red(black), Color.green(black), Color.blue(black))
        rect.set(cx - bodyRx * 0.5f, cy + bodyRy * 0.1f, cx + bodyRx * 0.5f, cy + bodyRy)
        canvas.drawOval(rect, paint)

        val headR = minDim * 0.09f * scale
        val hx = cx - bodyRx * 0.15f
        val hy = cy - bodyRy * 1.15f
        paint.color = white
        canvas.drawCircle(hx, hy, headR, paint)
        paint.color = black
        canvas.drawCircle(hx - headR * 0.7f, hy - headR * 0.7f, headR * 0.35f, paint)
        canvas.drawCircle(hx + headR * 0.7f, hy - headR * 0.7f, headR * 0.35f, paint)
        // Eye patches
        rect.set(hx - headR * 0.65f, hy - headR * 0.2f, hx - headR * 0.2f, hy + headR * 0.15f)
        canvas.drawOval(rect, paint)
        rect.set(hx + headR * 0.2f, hy - headR * 0.2f, hx + headR * 0.65f, hy + headR * 0.15f)
        canvas.drawOval(rect, paint)
        // Paws and feet
        canvas.drawCircle(cx - bodyRx * 0.9f, cy, minDim * 0.04f * scale, paint)
        canvas.drawCircle(cx + bodyRx * 0.85f, cy + bodyRy * 0.1f, minDim * 0.04f * scale, paint)
        canvas.drawCircle(cx - bodyRx * 0.3f, cy + bodyRy * 1.3f, minDim * 0.038f * scale, paint)
        canvas.drawCircle(cx + bodyRx * 0.3f, cy + bodyRy * 1.3f, minDim * 0.038f * scale, paint)

        // Held bamboo stick
        paint.color = bamboo
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.012f
        canvas.drawLine(cx + bodyRx * 0.3f, cy - bodyRy * 0.2f, cx + bodyRx * 0.5f, cy - bodyRy * 1.8f, paint)
        // Leaves on held bamboo
        paint.color = leafCol
        paint.strokeWidth = minDim * 0.005f
        val htx = cx + bodyRx * 0.5f
        val hty = cy - bodyRy * 1.8f
        canvas.drawLine(htx, hty, htx + minDim * 0.03f, hty - minDim * 0.015f, paint)
        canvas.drawLine(htx, hty + minDim * 0.02f, htx - minDim * 0.03f, hty + minDim * 0.005f, paint)
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
