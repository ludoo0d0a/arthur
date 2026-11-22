package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Bakes one frozen frame of Penrose Stairs for Auto/Ambient album art. */
internal object PenroseStairsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val cos30 = cos(PI.toFloat() / 6f)
    private val sin30 = sin(PI.toFloat() / 6f)

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
            w * 0.5f, h * 0.44f, maxOf(w, h),
            intArrayOf(0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.12f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val dim = 0.62f + 0.38f * pulse
        val cx = w * 0.5f
        val cy = h * 0.52f
        val stepW = minDim * 0.042f
        val stepH = stepW * 0.38f
        val rise = stepW * 0.22f
        val ringHalfW = minDim * 0.34f
        val ringHalfH = minDim * 0.26f
        val stepsPerSide = 9
        val tread = palette.colorAt(1)
        val riser = mixArgb(tread, Color.BLACK, 0.22f)
        val highlight = mixArgb(tread, Color.WHITE, 0.18f)
        val washAngle = loop * 2f * PI.toFloat() * 0.25f
        for (side in 0 until 4) {
            for (s in 0 until stepsPerSide) {
                val progress = (side * stepsPerSide + s) / (stepsPerSide * 4f)
                val wash = 0.5f + 0.5f * sin(washAngle + progress * 2f * PI.toFloat() - side * PI.toFloat() / 2f)
                val pts = stepQuad(cx, cy, side, s, stepsPerSide, ringHalfW, ringHalfH, stepW, stepH, rise)
                paint.color = withAlpha(riser, 0.88f * dim)
                path.reset()
                path.moveTo(pts[0].first, pts[0].second)
                path.lineTo(pts[1].first, pts[1].second)
                path.lineTo(pts[2].first, pts[2].second)
                path.lineTo(pts[3].first, pts[3].second)
                path.close()
                canvas.drawPath(path, paint)
                val shade = mixArgb(tread, highlight, (wash * 0.35f).coerceIn(0f, 0.35f))
                paint.color = withAlpha(shade, 0.92f * dim)
                path.reset()
                path.moveTo(pts[1].first, pts[1].second)
                path.lineTo(pts[4].first, pts[4].second)
                path.lineTo(pts[5].first, pts[5].second)
                path.lineTo(pts[2].first, pts[2].second)
                path.close()
                canvas.drawPath(path, paint)
                paint.color = Color.argb((0.22f * dim * wash * 255).toInt().coerceIn(0, 255), 0, 0, 0)
                path.reset()
                path.moveTo(pts[4].first, pts[4].second + stepH * 0.15f)
                path.lineTo(pts[5].first, pts[5].second + stepH * 0.15f)
                path.lineTo(pts[2].first, pts[2].second + stepH * 0.2f)
                path.lineTo(pts[1].first, pts[1].second + stepH * 0.18f)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
        paint.alpha = 255
    }

    private fun stepQuad(
        cx: Float,
        cy: Float,
        side: Int,
        step: Int,
        total: Int,
        halfW: Float,
        halfH: Float,
        stepW: Float,
        stepH: Float,
        rise: Float,
    ): List<Pair<Float, Float>> {
        val t0 = step.toFloat() / total
        val t1 = (step + 1f) / total
        val span = sideSpan(side, t0, t1, halfW, halfH, cx, cy)
        val riseOff = step * rise
        val p0 = iso(span[0], span[1] + riseOff)
        val p1 = iso(span[2], span[3] + riseOff)
        val p2 = iso(span[2], span[3] + riseOff + stepH)
        val p3 = iso(span[0], span[1] + riseOff + stepH)
        val p4 = iso(span[0] + (span[2] - span[0]) * 0.15f, span[1] + riseOff - stepW * 0.35f)
        val p5 = iso(span[2] - (span[2] - span[0]) * 0.15f, span[3] + riseOff - stepW * 0.35f)
        return listOf(p0, p1, p2, p3, p4, p5)
    }

    private fun sideSpan(
        side: Int,
        t0: Float,
        t1: Float,
        halfW: Float,
        halfH: Float,
        cx: Float,
        cy: Float,
    ): FloatArray = when (side) {
        0 -> {
            val x0 = cx - halfW + t0 * halfW * 2f
            val x1 = cx - halfW + t1 * halfW * 2f
            floatArrayOf(x0, cy - halfH, x1, cy - halfH)
        }
        1 -> {
            val y0 = cy - halfH + t0 * halfH * 2f
            val y1 = cy - halfH + t1 * halfH * 2f
            floatArrayOf(cx + halfW, y0, cx + halfW, y1)
        }
        2 -> {
            val x0 = cx + halfW - t0 * halfW * 2f
            val x1 = cx + halfW - t1 * halfW * 2f
            floatArrayOf(x0, cy + halfH, x1, cy + halfH)
        }
        else -> {
            val y0 = cy + halfH - t0 * halfH * 2f
            val y1 = cy + halfH - t1 * halfH * 2f
            floatArrayOf(cx - halfW, y0, cx - halfW, y1)
        }
    }

    private fun iso(x: Float, y: Float): Pair<Float, Float> =
        Pair(
            x * cos30 - y * sin30 * 0.35f,
            -y * 0.55f + x * sin30 * 0.18f,
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