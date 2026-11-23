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

/** Bakes one frozen frame of Micro Pollen Close for Auto/Ambient album art. */
internal object MicroPollenCloseStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val leaf = Path()

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
        val unit = size.toFloat()
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = 0.85f + 0.15f * pulse
        val glow = loopedFbm(t, radius = 1.2f, octaves = 3, seedOffset = 43)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF2A2210.toInt(), 0xFF18140A.toInt(), 0xFF0C100A.toInt()),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        leaf.reset()
        leaf.moveTo(-w * 0.08f, h * 0.82f)
        leaf.cubicTo(w * 0.16f, h * 0.56f, w * 0.36f, h * 0.63f, w * 0.54f, h * 1.08f)
        leaf.cubicTo(w * 0.30f, h * 0.93f, w * 0.08f, h * 0.91f, -w * 0.08f, h * 0.82f)
        leaf.close()
        paint.color = withAlpha(0xFF35451C.toInt(), (0.56f * dim * 255).toInt())
        canvas.drawPath(leaf, paint)
        paint.color = withAlpha(0xFF9DAD54.toInt(), (0.32f * dim * 255).toInt())
        paint.strokeWidth = unit * 0.003f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(w * 0.04f, h * 0.84f, w * 0.43f, h * 0.96f, paint)
        paint.style = Paint.Style.FILL

        val cx = w * 0.47f
        val cy = h * (0.38f + 0.025f * sin(t * 2f * PI.toFloat()))
        paint.shader = RadialGradient(
            cx, cy, unit * 0.72f,
            intArrayOf(withAlpha(0xFFFFE080.toInt(), (0.48f * dim * 255).toInt()), withAlpha(0xFFB87828.toInt(), (0.18f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, unit * 0.72f, paint)
        paint.shader = null

        for (i in 0 until 3) {
            val x = w * (0.27f + i * 0.24f) + unit * 0.012f * sin(t * 2f * PI.toFloat() + i)
            val y = h * (0.40f + (i % 2) * 0.12f) + unit * 0.008f * sin(t * 2f * PI.toFloat() + i * 1.7f)
            val r = unit * (0.047f + i * 0.009f) * (0.96f + 0.04f * glow)
            paint.shader = RadialGradient(
                x - r * 0.22f, y - r * 0.28f, r * 1.45f,
                intArrayOf(
                    withAlpha(0xFFFFF2B0.toInt(), (0.96f * dim * 255).toInt()),
                    withAlpha(0xFFFFD45A.toInt(), (0.94f * dim * 255).toInt()),
                    withAlpha(0xFFB56A1C.toInt(), (0.86f * dim * 255).toInt()),
                    withAlpha(0xFF58310F.toInt(), (0.70f * dim * 255).toInt()),
                ),
                floatArrayOf(0f, 0.3f, 0.78f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, r, paint)
            paint.shader = null
            paint.color = withAlpha(Color.WHITE, (0.78f * dim * 255).toInt())
            canvas.drawCircle(x - r * 0.25f, y - r * 0.31f, r * 0.12f, paint)
        }

        for (i in 0 until 7) {
            val n = loopedFbm(t, radius = 1.1f, octaves = 2, seedOffset = i * 97 + 17)
            val x = ((0.12f + i * 0.115f + 0.018f * (n - 0.5f) + 1f) % 1f) * w
            val y = ((0.14f + (i % 4) * 0.17f - t * 0.035f + 1f) % 1f) * h
            val r = unit * (0.012f + 0.023f * n)
            paint.shader = RadialGradient(
                x - r * 0.18f, y - r * 0.2f, r,
                intArrayOf(withAlpha(0xFFFFE894.toInt(), (0.85f * dim * 255).toInt()), withAlpha(0xFFE3A63B.toInt(), (0.72f * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(x - r, y - r * 0.72f, x + r, y + r * 0.72f, paint)
            paint.shader = null
        }
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}
