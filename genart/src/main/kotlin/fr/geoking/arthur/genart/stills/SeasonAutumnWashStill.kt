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

/** Bakes one frozen frame of Season Autumn Wash for Auto/Ambient album art. */
internal object SeasonAutumnWashStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val leafPath = Path()

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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = (0.85f + 0.15f * pulse).coerceIn(0f, 1f)
        val drift = loopedFbm(t, radius = 1.3f, octaves = 3, seedOffset = 11)
        val amber = mixArgb(0xFFE8B86D.toInt(), palette.colorAt(0), 0.24f)
        val russet = mixArgb(0xFFC9783A.toInt(), palette.colorAt(1), 0.18f)

        // Dusky amber-to-russet autumn atmosphere.
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF25110E.toInt(), 0xFF5A2315.toInt(), 0xFF964320.toInt(), 0xFFC9783A.toInt(), 0xFF521C12.toInt()),
            floatArrayOf(0f, 0.35f, 0.65f, 0.88f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Low dusk sun and reflected warmth, both soft-edged.
        val sunX = w * (0.52f + (drift - 0.5f) * 0.14f)
        val sunY = h * (0.34f + (loopedFbm(t, radius = 1.25f, seedOffset = 29) - 0.5f) * 0.08f)
        val sunR = minDim * 0.62f
        paint.shader = RadialGradient(
            sunX, sunY, sunR,
            intArrayOf(withAlpha(0xFFFFE5A3.toInt(), (145 * dim).toInt()), withAlpha(amber, (90 * dim).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sunX, sunY, sunR, paint)
        paint.shader = null
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.92f, minDim * 0.58f,
            intArrayOf(withAlpha(russet, (75 * dim).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, h * 0.92f, minDim * 0.58f, paint)
        paint.shader = null

        // Broad, translucent wash bands add the flowing painted atmosphere.
        for (i in 0..3) {
            val y = h * (0.39f + i * 0.145f)
            val bend = (loopedFbm(t, radius = 1.1f + i * 0.1f, seedOffset = 43 + i * 17) - 0.5f) * minDim * 0.16f
            val band = Path().apply {
                moveTo(-minDim * 0.12f, y)
                cubicTo(w * 0.25f, y - minDim * 0.10f + bend, w * 0.62f, y + minDim * 0.10f - bend, w + minDim * 0.12f, y - minDim * 0.015f)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * (0.12f - i * 0.012f)
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = withAlpha(if (i % 2 == 0) amber else russet, (24 * dim).toInt())
            canvas.drawPath(band, paint)
        }
        paint.style = Paint.Style.FILL

        // Drifting maple-like leaves with softly rendered lobes and a fine central vein.
        val leafColors = intArrayOf(0xFFE5A84B.toInt(), 0xFFA3281E.toInt(), 0xFFE8B86D.toInt(), 0xFFD96624.toInt(), 0xFFC9783A.toInt())
        for (i in 0 until 12) {
            val seed = i * 97 + 5
            val phaseOffset = (i * 0.6180339f) % 1f
            val fall = (t + phaseOffset) % 1f
            val noise = loopedFbm(t, radius = 1.2f + i * 0.025f, octaves = 2, seedOffset = seed)
            val x = w * ((i * 0.173f + 0.08f + 0.08f * sin(t * 2f * PI.toFloat() + i)) % 1f)
            val y = h * ((fall * 1.15f - 0.08f + 1f) % 1f)
            val radius = minDim * (0.018f + (i % 4) * 0.004f)
            val angle = (t * 2f * PI.toFloat() * (if (i % 2 == 0) 0.35f else -0.25f) + i * 1.7f) * 180f / PI.toFloat()
            val leafColor = mixArgb(leafColors[i % leafColors.size], palette.colorAt(i), 0.17f)

            val save = canvas.save()
            canvas.translate(x, y)
            canvas.rotate(angle)
            leafPath.reset()
            leafPath.moveTo(0f, -radius)
            leafPath.cubicTo(radius * 0.8f, -radius * 0.82f, radius * 0.58f, -radius * 0.28f, 0f, radius)
            leafPath.cubicTo(-radius * 0.62f, radius * 0.25f, -radius * 0.78f, -radius * 0.48f, 0f, -radius)
            leafPath.close()
            paint.color = withAlpha(leafColor, ((78f + 38f * noise) * dim).toInt())
            canvas.drawPath(leafPath, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minDim * 0.0018f
            paint.color = withAlpha(0xFFFFD58A.toInt(), (95 * dim).toInt())
            canvas.drawLine(0f, -radius * 0.78f, 0f, radius * 0.82f, paint)
            paint.style = Paint.Style.FILL
            canvas.restoreToCount(save)
        }

        // Tiny floating dust glints near the sun.
        for (i in 0 until 9) {
            val n = loopedFbm(t, radius = 1.25f, seedOffset = i * 31 + 137)
            val x = w * ((0.08f + i * 0.103f + (n - 0.5f) * 0.05f) % 1f)
            val y = h * (0.2f + (i % 5) * 0.13f + 0.025f * sin(t * 2f * PI.toFloat() + i))
            paint.color = withAlpha(0xFFFFE6B5.toInt(), ((35f + 35f * n) * dim).toInt())
            canvas.drawCircle(x, y, minDim * (0.0025f + n * 0.002f), paint)
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
