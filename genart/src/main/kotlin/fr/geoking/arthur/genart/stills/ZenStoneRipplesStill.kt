package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of a quiet raked garden with smooth stones and ripples. */
internal object ZenStoneRipplesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
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
        val minDim = minOf(w, h)
        val tau = 2f * PI.toFloat()
        val t01 = ((phase % tau) / tau).let { if (it < 0f) it + 1f else it }
        val ebb = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val sand = 0xFFD8E0D8.toInt()
        val sandShade = mixArgb(sand, 0xFF8AA090.toInt(), 0.25f)
        val stone = 0xFF8AA090.toInt()
        val deepStone = mixArgb(0xFF2E3A32.toInt(), palette.colorAt(0), 0.22f)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(sand, sandShade, sand),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Fine raked grooves crossing the sand in long, quiet curves.
        val grooveCount = 22
        for (i in 0 until grooveCount) {
            val fraction = i / (grooveCount - 1f)
            val y = h * (0.08f + fraction * 0.84f)
            val wave = sin(fraction * tau * 2f + t01 * tau) * minDim * 0.004f * (0.85f + 0.15f * ebb)
            path.reset()
            path.moveTo(-w * 0.04f, y)
            path.cubicTo(w * 0.26f, y + wave, w * 0.70f, y - wave, w * 1.04f, y)
            paint.color = withAlpha(if (i % 3 == 0) Color.WHITE else deepStone, ((if (i % 3 == 0) 0.18f else 0.12f) * dim * 255).toInt())
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.0018f
            canvas.drawPath(path, paint)
        }

        val stones = listOf(
            Triple(0.33f, 0.42f, 0.19f),
            Triple(0.65f, 0.59f, 0.15f),
            Triple(0.43f, 0.72f, 0.105f),
        )
        stones.forEachIndexed { index, (x, y, radius) ->
            val cx = w * x
            val cy = h * y
            val rx = minDim * radius
            val ry = rx * (0.54f + index * 0.025f)
            val pulseWave = 0.5f + 0.5f * sin(t01 * tau + index * 1.7f)

            paint.style = Paint.Style.FILL
            paint.color = withAlpha(deepStone, (0.19f * dim * 255).toInt())
            rect.set(cx - rx * 0.92f, cy - ry * 0.32f, cx + rx * 0.98f, cy + ry * 1.03f)
            canvas.drawOval(rect, paint)

            rect.set(cx - rx, cy - ry, cx + rx, cy + ry)
            paint.shader = LinearGradient(
                cx - rx, cy - ry, cx + rx, cy + ry,
                intArrayOf(mixArgb(stone, Color.WHITE, 0.24f), stone, deepStone),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(rect, paint)
            paint.shader = null

            rect.set(cx - rx * 0.62f, cy - ry * 0.65f, cx + rx * 0.22f, cy - ry * 0.33f)
            paint.color = withAlpha(Color.WHITE, (0.32f * dim * 255).toInt())
            canvas.drawOval(rect, paint)

            // Four soft expanding rings cycle seamlessly around each stone.
            for (ring in 0 until 4) {
                val ringPhase = (t01 + ring / 4f + index * 0.17f) % 1f
                val scale = 1.12f + ringPhase * 0.75f
                val alpha = (1f - ringPhase) * (0.18f + 0.08f * pulseWave) * dim
                rect.set(cx - rx * scale, cy - ry * scale, cx + rx * scale, cy + ry * scale)
                paint.color = withAlpha(deepStone, (alpha * 255).toInt())
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = minDim * 0.0022f
                canvas.drawOval(rect, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
