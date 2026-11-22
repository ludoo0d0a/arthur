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

/** Bakes one frozen frame of Impossible Cube for Auto/Ambient album art. */
internal object ImpossibleCubeStill {
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
            w * 0.5f, h * 0.48f, maxOf(w, h),
            intArrayOf(0xFF151825.toInt(), 0xFF080A10.toInt(), 0xFF020306.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.1f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val dim = 0.62f + 0.38f * pulse
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = minDim * 0.2f
        val base = palette.colorAt(1)
        val paradox = palette.colorAt(3)
        val beamW = scale * 0.22f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        segments.forEachIndexed { i, seg ->
            val a = projectIso(seg[0], seg[1], cx, cy, scale)
            val b = projectIso(seg[2], seg[3], cx, cy, scale)
            val along = (i / segments.size.toFloat() + loop * 0.15f) % 1f
            val body = if (seg[4] == 1f) paradox else base
            val shaded = mixArgb(body, Color.WHITE, along * 0.28f)
            paint.strokeWidth = beamW * if (seg[4] == 1f) 1.35f else 1.2f
            paint.color = withAlpha(mixArgb(body, Color.BLACK, 0.25f), 0.7f * dim)
            canvas.drawLine(a.first, a.second, b.first, b.second, paint)
            paint.strokeWidth = beamW * if (seg[4] == 1f) 1.08f else 1f
            paint.color = withAlpha(shaded, 0.9f * dim)
            canvas.drawLine(a.first, a.second, b.first, b.second, paint)
            val hiT = (along * 0.85f + 0.075f).coerceIn(0.05f, 0.92f)
            val hx = a.first + (b.first - a.first) * hiT
            val hy = a.second + (b.second - a.second) * hiT
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((0.32f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(hx, hy, beamW * 0.55f, paint)
            paint.style = Paint.Style.STROKE
        }
        paint.alpha = 255
    }

    private val segments = listOf(
        floatArrayOf(-1f, -1f, 1f, -1f, 0f),
        floatArrayOf(1f, -1f, 1f, 1f, 0f),
        floatArrayOf(1f, 1f, -1f, 1f, 0f),
        floatArrayOf(-1f, 1f, -1f, -1f, 0f),
        floatArrayOf(-1f, -1f, -0.35f, -1.65f, 0f),
        floatArrayOf(1f, -1f, 1.65f, -0.35f, 0f),
        floatArrayOf(1f, 1f, 1.65f, 1.65f, 0f),
        floatArrayOf(-1f, 1f, -0.35f, 1.65f, 0f),
        floatArrayOf(-0.35f, -1.65f, 1.65f, -0.35f, 0f),
        floatArrayOf(1.65f, -0.35f, 1.65f, 1.65f, 0f),
        floatArrayOf(1.65f, 1.65f, -0.35f, 1.65f, 1f),
        floatArrayOf(-0.35f, 1.65f, -0.35f, -1.65f, 1f),
    )

    private fun projectIso(x: Float, y: Float, cx: Float, cy: Float, scale: Float): Pair<Float, Float> {
        val px = (x - y) * cos(PI.toFloat() / 6f) * scale + cx
        val py = -(x + y) * 0.38f * scale + cy - y * scale * 0.12f
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