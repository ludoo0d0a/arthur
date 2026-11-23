package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of a shaded ride toward a warm forest clearing. */
internal object VeloForestCanopyStill {
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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.3f, octaves = 3, seedOffset = 11)
        val breathing = 0.9f + 0.1f * loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 23)
        val dim = 0.85f + 0.15f * pulse
        val accent = mixArgb(0xFF60A070.toInt(), palette.colorAt(0), 0.25f)
        val horizonY = h * (0.57f + (wash - 0.5f) * 0.018f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0C1810.toInt(), 0xFF1A3020.toInt(), 0xFF101D14.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Bright clearing beyond the shaded tree tunnel.
        paint.shader = RadialGradient(
            w * 0.5f, horizonY, minDim * 0.54f,
            intArrayOf(
                withAlpha(0xFFFFD9A0.toInt(), (0.58f * breathing * dim * 255).toInt()),
                withAlpha(accent, (0.30f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, horizonY, minDim * 0.54f, paint)
        paint.shader = null

        // Filtered light shafts tapering into the distance.
        for (i in 0 until 4) {
            val x = w * (0.30f + i * 0.13f) + (wash - 0.5f) * w * 0.025f
            path.reset()
            path.moveTo(x - w * 0.025f, h * 0.12f)
            path.lineTo(x + w * 0.035f, h * 0.12f)
            path.lineTo(w * (0.47f + i * 0.018f), horizonY + h * 0.12f)
            path.lineTo(w * (0.46f + i * 0.018f), horizonY + h * 0.12f)
            path.close()
            paint.color = withAlpha(0xFFFFD89A.toInt(), ((0.045f + 0.025f * breathing) * dim * 255).toInt())
            canvas.drawPath(path, paint)
        }

        // Layered distant tree line.
        for (i in 0 until 9) {
            val x = w * (i / 8f)
            val crownW = w * (0.13f + (i % 3) * 0.025f)
            val crownH = h * (0.13f + (i % 4) * 0.025f)
            val y = horizonY + h * (0.03f + (i % 2) * 0.025f)
            paint.color = withAlpha(0xFF14261A.toInt(), (0.75f * dim * 255).toInt())
            rect.set(x - crownW * 0.5f, y - crownH * 0.65f, x + crownW * 0.5f, y + crownH * 0.35f)
            canvas.drawOval(rect, paint)
        }

        // Path narrows toward the clearing.
        path.reset()
        path.moveTo(w * 0.47f, horizonY)
        path.lineTo(w * 0.53f, horizonY)
        path.lineTo(w * 0.91f, h)
        path.lineTo(w * 0.09f, h)
        path.close()
        paint.shader = LinearGradient(
            0f, horizonY, 0f, h,
            intArrayOf(withAlpha(0xFF59634D.toInt(), (0.58f * dim * 255).toInt()), withAlpha(0xFF18251A.toInt(), (0.84f * dim * 255).toInt())),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.003f
        paint.color = withAlpha(0xFFD2C49A.toInt(), (0.24f * dim * 255).toInt())
        canvas.drawLine(w * 0.47f, horizonY, w * 0.28f, h, paint)
        canvas.drawLine(w * 0.53f, horizonY, w * 0.72f, h, paint)
        paint.style = Paint.Style.FILL

        // Trunks and foliage frame the path.
        for (i in 0 until 6) {
            val side = if (i % 2 == 0) -1f else 1f
            val x = if (side < 0f) w * (0.04f + (i / 2) * 0.045f) else w * (0.96f - (i / 2) * 0.045f)
            val topY = h * (0.08f + (i % 3) * 0.06f)
            val trunkWidth = minDim * (0.022f + (i % 3) * 0.008f)
            paint.color = withAlpha(0xFF101B12.toInt(), ((0.7f + (i % 2) * 0.18f) * dim * 255).toInt())
            paint.strokeWidth = trunkWidth
            canvas.drawLine(x, topY, x + side * trunkWidth * 1.5f, h * (0.95f - (i % 2) * 0.08f), paint)
            paint.color = withAlpha(0xFF24452D.toInt(), (0.65f * dim * 255).toInt())
            rect.set(x - trunkWidth * 3f, topY - trunkWidth * 2f, x + trunkWidth * 3f, topY - trunkWidth * 2f + minDim * 0.18f)
            canvas.drawOval(rect, paint)
        }

        // Leafy overhang forms a shaded frame.
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(w, 0f)
        path.lineTo(w, h * 0.16f)
        path.cubicTo(w * 0.80f, h * 0.11f, w * 0.73f, h * 0.25f, w * 0.58f, h * 0.23f)
        path.cubicTo(w * 0.42f, h * 0.20f, w * 0.35f, h * 0.12f, w * 0.20f, h * 0.20f)
        path.cubicTo(w * 0.12f, h * 0.25f, w * 0.08f, h * 0.17f, 0f, h * 0.23f)
        path.close()
        paint.color = withAlpha(0xFF0C1810.toInt(), (0.96f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        for (i in 0 until 7) {
            val x = w * ((i + 0.5f) / 7f)
            val sway = sin(t01 * 2f * PI.toFloat() + i) * minDim * 0.006f
            val y = h * (0.17f + (i % 3) * 0.025f) + sway
            val leafW = minDim * (0.08f + (i % 4) * 0.012f)
            paint.color = withAlpha(0xFF24452D.toInt(), (0.45f * dim * 255).toInt())
            rect.set(x - leafW * 0.5f, y - leafW * 0.16f, x + leafW * 0.5f, y + leafW * 0.16f)
            canvas.drawOval(rect, paint)
        }

        // Sparse foreground grasses and edge falloff.
        for (i in 0 until 7) {
            val side = if (i % 2 == 0) -1f else 1f
            val progress = (i / 7f + (sin(t01 * 2f * PI.toFloat()) + 1f) * 0.06f) % 1f
            val y = horizonY + (h - horizonY) * (0.34f + 0.64f * progress)
            val x = if (side < 0f) w * (0.06f + progress * 0.29f) else w * (0.94f - progress * 0.29f)
            val height = minDim * (0.025f + 0.04f * progress)
            paint.color = withAlpha(accent, (0.22f * dim * 255).toInt())
            paint.strokeWidth = minDim * 0.002f
            canvas.drawLine(x, y, x + side * height * 0.24f, y - height, paint)
        }
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(withAlpha(0xFF080A0C.toInt(), (0.34f * dim * 255).toInt()), Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(0xFF080A0C.toInt(), (0.34f * dim * 255).toInt())),
            floatArrayOf(0f, 0.18f, 0.82f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
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
