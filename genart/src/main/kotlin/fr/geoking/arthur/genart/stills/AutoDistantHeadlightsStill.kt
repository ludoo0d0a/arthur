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
import kotlin.math.sin

/** Bakes one frozen frame of Auto Distant Headlights for Auto/Ambient album art. */
internal object AutoDistantHeadlightsStill {
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
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val haze = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 37)
        val sway = sin(t01 * 2f * PI.toFloat())
        val horizon = h * (0.47f + 0.018f * (haze - 0.5f))
        val cx = w * (0.5f + 0.018f * sway)
        val dim = 0.85f + 0.15f * pulse
        val warm = mixArgb(0xFFFFE8A0.toInt(), palette.colorAt(0), 0.18f)
        val headlight = mixArgb(0xFFFFF2C2.toInt(), warm, 0.18f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF080A10.toInt(), 0xFF12161E.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, horizon / h, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, horizon - h * 0.18f, 0f, horizon + h * 0.16f,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFFB4C1D0.toInt(), ((0.035f + 0.025f * haze) * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.18f, w, horizon + h * 0.16f, paint)
        paint.shader = null

        // Road surface and edge traces converge into the hazy vanishing point.
        path.reset()
        path.moveTo(cx - minDim * 0.055f, horizon)
        path.cubicTo(cx - minDim * 0.13f, h * 0.70f, w * 0.16f, h * 0.82f, w * 0.08f, h)
        path.lineTo(w * 0.92f, h)
        path.cubicTo(w * 0.84f, h * 0.82f, cx + minDim * 0.13f, h * 0.70f, cx + minDim * 0.055f, horizon)
        path.close()
        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(0xFF252932.toInt(), 0xFF171B22.toInt(), 0xFF0B0D12.toInt()),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        paint.color = withAlpha(0xFF31353A.toInt(), (0.34f * dim * 255).toInt())
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.004f
        path.reset()
        path.moveTo(cx - minDim * 0.055f, horizon)
        path.cubicTo(cx - minDim * 0.13f, h * 0.70f, w * 0.16f, h * 0.82f, w * 0.08f, h)
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(cx + minDim * 0.055f, horizon)
        path.cubicTo(cx + minDim * 0.13f, h * 0.70f, w * 0.84f, h * 0.82f, w * 0.92f, h)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // Tapered asphalt reflection under the headlights.
        path.reset()
        path.moveTo(cx - minDim * 0.012f, horizon + h * 0.018f)
        path.cubicTo(cx - minDim * 0.035f, h * 0.65f, w * 0.38f, h * 0.84f, w * 0.33f, h)
        path.lineTo(w * 0.67f, h)
        path.cubicTo(w * 0.62f, h * 0.84f, cx + minDim * 0.035f, h * 0.65f, cx + minDim * 0.012f, horizon + h * 0.018f)
        path.close()
        paint.shader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(warm, ((0.12f + 0.06f * haze) * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.36f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // Distant pair of headlights and their soft atmospheric halo.
        val lightY = horizon + h * 0.006f
        val separation = minDim * (0.022f + 0.004f * haze)
        val glowRadius = minDim * (0.13f + 0.012f * haze)
        for (side in -1..1 step 2) {
            val x = cx + side * separation
            paint.shader = RadialGradient(
                x, lightY, glowRadius,
                intArrayOf(withAlpha(headlight, (0.62f * dim * 255).toInt()), withAlpha(warm, (0.30f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.16f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, lightY, glowRadius, paint)
            paint.shader = null
            paint.color = withAlpha(headlight, (0.92f * dim * 255).toInt())
            canvas.drawCircle(x, lightY, minDim * 0.0055f, paint)
        }

        paint.shader = RadialGradient(
            cx, lightY, minDim * 0.22f,
            intArrayOf(withAlpha(warm, (0.16f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, lightY, minDim * 0.22f, paint)
        paint.shader = null

        // Small pairs of roadside reflectors establish the road's depth.
        for (i in 0 until 5) {
            val f = (i + 1) / 6f
            val y = horizon + (h - horizon) * f * 0.88f
            val halfRoad = minDim * (0.055f + 0.39f * f)
            val radius = minDim * (0.003f + 0.0035f * f)
            val alpha = (0.18f + 0.10f * loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 100 + i)) * dim
            paint.color = withAlpha(0xFFC4A978.toInt(), (alpha * 255).toInt())
            canvas.drawCircle(cx - halfRoad, y, radius, paint)
            canvas.drawCircle(cx + halfRoad, y, radius, paint)
        }

        // A low dashboard silhouette anchors the point of view.
        path.reset()
        path.moveTo(0f, h * 0.94f)
        path.cubicTo(w * 0.24f, h * 0.89f, w * 0.32f, h * 0.92f, cx, h * 0.93f)
        path.cubicTo(w * 0.68f, h * 0.92f, w * 0.76f, h * 0.89f, w, h * 0.94f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = withAlpha(0xFF050609.toInt(), (0.72f * dim * 255).toInt())
        canvas.drawPath(path, paint)
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
