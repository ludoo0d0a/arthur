package fr.geoking.arthur.fractal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * android.graphics still baker for Auto album art (no Compose).
 * [generation] shifts phase so Ambient Rotation URIs look distinct.
 */
object CustomFractalStillRenderer {
    fun draw(
        canvas: Canvas,
        params: CustomFractalParams,
        size: Int,
        generation: Long,
        quality: CustomFractalQuality = CustomFractalQuality.Medium,
    ) {
        canvas.drawColor(Color.BLACK)
        val t = ((generation % 240L).toFloat() / 240f)
        val strokes = CustomFractalEngine.frame(params, t, quality)
        val palette = CustomFractalEngine.paletteArgb(params.colorSeed)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val baseStroke = (size * 0.0045f).coerceIn(1.2f, 3.5f)
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val argb = palette[stroke.colorIndex % palette.size]
            val a = (Color.alpha(argb) * stroke.alpha).toInt().coerceIn(20, 240)
            paint.color = Color.argb(a, Color.red(argb), Color.green(argb), Color.blue(argb))
            paint.strokeWidth = baseStroke * stroke.strokeScale
            for (i in 0 until stroke.points.lastIndex) {
                val p0 = stroke.points[i]
                val p1 = stroke.points[i + 1]
                canvas.drawLine(
                    p0.x * size,
                    p0.y * size,
                    p1.x * size,
                    p1.y * size,
                    paint,
                )
            }
        }
    }
}
