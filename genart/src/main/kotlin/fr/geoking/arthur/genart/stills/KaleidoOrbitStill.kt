package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Kaleido Orbit for Auto/Ambient album art. */
internal object KaleidoOrbitStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val petal = Path()

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
        val cx = w * 0.5f
        val cy = h * 0.5f
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val loop = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 23)
        val breathing = (0.5f + 0.5f * sin(t01 * 2f * PI.toFloat())) * (0.75f + 0.25f * loop)
        val dim = 0.85f + 0.15f * pulse
        val gold = mixArgb(0xFFFFD080.toInt(), palette.colorAt(0), 0.22f)
        val rose = 0xFFB86A88.toInt()
        val lilac = 0xFF786080.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(brightness(0xFF101018.toInt(), dim), brightness(0xFF201828.toInt(), dim)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            cx, cy, minDim * 0.58f,
            intArrayOf(
                withAlpha(gold, (0.12f + 0.04f * breathing) * dim),
                withAlpha(rose, 0.075f * dim),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, minDim * 0.58f, paint)
        paint.shader = null

        // Fine glass-like guide rings behind the mirrored petals.
        paint.style = Paint.Style.STROKE
        for (i in 0..3) {
            paint.strokeWidth = minDim * 0.0018f
            paint.color = withAlpha(if (i % 2 == 0) gold else lilac, (0.12f + 0.025f * breathing) * dim)
            canvas.drawCircle(cx, cy, minDim * (0.105f + i * 0.085f), paint)
        }

        val petals = 8
        for (i in 0 until petals) {
            val angle = t01 * 2f * PI.toFloat() + i * (2f * PI.toFloat() / petals)
            val orbit = minDim * 0.17f
            val petalLength = minDim * (0.13f + 0.012f * breathing)
            val petalWidth = minDim * 0.045f
            petal.reset()
            petal.moveTo(cx, cy - orbit)
            petal.cubicTo(
                cx - petalWidth, cy - orbit - petalLength * 0.25f,
                cx - petalWidth * 0.72f, cy - orbit - petalLength * 0.82f,
                cx, cy - orbit - petalLength,
            )
            petal.cubicTo(
                cx + petalWidth * 0.72f, cy - orbit - petalLength * 0.82f,
                cx + petalWidth, cy - orbit - petalLength * 0.25f,
                cx, cy - orbit,
            )
            petal.close()
            val color = when (i % 3) {
                0 -> gold
                1 -> rose
                else -> lilac
            }
            canvas.save()
            canvas.rotate(angle * (180f / PI.toFloat()), cx, cy)
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                cx, cy - orbit - petalLength, cx, cy - orbit,
                intArrayOf(
                    withAlpha(color, 0.12f * dim),
                    withAlpha(color, 0.34f * dim),
                    withAlpha(gold, 0.18f * dim),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(petal, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.0015f
            paint.color = withAlpha(color, 0.24f * dim)
            canvas.drawPath(petal, paint)
            canvas.restore()

            // Slowly orbiting pearls trace a second, wider ring.
            val beadAngle = angle - t01 * 2f * PI.toFloat() * 0.08f
            val bx = cx + cos(beadAngle) * minDim * 0.31f
            val by = cy + sin(beadAngle) * minDim * 0.31f
            val beadRadius = minDim * 0.013f
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                bx, by, beadRadius * 3f,
                intArrayOf(withAlpha(gold, 0.30f * dim), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(bx, by, beadRadius * 3f, paint)
            paint.shader = null
            paint.color = withAlpha(gold, 0.38f * dim)
            canvas.drawCircle(bx, by, beadRadius * (0.85f + breathing * 0.15f), paint)
        }

        paint.shader = RadialGradient(
            cx, cy, minDim * 0.10f,
            intArrayOf(
                withAlpha(gold, (0.38f + 0.08f * breathing) * dim),
                withAlpha(rose, 0.24f * dim),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, minDim * 0.10f, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.002f
        paint.color = withAlpha(gold, 0.42f * dim)
        canvas.drawCircle(cx, cy, minDim * 0.018f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, blue)
    }

    private fun brightness(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255),
    )

    private fun withAlpha(rgb: Int, alpha: Float): Int =
        Color.argb((alpha * 255f).toInt().coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
