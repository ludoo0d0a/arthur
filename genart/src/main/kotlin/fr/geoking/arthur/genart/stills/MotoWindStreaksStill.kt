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

/** Bakes one frozen frame of Moto Wind Streaks for Auto/Ambient album art. */
internal object MotoWindStreaksStill {
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
        val cycle = (2f * PI.toFloat())
        val t01 = ((phase % cycle) / cycle).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 11)
        val dim = 0.88f + 0.12f * pulse

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF12161C.toInt(), 0xFF0A0C10.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)

        val horizonY = h * (0.52f + (wash - 0.5f) * 0.025f)
        val glowRadius = minDim * 0.48f
        paint.shader = RadialGradient(
            w * 0.5f, horizonY, glowRadius,
            intArrayOf(withAlpha(0xFFC0D0E0.toInt(), (0.13f * dim * 255).toInt()),
                withAlpha(0xFF566578.toInt(), (0.045f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, horizonY, glowRadius, paint)

        // Deterministic streaks rush outward from the point ahead of the rider.
        val count = 17
        val centerX = w * 0.5f
        for (i in 0 until count) {
            val side = if (i % 2 == 0) -1f else 1f
            val angle = side * (0.22f + seeded(i, 3) * 0.93f)
            val startPhase = seeded(i, 7)
            val travel = (startPhase + t01) % 1f
            val near = 0.08f + travel * 0.94f
            val far = (near - (0.10f + seeded(i, 13) * 0.24f)).coerceAtLeast(0f)
            val ux = sin(angle)
            val uy = cos(angle)
            val sx = centerX + ux * minDim * far * 1.35f
            val sy = horizonY + uy * minDim * far * 1.65f
            val ex = (centerX + ux * minDim * near * 1.35f).coerceIn(-w * 0.2f, w * 1.2f)
            val ey = (horizonY + uy * minDim * near * 1.65f).coerceIn(-h * 0.15f, h * 1.15f)
            val bend = side * (0.01f + seeded(i, 19) * 0.06f) * minDim
            val warp = loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = i * 31 + 29) - 0.5f
            val cx = (sx + ex) * 0.5f + bend * (0.5f + warp)
            val cy = (sy + ey) * 0.5f
            val alpha = (0.025f + 0.11f * travel).coerceAtMost(0.14f) * dim
            val color = if (i % 7 == 0) 0xFF9DB8D0.toInt() else palette.colorAt(i)

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = minDim * (0.0014f + seeded(i, 23) * 0.002f)
            paint.shader = LinearGradient(
                sx, sy, ex, ey,
                intArrayOf(Color.TRANSPARENT, withAlpha(color, (alpha * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.56f, 1f),
                Shader.TileMode.CLAMP,
            )
            path.reset()
            path.moveTo(sx, sy)
            path.quadTo(cx, cy, ex, ey)
            canvas.drawPath(path, paint)
        }

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, h * 0.78f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFF05070A.toInt(), (0.72f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.78f, w, h, paint)
        paint.shader = null
    }

    private fun seeded(index: Int, salt: Int): Float {
        val n = kotlin.math.sin((index * 127.1f + salt * 311.7f).toDouble()) * 43758.5453
        return (n - kotlin.math.floor(n)).toFloat()
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
