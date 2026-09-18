package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI

/** Bakes one frozen frame of the empty perspective "Roads" look for Auto/Ambient album art. */
internal object RoadsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private const val MARKER_COUNT = 12

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
        val vanishX = w * 0.5f
        val vanishY = h * 0.34f

        val skyTop = scaleBrightness(palette.colorAt(0), 0.4f + pulse * 0.05f)
        val skyBottom = scaleBrightness(palette.colorAt(1), 0.55f + pulse * 0.05f)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, vanishY,
            skyTop, skyBottom,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, vanishY, paint)
        paint.shader = null

        val landColor = scaleBrightness(palette.colorAt(2), 0.35f)
        paint.color = landColor
        canvas.drawRect(0f, vanishY, w, h, paint)

        val roadFar = scaleBrightness(Color.rgb(0x2E, 0x31, 0x38), 1f)
        val roadNear = scaleBrightness(Color.rgb(0x16, 0x18, 0x1C), 1f)
        path.reset()
        path.moveTo(vanishX - w * 0.02f, vanishY)
        path.lineTo(vanishX + w * 0.02f, vanishY)
        path.lineTo(w * 0.94f, h)
        path.lineTo(w * 0.06f, h)
        path.close()
        paint.shader = LinearGradient(
            0f, vanishY, 0f, h,
            roadFar, roadNear,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        val shoulderColor = withAlpha(scaleBrightness(palette.colorAt(3), 1f), 0.55f)
        paint.color = shoulderColor
        path.reset()
        path.moveTo(vanishX - w * 0.02f, vanishY)
        path.lineTo(w * 0.06f, h)
        path.lineTo(w * 0.02f, h)
        path.close()
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(vanishX + w * 0.02f, vanishY)
        path.lineTo(w * 0.98f, h)
        path.lineTo(w * 0.94f, h)
        path.close()
        canvas.drawPath(path, paint)

        val markerColor = scaleBrightness(Color.rgb(0xE8, 0xE4, 0xD8), 1f)
        val scroll = phase01(phase / (2f * PI.toFloat()))
        val k = 0.55f
        val step = 1f / MARKER_COUNT
        for (i in 0 until MARKER_COUNT) {
            val depth = phase01((i * step) + scroll * step)
            val nearness = 1f / (1f + depth * MARKER_COUNT * k)
            val y = vanishY + (h - vanishY) * (1f - nearness)
            val roadHalfWidthAtY = (w * 0.02f) + (w * 0.46f) * (1f - nearness)
            val markerWidth = roadHalfWidthAtY * 0.12f
            val markerHeight = markerWidth * 3.2f
            val alpha = (0.85f * (0.25f + 0.75f * (1f - nearness))).coerceIn(0.1f, 0.85f)
            paint.color = withAlpha(markerColor, alpha)
            canvas.drawRect(
                vanishX - markerWidth / 2f,
                y - markerHeight / 2f,
                vanishX + markerWidth / 2f,
                y + markerHeight / 2f,
                paint,
            )
        }
        paint.alpha = 255
    }

    private fun phase01(t: Float): Float = ((t % 1f) + 1f) % 1f

    private fun scaleBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
