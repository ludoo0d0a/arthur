package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Bakes one frozen frame of Necker Cube for Auto/Ambient album art. */
internal object NeckerCubeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

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
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.9f,
            intArrayOf(0xFF141820.toInt(), 0xFF0A0C12.toInt(), 0xFF030408.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.08f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val flip = (sin(loop * 2f * PI.toFloat()) + 1f) * 0.5f
        val dim = 0.62f + 0.38f * pulse
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.22f
        val accent = palette.colorAt(0)
        val ghost = mixArgb(accent, 0xFF64748B.toInt(), 0.45f)
        val screenVerts = verts.map { projectNecker(it, cx, cy, scale) }
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val strokeBase = minDim * 0.008f
        edges.forEach { edge ->
            val aFlip = if (edge[2] == 1) flip else 1f - flip
            val bFlip = if (edge[3] == 1) flip else 1f - flip
            val frontBias = (aFlip + bFlip) * 0.5f
            val p0 = screenVerts[edge[0]]
            val p1 = screenVerts[edge[1]]
            paint.strokeWidth = strokeBase
            paint.color = withAlpha(ghost, (0.22f + 0.18f * (1f - frontBias)) * dim)
            canvas.drawLine(p0.first, p0.second, p1.first, p1.second, paint)
            paint.strokeWidth = strokeBase * (0.85f + 0.35f * frontBias)
            paint.color = withAlpha(accent, (0.55f + 0.4f * frontBias) * dim)
            canvas.drawLine(p0.first, p0.second, p1.first, p1.second, paint)
            if (frontBias > 0.62f) {
                val mx = (p0.first + p1.first) * 0.5f
                val my = (p0.second + p1.second) * 0.5f
                paint.style = Paint.Style.FILL
                paint.color = withAlpha(mixArgb(accent, Color.WHITE, 0.35f), 0.28f * dim * frontBias)
                canvas.drawCircle(mx, my, strokeBase * 1.8f, paint)
                paint.style = Paint.Style.STROKE
            }
        }
        paint.alpha = 255
    }

    private val verts = listOf(
        Triple(-1f, -1f, -1f),
        Triple(1f, -1f, -1f),
        Triple(1f, 1f, -1f),
        Triple(-1f, 1f, -1f),
        Triple(-1f, -1f, 1f),
        Triple(1f, -1f, 1f),
        Triple(1f, 1f, 1f),
        Triple(-1f, 1f, 1f),
    )

    private val edges = listOf(
        intArrayOf(0, 1, 1, 1),
        intArrayOf(1, 2, 1, 1),
        intArrayOf(2, 3, 1, 1),
        intArrayOf(3, 0, 1, 1),
        intArrayOf(4, 5, 0, 0),
        intArrayOf(5, 6, 0, 0),
        intArrayOf(6, 7, 0, 0),
        intArrayOf(7, 4, 0, 0),
        intArrayOf(0, 4, 1, 0),
        intArrayOf(1, 5, 1, 0),
        intArrayOf(2, 6, 1, 0),
        intArrayOf(3, 7, 1, 0),
    )

    private fun projectNecker(v: Triple<Float, Float, Float>, cx: Float, cy: Float, scale: Float): Pair<Float, Float> {
        val px = (v.first - v.third) * cos(PI.toFloat() / 6f) * scale + cx
        val py = -(v.second + (v.first + v.third) * 0.35f) * scale + cy
        return px to py
    }

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