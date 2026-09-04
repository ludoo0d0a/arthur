package fr.geoking.arthur.fractal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

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
        val wash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size * 0.5f,
                size * 0.45f,
                size * 0.7f,
                intArrayOf(
                    Color.argb(36, Color.red(palette[0]), Color.green(palette[0]), Color.blue(palette[0])),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), wash)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val baseStroke = (size * 0.0045f).coerceIn(1.2f, 3.5f)
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val c0 = palette[stroke.colorIndex % palette.size]
            val c1 = palette[(stroke.colorIndex + 2) % palette.size]
            val a = (255 * stroke.alpha).toInt().coerceIn(30, 250)
            val start = stroke.points.first()
            val end = stroke.points.last()
            paint.shader = LinearGradient(
                start.x * size,
                start.y * size,
                end.x * size,
                end.y * size,
                Color.argb(a, Color.red(c0), Color.green(c0), Color.blue(c0)),
                Color.argb((a * 0.85f).toInt().coerceIn(20, 250), Color.red(c1), Color.green(c1), Color.blue(c1)),
                Shader.TileMode.CLAMP,
            )
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
            paint.shader = null
        }
    }
}
