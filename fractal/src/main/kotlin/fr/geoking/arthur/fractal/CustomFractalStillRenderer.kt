package fr.geoking.arthur.fractal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * android.graphics still baker for Auto album art (no Compose).
 * [generation] shifts phase so Ambient Rotation URIs look distinct.
 */
object CustomFractalStillRenderer {
    private const val FillScale = 1.13f

    fun draw(
        canvas: Canvas,
        params: CustomFractalParams,
        size: Int,
        generation: Long,
        quality: CustomFractalQuality = CustomFractalQuality.High,
    ) {
        canvas.drawColor(Color.BLACK)
        val t = ((generation % 240L).toFloat() / 240f)
        val strokes = CustomFractalEngine.frame(params, t, quality)
        val palette = CustomFractalEngine.paletteArgb(params.colorSeed)

        canvas.save()
        val half = size * 0.5f
        canvas.scale(FillScale, FillScale, half, half)

        val wash = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        val baseStroke = (size * 0.0045f).coerceIn(1.2f, 12f)
        for (stroke in strokes) {
            if (stroke.cubics.isEmpty()) continue
            val c0 = palette[stroke.colorIndex % palette.size]
            val c1 = palette[(stroke.colorIndex + 2) % palette.size]
            val a = (255 * stroke.alpha).toInt().coerceIn(30, 250)
            val start = stroke.start
            val end = stroke.end
            path.reset()
            val first = stroke.cubics.first()
            path.moveTo(first.p0.x * size, first.p0.y * size)
            for (cubic in stroke.cubics) {
                path.cubicTo(
                    cubic.c1.x * size,
                    cubic.c1.y * size,
                    cubic.c2.x * size,
                    cubic.c2.y * size,
                    cubic.p3.x * size,
                    cubic.p3.y * size,
                )
            }
            val coreWidth = baseStroke * stroke.strokeScale
            paint.shader = LinearGradient(
                start.x * size,
                start.y * size,
                end.x * size,
                end.y * size,
                Color.argb((a * 0.28f).toInt().coerceIn(8, 120), Color.red(c0), Color.green(c0), Color.blue(c0)),
                Color.argb((a * 0.18f).toInt().coerceIn(4, 90), Color.red(c1), Color.green(c1), Color.blue(c1)),
                Shader.TileMode.CLAMP,
            )
            paint.strokeWidth = coreWidth * 2.8f
            canvas.drawPath(path, paint)

            paint.shader = LinearGradient(
                start.x * size,
                start.y * size,
                end.x * size,
                end.y * size,
                Color.argb(a, Color.red(c0), Color.green(c0), Color.blue(c0)),
                Color.argb((a * 0.85f).toInt().coerceIn(20, 250), Color.red(c1), Color.green(c1), Color.blue(c1)),
                Shader.TileMode.CLAMP,
            )
            paint.strokeWidth = coreWidth
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        // Soft center lift + vignette (near-4K polish)
        val polish = Paint(Paint.ANTI_ALIAS_FLAG)
        polish.shader = RadialGradient(
            half,
            size * 0.48f,
            size * 0.55f,
            intArrayOf(Color.argb(26, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), polish)
        polish.shader = RadialGradient(
            half,
            size * 0.48f,
            size * 0.78f,
            intArrayOf(Color.TRANSPARENT, Color.argb(72, 0, 0, 0)),
            floatArrayOf(0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), polish)

        canvas.restore()
    }
}
