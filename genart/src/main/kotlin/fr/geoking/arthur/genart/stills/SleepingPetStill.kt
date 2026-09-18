package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of the "Sleeping Pet" curled silhouette for Auto/Ambient album art. */
internal object SleepingPetStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val bodyRect = RectF()
    private val darkBrown = Color.rgb(0x2A, 0x1D, 0x18)

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
        val centerX = w * 0.5f
        val centerY = h * 0.55f
        val minDim = w.coerceAtMost(h)

        paint.shader = RadialGradient(
            centerX,
            centerY,
            w.coerceAtLeast(h) * 0.8f,
            intArrayOf(0xFF3B2A22.toInt(), 0xFF241A16.toInt(), 0xFF150F0D.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val breathFactor = sin(phase) * 0.5f + 0.5f
        val scale = 1f + 0.03f * breathFactor * pulse

        val bodyColor = mixArgb(darkBrown, palette.colorAt(0), 0.3f)

        val bodyRadiusX = minDim * 0.32f * scale
        val bodyRadiusY = minDim * 0.22f * scale
        val bodyCx = centerX
        val bodyCy = centerY + minDim * 0.05f

        val headRadius = minDim * 0.16f * scale
        val headCx = centerX - bodyRadiusX * 0.55f
        val headCy = centerY - bodyRadiusY * 0.35f

        val tuckRadius = minDim * 0.12f * scale
        val tuckCx = centerX + bodyRadiusX * 0.35f
        val tuckCy = centerY - bodyRadiusY * 0.15f

        paint.color = bodyColor
        bodyRect.set(bodyCx - bodyRadiusX, bodyCy - bodyRadiusY, bodyCx + bodyRadiusX, bodyCy + bodyRadiusY)
        canvas.drawOval(bodyRect, paint)
        canvas.drawCircle(tuckCx, tuckCy, tuckRadius, paint)
        canvas.drawCircle(headCx, headCy, headRadius, paint)

        val glowColor = palette.colorAt(1)
        val moteCount = 2
        for (i in 0 until moteCount) {
            val mx = w * (0.15f + i * 0.7f)
            val my = h * (0.2f + i * 0.1f)
            val glowAlpha = ((0.18f + 0.10f * (sin(phase + i) * 0.5f + 0.5f)) * 255).toInt().coerceIn(0, 255)
            val glowRadius = minDim * 0.06f
            paint.shader = RadialGradient(
                mx,
                my,
                glowRadius,
                Color.argb(glowAlpha, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(mx, my, glowRadius, paint)
            paint.shader = null
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}
