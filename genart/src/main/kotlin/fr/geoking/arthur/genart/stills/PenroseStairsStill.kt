package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.sin01
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
        val dim = 0.62f + 0.38f * pulse
        val bright = 1.28f * dim
        val floorY = h * 0.8f

        paint.shader = RadialGradient(
            w * 0.5f,
            h * 0.42f,
            maxOf(w, h) * 0.95f,
            intArrayOf(
                0xFF2A3A55.toInt(),
                0xFF182238.toInt(),
                0xFF0B1220.toInt(),
                0xFF05080F.toInt(),
            ),
            floatArrayOf(0f, 0.55f, 0.85f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Soft floor plane with a faint warm reflection below the stairs.
        paint.shader = LinearGradient(
            0f,
            floorY,
            w,
            h,
            intArrayOf(
                0xFF131B2B.toInt(),
                0xFF0B101B.toInt(),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, floorY, w, h, paint)
        paint.shader = null

        val loop = phase01((phase + rotationDeg * PI.toFloat() / 180f * 0.12f) / (2f * PI.toFloat()))
        val time = loop * 2f * PI.toFloat()
        val breath = 1f + 0.02f * (sin01(time) * 2f - 1f)
        val rotRad = (sin01(time * 0.83f) * 2f - 1f) * (PI.toFloat() / 180f)
        val cx = w * 0.5f
        val cy = h * 0.52f
        val ringHalfW = minDim * 0.44f
        val ringHalfH = minDim * 0.30f
        val stepW = minDim * 0.075f
        val stepH = stepW * 0.42f
        val rise = stepW * 0.24f
        val tread = tinted(mixArgb(palette.colorAt(1), Color.WHITE, 0.24f), bright)
        val darkTread = mixArgb(tread, Color.BLACK, 0.38f)
        val litTread = mixArgb(tread, Color.WHITE, 0.55f)
        val light = mixArgb(palette.colorAt(0), 0xFFFFF3E0.toInt(), 0.35f)
        val washAngle = time * 0.25f
        val stepsPerSide = 9
        var globalStep = 0
        for (side in 0 until 4) {
            for (s in 0 until stepsPerSide) {
                val progress = (globalStep + s) / (stepsPerSide * 4f)
                val wash = (0.5f + 0.5f * sin(washAngle + progress * 2f * PI.toFloat() - side * PI.toFloat() / 2f)).coerceIn(0f, 1f)
                val lit = (0.30f + 0.70f * wash).coerceIn(0f, 1f)
                val pts = stepQuad(cx, cy, side, s, stepsPerSide, ringHalfW, ringHalfH, stepW, stepH, rise)
                val riser = mixArgb(mixArgb(tread, Color.BLACK, 0.30f), mixArgb(tread, Color.WHITE, 0.5f), lit * 0.8f)
                paint.color = withAlpha(riser, 0.95f * dim)
                path.reset()
                path.moveTo(pts[0].first, pts[0].second)
                path.lineTo(pts[1].first, pts[1].second)
                path.lineTo(pts[2].first, pts[2].second)
                path.lineTo(pts[3].first, pts[3].second)
                path.close()
                canvas.drawPath(path, paint)
                paint.color = withAlpha(mixArgb(darkTread, litTread, lit), 0.95f * dim)
                path.reset()
                path.moveTo(pts[1].first, pts[1].second)
                path.lineTo(pts[4].first, pts[4].second)
                path.lineTo(pts[5].first, pts[5].second)
                path.lineTo(pts[2].first, pts[2].second)
                path.close()
                canvas.drawPath(path, paint)
                paint.color = withAlpha(0xFF000000.toInt(), (0.30f - 0.16f * lit).coerceIn(0.06f, 0.30f) * dim)
                path.reset()
                val lift = stepH * 0.15f
                path.moveTo(pts[4].first, pts[4].second + lift)
                path.lineTo(pts[5].first, pts[5].second + lift)
                path.lineTo(pts[2].first, pts[2].second + lift + stepH * 0.05f)
                path.lineTo(pts[1].first, pts[1].second + lift + stepH * 0.06f)
                path.close()
                canvas.drawPath(path, paint)

                if (lit > 0.55f) {
                    val cx2 = (pts[1].first + pts[2].first) * 0.5f
                    val cy2 = (pts[1].second + pts[2].second) * 0.5f
                    val glowRadius = minDim * 0.055f
                    val glowAlpha = ((lit - 0.55f) / 0.45f * 0.35f).coerceIn(0f, 0.35f) * dim
                    paint.color = withAlpha(light, glowAlpha)
                    canvas.drawCircle(cx2, cy2, glowRadius, paint)
                }
            }
            globalStep += stepsPerSide
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
        val (ax0, ay0, ax1, ay1) = sideSpan(side, t0, t1, halfW, halfH, cx, cy)
        val riseOff = step * rise
        val p0 = iso(ax0, ay0 + riseOff)
        val p1 = iso(ax1, ay1 + riseOff)
        val p2 = iso(ax1, ay1 + riseOff + stepH)
        val p3 = iso(ax0, ay0 + riseOff + stepH)
        val p4 = iso(ax0 + (ax1 - ax0) * 0.15f, ay0 + riseOff - stepW * 0.35f)
        val p5 = iso(ax1 - (ax1 - ax0) * 0.15f, ay1 + riseOff - stepW * 0.35f)
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

    private fun tinted(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).coerceIn(0f, 1f) * 255f
        val g = (Color.green(argb) * factor).coerceIn(0f, 1f) * 255f
        val b = (Color.blue(argb) * factor).coerceIn(0f, 1f) * 255f
        return Color.argb(
            255,
            r.toInt().coerceIn(0, 255),
            g.toInt().coerceIn(0, 255),
            b.toInt().coerceIn(0, 255),
        )
    }
}
