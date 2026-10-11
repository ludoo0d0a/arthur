package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen/live frame of PPSSPP CRT Scanlines for Auto/Ambient. */
internal object PpssppCrtScanlinesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
        width: Int = size,
        height: Int = size,
        isActive: Boolean = true,
    ) {
        @Suppress("UNUSED_PARAMETER")
        val _unused = generation to rotationDeg
        val centerX = width / 2f
        val centerY = height / 2f
        val elapsedSec = phase

        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val time = elapsedSec

        val primaryColor = palette.primary
        val secondaryColor = palette.secondary

        // Base screen graphics (underlying test pattern signal)
        paint.shader = RadialGradient(
            centerX, centerY, maxOf(w, h) * 0.75f,
            intArrayOf(primaryColor, secondaryColor, 0xFF000000.toInt()),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Animated color bars/waves under CRT filter
        paint.style = Paint.Style.FILL
        val barCount = 6
        val barW = w / barCount
        val paletteColors = if (palette.colors.isNotEmpty()) palette.colors else listOf(0xFFEF4444.toInt(), 0xFF3B82F6.toInt())
        for (i in 0 until barCount) {
            val c = paletteColors[i % paletteColors.size]
            val waveY = sin(time * 2f + i) * h * 0.1f
            paint.color = Color.argb(
                180,
                Color.red(c),
                Color.green(c),
                Color.blue(c)
            )
            canvas.drawRect(i * barW, h * 0.2f + waveY, (i + 1) * barW, h * 0.8f + waveY, paint)
        }

        // 1. Scanlines (horizontal darkness stripes simulating raster lines)
        val lineSpacing = 6f
        val scanlineCount = (h / lineSpacing).toInt()
        paint.color = Color.argb(if (isActive) 120 else 80, 0, 0, 0)
        paint.strokeWidth = 2.5f
        paint.style = Paint.Style.STROKE
        for (i in 0 until scanlineCount) {
            val y = i * lineSpacing
            canvas.drawLine(0f, y, w, y, paint)
        }

        // 2. Rolling V-SYNC flicker line
        val vSyncY = ((time * 0.3f) % 1f) * h
        paint.color = Color.argb(40, 255, 255, 255)
        paint.strokeWidth = 20f
        canvas.drawLine(0f, vSyncY, w, vSyncY, paint)

        // 3. CRT Screen Curvature & Corner Bezel Vignette
        paint.shader = RadialGradient(
            centerX, centerY, maxOf(w, h) * 0.65f,
            intArrayOf(Color.TRANSPARENT, Color.argb(160, 0, 0, 0), Color.argb(255, 0, 0, 0)),
            floatArrayOf(0f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // 4. Subtle RGB Phosphor Triad Overlay
        val phosphorCols = (w / 8f).toInt()
        val pW = w / phosphorCols
        paint.style = Paint.Style.FILL
        for (i in 0 until phosphorCols) {
            val px = i * pW
            val colType = i % 3
            paint.color = when (colType) {
                0 -> Color.argb(20, 255, 0, 0)
                1 -> Color.argb(20, 0, 255, 0)
                else -> Color.argb(20, 0, 0, 255)
            }
            canvas.drawRect(px, 0f, px + pW, h, paint)
        }
    }
}
