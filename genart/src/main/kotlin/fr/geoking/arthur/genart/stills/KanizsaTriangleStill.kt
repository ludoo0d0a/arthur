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

/** Bakes one frozen frame of Kanizsa Triangle for Auto/Ambient album art. */
internal object KanizsaTriangleStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val arcRect = android.graphics.RectF()

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
            w * 0.5f, h * 0.52f, maxOf(w, h),
            intArrayOf(0xFF1C1F26.toInt(), 0xFF0E1016.toInt(), 0xFF050608.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.06f
        val loop = ((time / (2f * PI.toFloat())) % 1f + 1f) % 1f
        val breath = 0.92f + 0.08f * sin(loop * 2f * PI.toFloat())
        val dim = 0.65f + 0.35f * pulse
        val cx = w * 0.5f
        val cy = h * 0.54f
        val r = minDim * 0.11f * breath
        val spread = minDim * 0.21f
        val wedgeOpen = 0.78f + 0.06f * sin(loop * 2f * PI.toFloat() + 0.3f)
        val centers = arrayOf(
            floatArrayOf(cx, cy - spread),
            floatArrayOf(cx - spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
            floatArrayOf(cx + spread * cos(PI.toFloat() / 6f), cy + spread * 0.55f),
        )
        val aimAngles = floatArrayOf(PI.toFloat() / 2f, -PI.toFloat() / 6f, -5f * PI.toFloat() / 6f)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((0.96f * dim * 255).toInt().coerceIn(0, 255), 243, 244, 246)
        for (i in centers.indices) {
            val openRad = (1f - wedgeOpen) * PI.toFloat()
            val aim = aimAngles[i]
            val startDeg = Math.toDegrees((aim - openRad * 0.5f).toDouble()).toFloat()
            val sweepDeg = Math.toDegrees((2f * PI.toFloat() - openRad).toDouble()).toFloat()
            arcRect.set(centers[i][0] - r, centers[i][1] - r, centers[i][0] + r, centers[i][1] + r)
            path.reset()
            path.arcTo(arcRect, startDeg, sweepDeg)
            path.lineTo(centers[i][0], centers[i][1])
            path.close()
            canvas.drawPath(path, paint)
        }
        val glow = palette.colorAt(1)
        val glowAlpha = 0.12f + 0.1f * sin(loop * 2f * PI.toFloat() + 1.1f)
        path.reset()
        path.moveTo(centers[0][0], centers[0][1] + r * 0.15f)
        path.lineTo(centers[1][0] + r * 0.2f, centers[1][1] - r * 0.35f)
        path.lineTo(centers[2][0] - r * 0.2f, centers[2][1] - r * 0.35f)
        path.close()
        paint.color = withAlpha(glow, glowAlpha * dim)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.12f
        paint.color = withAlpha(mixArgb(glow, Color.WHITE, 0.4f), (glowAlpha * 1.8f).coerceAtMost(0.28f) * dim)
        canvas.drawPath(path, paint)
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