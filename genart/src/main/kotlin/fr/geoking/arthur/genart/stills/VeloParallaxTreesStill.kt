package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI

/** Bakes one frozen frame of Velo Parallax Trees for Auto/Ambient album art. */
internal object VeloParallaxTreesStill {
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
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t, radius = 1.35f, octaves = 2, seedOffset = 11)
        val dim = 0.85f + 0.15f * pulse
        val horizonGlow = mixArgb(0xFF80A070.toInt(), palette.colorAt(0), 0.22f)
        val horizon = h * (0.53f + 0.025f * (wash - 0.5f))

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(brightness(0xFF2A4030.toInt(), dim), brightness(0xFF1A281C.toInt(), dim)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, horizon - h * 0.13f, 0f, horizon + h * 0.13f,
            intArrayOf(Color.TRANSPARENT, withAlpha(horizonGlow, (0.24f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon - h * 0.13f, w, horizon + h * 0.13f, paint)
        paint.shader = null

        // Distant tree line, moving slowly against the warm horizon.
        for (i in 0 until 9) {
            val x = ((i * 0.137f + 0.03f - t * 0.12f + 1f) % 1f) * w
            val treeW = minDim * (0.035f + (i % 3) * 0.008f)
            val treeH = h * (0.065f + (i % 4) * 0.013f)
            val baseY = horizon + h * 0.065f
            path.reset()
            path.moveTo(x - treeW * 0.5f, baseY)
            path.lineTo(x, baseY - treeH)
            path.lineTo(x + treeW * 0.5f, baseY)
            path.close()
            paint.color = withAlpha(0xFF1A2A20.toInt(), (0.72f * dim * 255).toInt())
            canvas.drawPath(path, paint)
        }

        paint.shader = LinearGradient(
            0f, horizon + h * 0.12f, 0f, h,
            intArrayOf(0x00263326, withAlpha(0xFF182018.toInt(), (0.85f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizon + h * 0.12f, w, h, paint)
        paint.shader = null

        path.reset()
        path.moveTo(w * 0.43f, horizon + h * 0.13f)
        path.lineTo(w * 0.57f, horizon + h * 0.13f)
        path.lineTo(w * 0.86f, h)
        path.lineTo(w * 0.14f, h)
        path.close()
        paint.color = withAlpha(0xFF101712.toInt(), (0.64f * dim * 255).toInt())
        canvas.drawPath(path, paint)
        paint.color = withAlpha(horizonGlow, (0.38f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.003f
        canvas.drawLine(w * 0.5f, horizon + h * 0.17f, w * 0.5f, h, paint)

        // Nearby evergreen silhouettes sweep by more quickly than the far line.
        for (i in 0 until 7) {
            val x = ((i * 0.173f + 0.04f - t * 0.34f + 1f) % 1f) * w
            val treeW = minDim * (0.085f + (i % 3) * 0.012f)
            val treeH = h * (0.15f + (i % 4) * 0.025f)
            val baseY = horizon + h * 0.24f + (i % 3) * h * 0.018f
            val trunkW = treeW * 0.11f
            paint.color = withAlpha(0xFF111912.toInt(), (0.76f * dim * 255).toInt())
            canvas.drawRect(x - trunkW * 0.5f, baseY - treeH * 0.18f, x + trunkW * 0.5f, baseY, paint)
            paint.color = withAlpha(0xFF101A13.toInt(), (0.84f * dim * 255).toInt())
            for (branch in 0..2) {
                val top = baseY - treeH * (0.42f + branch * 0.18f)
                val halfW = treeW * (0.5f - branch * 0.10f)
                path.reset()
                path.moveTo(x - halfW, top + treeH * 0.16f)
                path.lineTo(x, top - treeH * 0.30f)
                path.lineTo(x + halfW, top + treeH * 0.16f)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun mixArgb(a: Int, b: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
        )
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))

    private fun brightness(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255),
    )
}
