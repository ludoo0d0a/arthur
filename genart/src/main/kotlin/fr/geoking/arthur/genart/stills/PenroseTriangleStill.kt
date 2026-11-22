package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Bakes one frozen frame of Penrose Triangle for Auto/Ambient album art. */
internal object PenroseTriangleStill {
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
        val minDim = min(w, h)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.46f, maxOf(w, h) * 0.95f,
            intArrayOf(0xFF1A1530.toInt(), 0xFF0A0814.toInt(), 0xFF030208.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.15f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val breath = 1f + 0.018f * sin(loop * 2f * PI.toFloat())
        val dim = 0.62f + 0.38f * pulse
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.78f * breath / 108f
        val bases = listOf(palette.colorAt(0), palette.colorAt(2), palette.colorAt(4))
        beams.forEachIndexed { i, beam ->
            val crawl = 0.12f + 0.28f * loop
            val base = bases[i % bases.size]
            val shade = when (i % 3) {
                0 -> mixArgb(base, Color.WHITE, 0.22f + crawl * 0.35f)
                1 -> mixArgb(base, Color.WHITE, crawl * 0.12f)
                else -> mixArgb(base, Color.BLACK, 0.18f + (1f - crawl) * 0.22f)
            }
            paint.color = withAlpha(shade, 0.94f * dim)
            path.reset()
            beam.forEachIndexed { j, pt ->
                val x = cx + (pt.first - 54f) * scale
                val y = cy + (pt.second - 54f) * scale
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
    }

    private val beams = listOf(
        arrayOf(
            19.83f to 78.77f, 30.11f to 78.82f, 58.42f to 29.57f,
            98.96f to 97.12f, 104f to 87.93f, 58.56f to 10.87f,
        ),
        arrayOf(
            4f to 88.26f, 8.77f to 97.24f, 99f to 97.18f, 58.43f to 29.57f,
            53.34f to 38.43f, 82.71f to 88.2f,
        ),
        arrayOf(
            47.52f to 10.76f, 4.01f to 88.24f, 82.73f to 88.22f, 77.15f to 78.8f,
            19.78f to 78.81f, 58.61f to 10.87f,
        ),
    )

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