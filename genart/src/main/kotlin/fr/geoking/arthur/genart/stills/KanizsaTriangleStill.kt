package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Bakes one frozen frame of Kanizsa Triangle for Auto/Ambient album art. */
internal object KanizsaTriangleStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val arcRect = RectF()

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
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.85f,
            intArrayOf(0xFFB8BDC6.toInt(), 0xFF9AA0A8.toInt(), 0xFF6B7280.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.06f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val ang = loop * 2f * PI.toFloat()
        val breath = 0.96f + 0.04f * ((sin(ang) + 1f) * 0.5f)
        val dim = 0.65f + 0.35f * pulse
        val cx = w * 0.5f
        val cy = h * 0.54f
        val r = minDim * 0.125f * breath
        val spread = minDim * 0.235f
        val openFrac = 0.18f + 0.025f * ((sin(ang + 0.4f) + 1f) * 0.5f)
        val rotWobble = 0.08f * sin(ang * 0.6f)
        val centers = arrayOf(
            floatArrayOf(cx, cy - spread),
            floatArrayOf(cx - spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
            floatArrayOf(cx + spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
        )
        val aimAngles = floatArrayOf(
            PI.toFloat() / 2f + rotWobble,
            -PI.toFloat() / 6f + rotWobble,
            -5f * PI.toFloat() / 6f + rotWobble,
        )
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((0.96f * dim * 255).toInt().coerceIn(0, 255), 17, 19, 24)
        for (i in centers.indices) {
            val openRad = openFrac * 2f * PI.toFloat()
            val aim = aimAngles[i]
            val startDeg = Math.toDegrees((aim - openRad * 0.5f).toDouble()).toFloat()
            val sweepDeg = Math.toDegrees((2f * PI.toFloat() - openRad).toDouble()).toFloat()
            arcRect.set(centers[i][0] - r, centers[i][1] - r, centers[i][0] + r, centers[i][1] + r)
            path.reset()
            path.moveTo(centers[i][0], centers[i][1])
            path.arcTo(arcRect, startDeg, sweepDeg)
            path.close()
            canvas.drawPath(path, paint)
        }

        val pulseA = 0.1f + 0.1f * ((sin(ang + 1.2f) + 1f) * 0.5f)
        path.reset()
        path.moveTo(centers[0][0], centers[0][1] + r * 0.08f)
        path.lineTo(centers[1][0] + r * 0.18f, centers[1][1] - r * 0.28f)
        path.lineTo(centers[2][0] - r * 0.18f, centers[2][1] - r * 0.28f)
        path.close()
        paint.color = Color.argb(((0.14f + pulseA) * dim * 255).toInt().coerceIn(0, 255), 245, 246, 248)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.07f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = withAlpha(mixArgb(palette.colorAt(1), Color.WHITE, 0.55f), (0.16f + pulseA) * dim)
        canvas.drawPath(path, paint)

        val invSpread = spread * 0.72f
        val inv = arrayOf(
            floatArrayOf(cx, cy + invSpread * 0.72f),
            floatArrayOf(cx - invSpread * cos(PI.toFloat() / 6f), cy - invSpread * 0.38f),
            floatArrayOf(cx + invSpread * cos(PI.toFloat() / 6f), cy - invSpread * 0.38f),
        )
        paint.strokeWidth = r * 0.09f
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = Color.argb((0.88f * dim * 255).toInt().coerceIn(0, 255), 17, 19, 24)
        for (i in 0..2) {
            val a = inv[i]
            val b = inv[(i + 1) % 3]
            val dx = b[0] - a[0]
            val dy = b[1] - a[1]
            canvas.drawLine(a[0], a[1], a[0] + dx * 0.28f, a[1] + dy * 0.28f, paint)
            canvas.drawLine(a[0] + dx * 0.72f, a[1] + dy * 0.72f, b[0], b[1], paint)
        }
        paint.alpha = 255
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
