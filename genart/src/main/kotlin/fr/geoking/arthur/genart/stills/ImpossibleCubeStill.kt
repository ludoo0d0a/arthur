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

/** Bakes one frozen frame of Impossible Cube for Auto/Ambient album art. */
internal object ImpossibleCubeStill {
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
            w * 0.5f, h * 0.46f, maxOf(w, h),
            intArrayOf(0xFF151825.toInt(), 0xFF080A10.toInt(), 0xFF020306.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.1f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val yaw = 0.12f * sin(loop * 2f * PI.toFloat() * 0.5f)
        val dim = 0.62f + 0.38f * pulse
        val cx = w * 0.5f
        val cy = h * 0.52f
        val scale = minDim * 0.26f
        val base = palette.colorAt(1)
        val paradox = palette.colorAt(3)
        val beamW = scale * 0.16f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE

        fun drawBeam(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, body: Int, along: Float, width: Float) {
            val a = project(ax, ay, az, cx, cy, scale, yaw)
            val b = project(bx, by, bz, cx, cy, scale, yaw)
            val shaded = mixArgb(body, Color.WHITE, along * 0.3f)
            paint.strokeWidth = width * 1.38f
            paint.color = withAlpha(mixArgb(body, Color.BLACK, 0.28f), 0.72f * dim)
            canvas.drawLine(a.first, a.second, b.first, b.second, paint)
            paint.strokeWidth = width
            paint.color = withAlpha(shaded, 0.92f * dim)
            canvas.drawLine(a.first, a.second, b.first, b.second, paint)
            val hiT = (along * 0.8f + 0.1f).coerceIn(0.08f, 0.9f)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((0.28f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(
                a.first + (b.first - a.first) * hiT,
                a.second + (b.second - a.second) * hiT,
                width * 0.48f,
                paint,
            )
            paint.style = Paint.Style.STROKE
        }

        beams.filter { it[6] == 0f }.forEachIndexed { i, b ->
            val along = ((i * 0.07f + loop) % 1f)
            drawBeam(b[0], b[1], b[2], b[3], b[4], b[5], base, along, beamW)
        }
        beams.filter { it[6] == 1f }.forEach { b ->
            drawBeam(b[0], b[1], b[2], b[3], b[4], b[5], paradox, 0.55f + loop * 0.2f, beamW * 1.06f)
        }

        val hull = listOf(
            project(-1f, -1f, -1f, cx, cy, scale, yaw),
            project(1f, -1f, -1f, cx, cy, scale, yaw),
            project(1f, 1f, -1f, cx, cy, scale, yaw),
            project(-1f, 1f, -1f, cx, cy, scale, yaw),
        )
        path.reset()
        path.moveTo(hull[0].first, hull[0].second)
        hull.drop(1).forEach { path.lineTo(it.first, it.second) }
        path.close()
        paint.strokeWidth = 1.4f
        paint.color = withAlpha(mixArgb(base, Color.WHITE, 0.15f), 0.18f * dim)
        canvas.drawPath(path, paint)
        paint.alpha = 255
    }

    // ax,ay,az,bx,by,bz,paradox(0/1)
    private val beams = listOf(
        floatArrayOf(-1f, -1f, -1f, 1f, -1f, -1f, 0f),
        floatArrayOf(1f, -1f, -1f, 1f, 1f, -1f, 0f),
        floatArrayOf(1f, 1f, -1f, -1f, 1f, -1f, 0f),
        floatArrayOf(-1f, 1f, -1f, -1f, -1f, -1f, 0f),
        floatArrayOf(-1f, -1f, 1f, 1f, -1f, 1f, 0f),
        floatArrayOf(1f, -1f, 1f, 1f, 1f, 1f, 0f),
        floatArrayOf(-1f, -1f, -1f, -1f, -1f, 1f, 0f),
        floatArrayOf(1f, -1f, -1f, 1f, -1f, 1f, 0f),
        floatArrayOf(1f, 1f, -1f, 1f, 1f, 1f, 0f),
        floatArrayOf(-1f, 1f, -1f, -1f, 1f, 1f, 0f),
        floatArrayOf(-1f, 1f, 1f, 1f, 1f, 1f, 1f),
        floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, 1f),
    )

    private fun project(
        x: Float,
        y: Float,
        z: Float,
        cx: Float,
        cy: Float,
        scale: Float,
        yaw: Float,
    ): Pair<Float, Float> {
        val cosY = cos(yaw)
        val sinY = sin(yaw)
        val rx = x * cosY - z * sinY
        val rz = x * sinY + z * cosY
        val px = (rx - rz) * cos(PI.toFloat() / 6f) * scale + cx
        val py = -(y * 0.92f + (rx + rz) * 0.32f) * scale + cy
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
